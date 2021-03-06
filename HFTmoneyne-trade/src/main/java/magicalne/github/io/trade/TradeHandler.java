package magicalne.github.io.trade;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Contended;

@Slf4j
public abstract class TradeHandler extends SimpleChannelInboundHandler<Object> {

  @Contended
  volatile long retryForOverloadTime = -1;
  @Contended
  volatile long retryForResetLimitTime = -1;
  protected final String host;
  private final int port;

  private boolean forceShutDown = false;
  ChannelHandlerContext ctx;

  TradeHandler(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpResponse) {
      HttpResponse response = (HttpResponse) msg;
      if (!response.headers().isEmpty()) {
        headerHandler(response.status().code(), response.headers());
        if (response.status().code() == 403) {
          forceShutDown = true;
          ctx.close();
          log.warn("Stop trading for 1 hour due to 403 status code.");
        }
      }
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    log.info("Connected to server. channel active: {}", ctx.channel().isActive());
    log.info("pipeline: {}", ctx.pipeline().names());
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) {
    log.info("Disconnected from: {}", ctx.channel().remoteAddress());
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) {
    if (!forceShutDown) {
      ctx.channel().eventLoop().execute(() -> {
        log.info("Reconnecting to {}:{}", BitMexTradeService.host, BitMexTradeService.port);
        BitMexTradeService.connect();
      });
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Caught exception: ", cause);
    ctx.close();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      if (event.state() == IdleState.READER_IDLE ||
        event.state() == IdleState.WRITER_IDLE ||
        event.state() == IdleState.ALL_IDLE) {
        keepAlive(ctx.channel());
      }
    }
  }

  abstract void headerHandler(int code, HttpHeaders headers);
  abstract boolean invalidate();
  abstract void keepAlive(Channel channel);
}
