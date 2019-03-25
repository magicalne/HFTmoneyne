package magicalne.github.io.httpclient;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeoutHandler extends ChannelDuplexHandler {

  private final HttpClient httpClient;

  TimeoutHandler(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    log.info("Disconnected from: {}", ctx.channel().remoteAddress());
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      log.info("event state: {}", event);
      if (event.state() == IdleState.READER_IDLE || event.state() == IdleState.WRITER_IDLE) {
        log.info("Disconnected due to no in/out traffic.");
        ctx.close();
      }
    }
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) {
    ctx.channel().eventLoop().submit(() -> {
      log.info("Reconnecting...");
      try {
        httpClient.connect();
      } catch (InterruptedException e) {
        log.error("Reconnect failed.", e);
      }
    });
  }
}
