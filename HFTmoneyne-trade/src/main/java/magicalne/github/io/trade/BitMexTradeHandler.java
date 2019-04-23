package magicalne.github.io.trade;

import com.google.common.base.Strings;
import io.netty.channel.Channel;
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
      if (statusCode > 200 && statusCode < 500) {
        String retryAfter = headers.get("Retry-After");
        if (Strings.isNullOrEmpty(retryAfter)) {
          try {
            retryForResetLimitTime = Long.parseLong(retryAfter) * 1000 + System.currentTimeMillis();
          } catch (NumberFormatException e) {
            log.error("Parse retryAfter to long failed. retryAfter = {}, {}", retryAfter, e);
            retryForResetLimitTime = System.currentTimeMillis() + 1000;
          }
        } else {
          String rateRemainValue = headers.get("X-RateLimit-Remaining");
          if (Strings.isNullOrEmpty(rateRemainValue)) {
            int remain = Integer.parseInt(rateRemainValue);
            if (remain == 0) {
              retryForResetLimitTime = 1000 + System.currentTimeMillis();
            }
          }
        }
      } else if (statusCode > 500) {
        retryForOverloadTime = System.currentTimeMillis() + 500;
      }
    } else {
      if (retryForResetLimitTime > 0) {
        retryForResetLimitTime = -1;
      }
      if (retryForOverloadTime > 0) {
        retryForOverloadTime = -1;
      }
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

  public void sendRequest(Object req) {
    if (invalidate()) return;
    if (this.ctx != null && ctx.channel().isActive()) {
      ctx.writeAndFlush(req, ctx.voidPromise());
    }
  }
}
