package magicalne.github.io.trade;

import com.google.common.base.Strings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class BitMexTradeHandler extends TradeHandler {

  BitMexTradeHandler(String host, int port) {
    super(host, port);
  }


  @Override
  void headerHandler(int statusCode, HttpHeaders headers) {
    if (statusCode != 200) {
      log.warn("Status code: {}, headers: {}", statusCode, headers);
    }
    switch (statusCode) {
      case 503:
        retryForOverloadTime = System.currentTimeMillis() + 500;
        break;
      case 429:
        String retryAfter = headers.get("Retry-After");
        if (Strings.isNullOrEmpty(retryAfter)) {
          retryForResetLimitTime = Long.parseLong(retryAfter) * 1000 + System.currentTimeMillis();
        }
        break;
      case 200:
        if (retryForResetLimitTime > 0) {
          retryForResetLimitTime = -1;
        }
        if (retryForOverloadTime > 0) {
          retryForOverloadTime = -1;
        }
        break;
    }
  }

  @Override
  boolean invalidate() {
    if (this.retryForOverloadTime < 0 && this.retryForResetLimitTime < 0) {
      return false;
    } else {
      long now = System.currentTimeMillis();
      if (now < this.retryForOverloadTime) {
        return true;
      } else {
        this.retryForOverloadTime = -1;
      }
      if (now < this.retryForResetLimitTime) {
        return true;
      } else {
        this.retryForResetLimitTime = -1;
      }
      return false;
    }
  }

  @Override
  void keepAlive(Channel channel) {
    log.info("Send keep-alive request");
    DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/healthcheck");
    req.headers().add(HttpHeaderNames.HOST, host);
    sendRequest(req);
  }

  public ChannelFuture sendRequest(Object req) {
    if (invalidate()) return null;
    if (this.ctx != null && ctx.channel().isActive()) {
      return ctx.writeAndFlush(req);
    }
    return null;
  }
}
