package magicalne.github.io.httpclient;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClientResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    if (msg instanceof HttpResponse) {
      HttpResponse response = (HttpResponse) msg;

      System.out.println("STATUS: " + response.getStatus());
      System.out.println("VERSION: " + response.getProtocolVersion());
      System.out.println();
      if (!response.headers().isEmpty()) {
        for (String name: response.headers().names()) {
          for (String value: response.headers().getAll(name)) {
            System.out.println("HEADER: " + name + " = " + value);
          }
        }
        System.out.println();
      }

      if (HttpHeaders.isTransferEncodingChunked(response)) {
        System.out.println("CHUNKED CONTENT {");
      } else {
        System.out.println("CONTENT {");
      }
    }
    if (msg instanceof HttpContent) {
      HttpContent content = (HttpContent) msg;

      System.out.print(content.content().toString(CharsetUtil.UTF_8));
      System.out.flush();

      if (content instanceof LastHttpContent) {
        System.out.println("} END OF CONTENT");
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Exception:", cause);
    ctx.close();
  }

}
