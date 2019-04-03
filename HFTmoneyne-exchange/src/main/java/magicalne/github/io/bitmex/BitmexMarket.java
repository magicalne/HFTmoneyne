package magicalne.github.io.bitmex;

import com.google.common.hash.HashFunction;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.util.BitMexOrderBook;
import magicalne.github.io.util.LocalOrderStore;
import magicalne.github.io.util.MarketTradeData;
import magicalne.github.io.util.Utils;
import magicalne.github.io.wire.bitmex.*;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;
import static org.asynchttpclient.Dsl.proxyServer;

@Slf4j
public class BitmexMarket {
  private static final String WEBSOCKET_URL = "wss://www.bitmex.com/realtime?subscribe=orderBookL2_25:%s,trade:%s";
  private static final String AUTH = "{\"op\": \"authKeyExpires\", \"args\": [\"%s\", %d, \"%s\"]}";
  private static final String SUB = "{\"op\": \"subscribe\", \"args\": [\"%s:%s\"]}";
  private static final String VERB = "GET";
  private static final String PATH = "/realtime";
  private final String symbol;
  private final String accessKey;
  private final HashFunction hashFunction;

  private volatile boolean buildingOrderBook;
  private final BitMexOrderBook orderBook;

  private volatile boolean buildingTrade;
  private final MarketTradeData tradeData;

  private volatile boolean buildingOrder;
  private final LocalOrderStore localOrderStore;

  private volatile boolean buildingPosition;
  private volatile Position position;

  public BitmexMarket(String symbol, String accessKey, String accessSecret, int tradeTimeout)
    throws IllegalAccessException, InstantiationException {
    this.symbol = symbol;
    this.accessKey = accessKey;

    this.buildingOrderBook = true;
    this.orderBook = new BitMexOrderBook(25);

    this.buildingTrade = true;
    this.tradeData = new MarketTradeData(tradeTimeout);

    this.hashFunction = Utils.bitmexSignatureHashFunction(accessSecret);

    this.buildingOrder = true;
    this.localOrderStore = new LocalOrderStore(100);

    this.buildingPosition = true;
  }

  public void createWSConnection() throws ExecutionException, InterruptedException {
    Disruptor<StringEvent> disruptor = createDisruptor();
    DefaultAsyncHttpClientConfig.Builder configBuilder = config()
      .setWebSocketMaxFrameSize(102400)
      .setIoThreadsCount(1)
      .setTcpNoDelay(true)
      .setThreadFactory(new AffinityThreadFactory("market-ws", AffinityStrategies.DIFFERENT_CORE))
//      .setProxyServer(proxyServer("127.0.0.1", 1087))
      ;
    AsyncHttpClient c = asyncHttpClient(configBuilder);
    String url = String.format(WEBSOCKET_URL, symbol, symbol);
    log.info(url);
    c.prepareGet(url)
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
          }

          @Override
          public void onTextFrame(String payload, boolean finalFragment, int rsv) {
            RingBuffer<StringEvent> ringBuffer = disruptor.getRingBuffer();
            ringBuffer.publishEvent((event, sequence) -> event.setPayload(payload));
          }
        }).build()).get();
  }

  private Disruptor<StringEvent> createDisruptor() {
    AffinityThreadFactory threadFactory =
      new AffinityThreadFactory("disruptor-consumer", AffinityStrategies.DIFFERENT_CORE);
    Disruptor<StringEvent> disruptor = new Disruptor<>(StringEvent::new, 1024, threadFactory);
    disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
      String payload = event.getPayload();
      Wire wire = WireType.JSON.apply(Bytes.fromString(payload));
      TableEnum table = wire.read("table").asEnum(TableEnum.class);
      ActionEnum action = wire.read("action").asEnum(ActionEnum.class);
      if (table != null) {
        final String data = "data";
        try {
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
        } catch (Exception e) {
          log.error("Disruptor handler came across an exception.", e);
        }
      }
      wire.bytes().release();
    });
    disruptor.start();
    return disruptor;
  }

  private void onEventPosition(ActionEnum action, List<Position> positions) {
    if (action != ActionEnum.partial && buildingPosition) {
      return;
    }
    switch (action) {
      case partial:
        buildingPosition = true;
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
        localOrderStore.insert(orders);
        buildingOrder = false;
        break;
      case insert:
        localOrderStore.insert(orders);
        break;
      case update:
        localOrderStore.update(orders);
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
    tradeData.insert(trades);
  }

  private void onEventOrderBook(ActionEnum action, List<OrderBookEntry> orderBookEntries) {
    if (action != ActionEnum.partial && buildingOrderBook) {
      return;
    }
    switch (action) {
      case partial:
        buildingOrderBook = true;
        onPartialOrderBookEntry(orderBookEntries);
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

  private void onPartialOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    this.orderBook.init(orderBookEntries);
  }

  private void onDeleteOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    this.orderBook.delete(orderBookEntries);
  }



  private void onUpdateOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    this.orderBook.update(orderBookEntries);
  }

  private void onInsertOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    this.orderBook.insert(orderBookEntries);
  }

  public OrderBookEntry getBestBid() {
    return this.orderBook.getBestBid();
  }

  public OrderBookEntry getBestAsk() {
    return this.orderBook.getBestAsk();
  }

  public double getLastBuyPrice() {
    return tradeData.getLastBuyPrice();
  }

  public double getLastSellPrice() {
    return tradeData.getLastSellPrice();
  }


  public Order[] getOrders() {
    return localOrderStore.get();
  }

  public int getOrderArrayIndex() {
    return localOrderStore.getIndex();
  }

  public Position getPosition() {
    return position;
  }

  public boolean ready() {
    return !buildingOrderBook && !buildingTrade && !buildingPosition;
  }

  public String printOrderBook() {
    return orderBook.toString();
  }
}
