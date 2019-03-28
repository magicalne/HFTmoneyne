package magicalne.github.io.trade;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;

import java.util.List;

import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;

public class BitMexHttpRequestEncoder extends HttpRequestEncoder {
  private static final int CRLF_SHORT = (CR << 8) | LF;
  private static final float HEADERS_WEIGHT_NEW = 1 / 5f;
  private static final float HEADERS_WEIGHT_HISTORICAL = 1 - HEADERS_WEIGHT_NEW;

  /**
   * Used to calculate an exponential moving average of the encoded size of the initial line and the headers for
   * a guess for future buffer allocations.
   */
  private float headersEncodedSizeAccumulator = 256;

  ByteBuf encode(DefaultFullHttpRequest req) throws Exception {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer((int) headersEncodedSizeAccumulator);

    // Encode the message.
    encodeInitialLine(buf, req);
    encodeHeaders(req.headers(), buf);
    ByteBufUtil.writeShortBE(buf, CRLF_SHORT);

    headersEncodedSizeAccumulator = HEADERS_WEIGHT_NEW * padSizeForAccumulation(buf.readableBytes()) +
      HEADERS_WEIGHT_HISTORICAL * headersEncodedSizeAccumulator;
    buf.writeBytes(req.content());
    return buf;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
    if (msg instanceof ByteBuf) {
      out.add(msg);
    } else {
      super.encode(ctx, msg, out);
    }
  }

  private int padSizeForAccumulation(int readableBytes) {
    return (readableBytes << 2) / 3;
  }

}
