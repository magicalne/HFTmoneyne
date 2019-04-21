package magicalne.github.io.market;

import com.google.common.hash.HashFunction;
import com.lmax.disruptor.dsl.Disruptor;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.ByteBufferEvent;
import magicalne.github.io.trade.BitMexTradeService;
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@ChannelHandler.Sharable
public class BitMexMarketHandler extends SimpleChannelInboundHandler<Object> {

  private static final String AUTH = "{\"op\": \"authKeyExpires\", \"args\": [\"%s\", %d, \"%s\"]}";
  private static final String SUB = "{\"op\": \"subscribe\", \"args\": [\"%s:%s\"]}";
  private static final Bytes<ByteBuffer> BYTES = Bytes.elasticByteBuffer();

  private final MyWebSocketClientHandshaker handShaker;
  private final String accessSecret;
  private final String accessKey;
  private final String symbol;
  private final BitMexTradeService tradeService;
  private final Disruptor<ByteBufferEvent> disruptor;

  private volatile boolean buildingOrderBook;
  private final BitMexOrderBook orderBook;

  private volatile boolean buildingTrade;
  private final MarketTradeData tradeData;

  private volatile boolean buildingOrder;
  private final LocalOrderStore localOrderStore;

  private volatile boolean buildingPosition;
  private volatile Position position;
  private final WireType json;

  public BitMexMarketHandler(MyWebSocketClientHandshaker handShaker,
                             String accessKey, String accessSecret, String symbol,
                             int tradeTimeout, BitMexTradeService tradeService)
    throws IllegalAccessException, InstantiationException {
    this.handShaker = handShaker;
    this.accessKey = accessKey;
    this.accessSecret = accessSecret;
    this.symbol = symbol;
    this.tradeService = tradeService;

    this.json = WireType.JSON;

    this.buildingOrderBook = true;
    this.orderBook = new BitMexOrderBook(25);

    this.buildingTrade = true;
    this.tradeData = new MarketTradeData(tradeTimeout);

    this.buildingOrder = true;
    this.localOrderStore = new LocalOrderStore(200);

    this.buildingPosition = true;

    this.disruptor = createDisruptor();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    log.info("pipeline init as: {}", ctx.pipeline().names());
    handShaker.handshake0(ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    log.info("WebSocket Client disconnected!");
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) {
    ctx.channel().eventLoop().execute(() -> {
      log.info("Reconnecting...");
      BitMexMarketService.connect();
    });
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof FullHttpResponse) {
      FullHttpResponse res = (FullHttpResponse) msg;
      handShaker.finishHandshake0(ctx.channel(), res);
      log.info("WebSocket Client connected!");
      String subOrderBookL2 = String.format(SUB, "orderBookL2_25", symbol);
      ctx.writeAndFlush(new TextWebSocketFrame(subOrderBookL2));
      String subTrade = String.format(SUB, "trade", symbol);
      ctx.writeAndFlush(new TextWebSocketFrame(subTrade));
      long expires = System.currentTimeMillis() + 5000;
      HashFunction hashFunction = Utils.bitmexSignatureHashFunction(accessSecret);
      String signature = hashFunction.hashString("GET/realtime" + expires, StandardCharsets.UTF_8).toString();
      String auth = String.format(AUTH, accessKey, expires, signature);
      ctx.writeAndFlush(new TextWebSocketFrame(auth));
      String subOrder = String.format(SUB, "order", symbol);
      ctx.writeAndFlush(subOrder);
      ctx.writeAndFlush(new TextWebSocketFrame(subOrder));
      String subPosition = String.format(SUB, "position", symbol);
      ctx.writeAndFlush(new TextWebSocketFrame(subPosition));
    } else if (msg instanceof WebSocketFrame) {
      WebSocketFrame frame = (WebSocketFrame) msg;
      if (frame instanceof TextWebSocketFrame) {
        TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
        this.disruptor.publishEvent((event, sequence) -> event.setByteBuffer(textFrame.content().retain().nioBuffer()));
      } else if (frame instanceof PongWebSocketFrame) {
        log.info("WebSocket Client received pong");
      } else if (frame instanceof CloseWebSocketFrame) {
        log.info("WebSocket Client received closing");
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Exception happened.", cause);
    ctx.close();
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

  public double tradeBalance() {
    return tradeData.imbalance();
  }

  public double imbalance() {
    return orderBook.imbalance();
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
    return !buildingOrderBook && !buildingTrade && !buildingPosition && !buildingOrder;
  }

  public String printOrderBook() {
    return orderBook.toString();
  }

  private Disruptor<ByteBufferEvent> createDisruptor() {
    AffinityThreadFactory threadFactory =
      new AffinityThreadFactory("disruptor-market-consumer", AffinityStrategies.DIFFERENT_CORE);

    Disruptor<ByteBufferEvent> disruptor = new Disruptor<>(ByteBufferEvent::new, 1024, threadFactory);
    disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
      ByteBuffer byteBuffer = event.getByteBuffer();
      BYTES.clear();
      BYTES.write(BYTES.writePosition(), byteBuffer, byteBuffer.position(), byteBuffer.limit());
      BYTES.writePosition(byteBuffer.limit());
      Wire wire = json.apply(BYTES);
      TableEnum table = wire.read("table").asEnum(TableEnum.class);
      log.debug("table: {}", table);
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
    long ns = System.nanoTime();
    for (OrderBookEntry e : orderBookEntries) {
      if (e.getSide() == SideEnum.Buy && e.getId() == this.orderBook.getBestBid().getId()) {
        this.tradeService.placeOrder(e.getPrice(), SideEnum.Sell, ns);
        log.info("Rob ask order on {}.", e.getPrice());
      } else if (e.getSide() == SideEnum.Sell && e.getId() == this.orderBook.getBestAsk().getId()) {
        this.tradeService.placeOrder(e.getPrice(), SideEnum.Buy, ns);
        log.info("Rob bid order on {}.", e.getPrice());
      }
    }
    this.orderBook.delete(orderBookEntries);
  }

  private void onUpdateOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    this.orderBook.update(orderBookEntries);
  }

  private void onInsertOrderBookEntry(List<OrderBookEntry> orderBookEntries) {
    this.orderBook.insert(orderBookEntries);
  }
}
