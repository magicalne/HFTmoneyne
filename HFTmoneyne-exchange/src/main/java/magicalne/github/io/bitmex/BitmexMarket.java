package magicalne.github.io.bitmex;

import com.google.common.hash.HashFunction;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.util.Utils;
import magicalne.github.io.wire.bitmex.*;
import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.netty.ws.NettyWebSocket;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import sun.misc.Contended;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.asynchttpclient.Dsl.*;

@Slf4j
public class BitmexMarket {
  private static final String WEBSOCKET_URL = "wss://testnet.bitmex.com/realtime?subscribe=orderBookL2_25:%s,trade:%s";
  private static final String AUTH = "{\"op\": \"authKeyExpires\", \"args\": [\"%s\", %d, \"%s\"]}";
  private static final String SUB = "{\"op\": \"subscribe\", \"args\": [\"%s:%s\"]}";
  private static final String VERB = "GET";
  private static final String PATH = "/realtime";
  private final String symbol;
  private final String accessKey;
  private final long tradeTimeout;
  private final HashFunction hashFunction;

  private volatile boolean buildingOrderBook;
  private final ConcurrentSkipListMap<Long, OrderBookEntry> bidMap;
  private final ConcurrentSkipListMap<Long, OrderBookEntry> askMap;
  @Contended
  private volatile long bestBidId = -1;
  @Contended
  private volatile long bestAskId = -1;

  private volatile boolean buildingTrade;
  private final ConcurrentLinkedQueue<TradeEntry> buyQueue;
  private final ConcurrentLinkedQueue<TradeEntry> sellQueue;

  private volatile boolean buildingOrder;
  private final ConcurrentHashMap<String, Order> orderMap;

  private volatile boolean buildingPosition;
  private volatile Position position;

  private volatile AtomicBoolean buyUp = new AtomicBoolean();
  private volatile AtomicBoolean sellDown = new AtomicBoolean();

  @Contended
  private volatile double lastBuyPrice = -1;
  @Contended
  private volatile double lastSellPrice = -1;

  private Disruptor<StringEvent> disruptor;
  private NettyWebSocket ws;

  public BitmexMarket(String symbol, String accessKey, String accessSecret, long tradeTimeout) {
    this.symbol = symbol;
    this.accessKey = accessKey;
    this.tradeTimeout = tradeTimeout;

    this.buildingOrderBook = true;
    this.bidMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    this.askMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    this.buildingTrade = true;
    this.buyQueue = new ConcurrentLinkedQueue<>();
    this.sellQueue = new ConcurrentLinkedQueue<>();

    this.hashFunction = Utils.bitmexSignatureHashFunction(accessSecret);

    this.buildingOrder = true;
    this.orderMap = new ConcurrentHashMap<>(100);

    this.buildingPosition = true;
  }

  public void createWSConnection() throws ExecutionException, InterruptedException {
    createDisruptor();
    DefaultAsyncHttpClientConfig.Builder configBuilder = config()
      .setWebSocketMaxFrameSize(102400)
      .setIoThreadsCount(1)
      .setThreadFactory(new AffinityThreadFactory("market-ws", AffinityStrategies.DIFFERENT_CORE))
//      .setProxyServer(proxyServer("127.0.0.1", 1087))
      ;
    AsyncHttpClient c = asyncHttpClient(configBuilder);
    String url = String.format(WEBSOCKET_URL, symbol, symbol);
    log.info(url);
    ws = c.prepareGet(url)
      .execute(new WebSocketUpgradeHandler.Builder()
        .addWebSocketListener(new WebSocketListener() {
          @Override
          public void onOpen(WebSocket websocket) {
            long expires = System.currentTimeMillis() + 5000;
            String signature = hashFunction.hashString(VERB + PATH + expires, StandardCharsets.UTF_8).toString();
            String auth = String.format(AUTH, accessKey, expires, signature);
            websocket.sendTextFrame(auth);
            String subOrder = String.format(SUB, "order", symbol);
            websocket.sendTextFrame(subOrder);
            String subPosition = String.format(SUB, "position", symbol);
            websocket.sendTextFrame(subPosition);
            log.info(auth);
            log.info(subOrder);
            log.info(subPosition);
          }

          @Override
          public void onClose(WebSocket websocket, int code, String reason) {
            log.info("Socket closed: {}, {}", code, reason);
            try {
              disruptor.shutdown();
              createWSConnection();
            } catch (ExecutionException | InterruptedException e) {
              log.error("Reconnect to server with exception.", e);
              onClose(websocket, code, reason);
            }
          }

          @Override
          public void onError(Throwable t) {
            log.error("connection may disconnect.", t);
            ws.sendCloseFrame();
          }

          @Override
          public void onTextFrame(String payload, boolean finalFragment, int rsv) {
            RingBuffer<StringEvent> ringBuffer = disruptor.getRingBuffer();
            ringBuffer.publishEvent((event, sequence) -> event.setPayload(payload));
          }
        }).build()).get();
  }

  private void createDisruptor() {
    AffinityThreadFactory threadFactory =
      new AffinityThreadFactory("disruptor-consumer", AffinityStrategies.DIFFERENT_CORE);
    disruptor = new Disruptor<>(StringEvent::new, 1024, threadFactory);
    disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
      String payload = event.getPayload();
      Wire wire = WireType.JSON.apply(Bytes.fromString(payload));
      TableEnum table = wire.read("table").asEnum(TableEnum.class);
      ActionEnum action = wire.read("action").asEnum(ActionEnum.class);
      if (table != null) {
        final String data = "data";
        switch (table) {
          case orderBookL2_25:
            List<OrderBookEntry> orderBookEntries = wire.read(() -> data).list(OrderBookEntry.class);
            onEventOrderBook(action, orderBookEntries);
            break;
          case trade:
            List<TradeEntry> trades = wire.read(() -> data).list(TradeEntry.class);
            onEventTrade(action, trades);
            break;
          case order:
            List<Order> orders = wire.read(() -> data).list(Order.class);
            onEventOrder(action, orders);
            break;
          case position:
            List<Position> positions = wire.read(() -> data).list(Position.class);
            onEventPosition(action, positions);
          default:
        }
      }
      wire.bytes().release();
    });
    disruptor.start();
  }

  private void onEventPosition(ActionEnum action, List<Position> positions) {
    if (action != ActionEnum.partial && buildingPosition) {
      return;
    }
    switch (action) {
      case partial:
        buildingPosition = true;
        positions.clear();
        for (Position position : positions) {
          if (position.getSymbol().equals(symbol)) {
            this.position = position;
          }
        }
        buildingPosition = false;
        break;
      default:
        for (Position position : positions) {
          if (position.getSymbol().equals(symbol)) {
            if (this.position == null) {
              this.position = position;
            } else {
              this.position.copyFrom(position);
            }
          }
        }
        break;
    }
  }

  private void onEventOrder(ActionEnum action, List<Order> orders) {
    if (action != ActionEnum.partial && buildingOrder) {
      return;
    }
    switch (action) {
      case partial:
        buildingOrder = true;
        orderMap.clear();
        for (Order o : orders) {
          orderMap.put(o.getOrderID(), o);
        }
        buildingOrder = false;
        break;
      case insert:
        for (Order o : orders) {
          orderMap.put(o.getOrderID(), o);
        }
        break;
      case update:
        for (Order o : orders) {
          Order order = orderMap.get(o.getOrderID());
          order.updateFrom(o);
        }
        break;
        default:
          break;
    }
  }

  private void onEventTrade(ActionEnum action, List<TradeEntry> trades) {
    if (action != ActionEnum.partial && buildingTrade) {
      return;
    }
    switch (action) {
      case partial:
        buildingTrade = true;
        buyQueue.clear();
        sellQueue.clear();
        onInsertTradeEntry(trades);
        buildingTrade = false;
        break;
      case insert:
        onInsertTradeEntry(trades);
        break;
        default:
          log.error("Unknown operation! action: {}, trades: {}", action, trades);
    }
  }

  private void onInsertTradeEntry(List<TradeEntry> trades) {
    long now = System.currentTimeMillis();
    for (TradeEntry t : trades) {
      t.setCreateAt(now);
      if (t.getSide() == SideEnum.Buy) {
        lastBuyPrice = t.getPrice();
        buyQueue.offer(t);
        OrderBookEntry bestAsk = getBestAsk();
        if (bestAsk != null && t.getPrice() > bestAsk.getPrice()) {
          buyUp.compareAndSet(false, true);
        }
      } else {
        lastSellPrice = t.getPrice();
        sellQueue.offer(t);
        OrderBookEntry bestBid = getBestBid();
        if (bestBid != null && t.getPrice() < bestBid.getPrice()) {
          sellDown.compareAndSet(false, true);
        }
      }
    }
    clearTimeoutTradeRecords(buyQueue, now);
    clearTimeoutTradeRecords(sellQueue, now);
  }

  private void clearTimeoutTradeRecords(ConcurrentLinkedQueue<TradeEntry> trades, long now) {
    while (!trades.isEmpty() && trades.peek().getCreateAt() + tradeTimeout < now) {
      trades.poll();
    }
  }

  private void onEventOrderBook(ActionEnum action, List<OrderBookEntry> orderBookEntries) {
    if (action != ActionEnum.partial && buildingOrderBook) {
      return;
    }
    switch (action) {
      case partial:
        buildingOrderBook = true;
        bidMap.clear();
        askMap.clear();
        onInsertOrderBookEntry(orderBookEntries);
        buildingOrderBook = false;
        break;
      case update:
        onUpdateOrderBookEntry(orderBookEntries);
        break;
      case delete:
        onDeleteOrderBookEntry(orderBookEntries);
        break;
      case insert:
        onInsertOrderBookEntry(orderBookEntries);
        break;
        default:
          log.error("Unknown action!");
    }
  }

  private void onDeleteOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    for (OrderBookEntry o : orderBookEntries) {
      long id = o.getId();
      if (o.getSide() == SideEnum.Buy) {
        bidMap.remove(id);
      } else {
        askMap.remove(id);
      }
    }
    this.bestBidId = bidMap.lastKey();
    this.bestAskId = askMap.firstKey();
  }



  private void onUpdateOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    for (OrderBookEntry o : orderBookEntries) {
      long id = o.getId();
      if (o.getSide() == SideEnum.Buy) {
        OrderBookEntry orderBookEntry = bidMap.get(id);
        orderBookEntry.setSize(o.getSize());
      } else {
        OrderBookEntry orderBookEntry = askMap.get(id);
        orderBookEntry.setSize(o.getSize());
      }
    }
  }

  private void onInsertOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    for (OrderBookEntry o : orderBookEntries) {
      long id = o.getId();
      if (o.getSide() == SideEnum.Buy) {
        bidMap.put(id, o);
      } else {
        askMap.put(id, o);
      }
    }
    this.bestBidId = bidMap.lastKey();
    this.bestAskId = askMap.firstKey();
  }

  public OrderBookEntry getBestBid() {
    if (bestBidId == -1) {
      return null;
    } else {
      return bidMap.get(bestBidId);
    }
  }

  public OrderBookEntry getBestAsk() {
    if (bestAskId == -1) {
      return null;
    } else {
      return askMap.get(bestAskId);
    }
  }

  public ConcurrentLinkedQueue<TradeEntry> getBuyTrades() {
    return buyQueue;
  }

  public ConcurrentLinkedQueue<TradeEntry> getSellTrades() {
    return sellQueue;
  }

  public boolean isBuyUp() {
    boolean tmp = this.buyUp.get();
    this.buyUp.compareAndSet(true, false);
    return tmp;
  }

  public boolean isSellDown() {
    boolean tmp = this.sellDown.get();
    this.sellDown.compareAndSet(true, false);
    return tmp;
  }

  public double getLastBuyPrice() {
    return lastBuyPrice;
  }

  public double getLastSellPrice() {
    return lastSellPrice;
  }

  public Order getOrderById(String orderId) {
    return orderMap.get(orderId);
  }

  public Collection<Order> getOrders() {
    return orderMap.values();
  }

  public List<Order> getOrders(SideEnum side, long longPrice, int scale) {
    return orderMap.values()
      .stream()
      .filter(o -> o.getSide() == side && ((long) (o.getPrice() * scale)) == longPrice)
      .collect(Collectors.toList());
  }

  public Order removeOrder(String orderId) {
    return orderMap.remove(orderId);
  }

  public void removeOrders(List<String> orderIDs) {
    for (String orderID : orderIDs) {
      orderMap.remove(orderID);
    }
  }

  public Position getPosition() {
    return position;
  }
}
