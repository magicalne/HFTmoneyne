package magicalne.github.io.trade;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.http.*;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class BitMexHttpClientCodec extends CombinedChannelDuplexHandler<HttpResponseDecoder, HttpRequestEncoder>
  implements HttpClientUpgradeHandler.SourceCodec {

  /**
   * A queue that is used for correlating a request and a response.
   */
  private final Queue<HttpMethod> queue = new ArrayDeque<>();
  private final boolean parseHttpAfterConnectRequest;

  /**
   * If true, decoding stops (i.e. pass-through)
   */
  private boolean done;

  private final AtomicLong requestResponseCounter = new AtomicLong();
  private final boolean failOnMissingResponse;

  /**
   * Creates a new instance with the default decoder options
   */
  BitMexHttpClientCodec() {
    BitMexHttpRequestEncoder encoder = new BitMexHttpRequestEncoder();
    init(new Decoder(4096, 8192, 8192, true), encoder);
    this.failOnMissingResponse = false;
    this.parseHttpAfterConnectRequest = false;
  }

  @Override
  public void prepareUpgradeFrom(ChannelHandlerContext ctx) {
  }

  @Override
  public void upgradeFrom(ChannelHandlerContext ctx) {
    final ChannelPipeline p = ctx.pipeline();
    p.remove(this);
  }

  private final class Decoder extends HttpResponseDecoder {
    Decoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
      super(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders);
    }

    @Override
    protected void decode(
      ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
      if (done) {
        int readable = actualReadableBytes();
        if (readable == 0) {
          // if non is readable just return null
          // https://github.com/netty/netty/issues/1159
          return;
        }
        out.add(buffer.readBytes(readable));
      } else {
        int oldSize = out.size();
        super.decode(ctx, buffer, out);
        if (failOnMissingResponse) {
          int size = out.size();
          for (int i = oldSize; i < size; i++) {
            decrement(out.get(i));
          }
        }
      }
    }

    private void decrement(Object msg) {
      if (msg == null) {
        return;
      }

      // check if it's an Header and its transfer encoding is not chunked.
      if (msg instanceof LastHttpContent) {
        requestResponseCounter.decrementAndGet();
      }
    }

    @Override
    protected boolean isContentAlwaysEmpty(HttpMessage msg) {
      final int statusCode = ((HttpResponse) msg).status().code();
      if (statusCode == 100 || statusCode == 101) {
        // 100-continue and 101 switching protocols response should be excluded from paired comparison.
        // Just delegate to super method which has all the needed handling.
        return super.isContentAlwaysEmpty(msg);
      }

      // Get the getMethod of the HTTP request that corresponds to the
      // current response.
      HttpMethod method = queue.poll();

      char firstChar = method.name().charAt(0);
      switch (firstChar) {
        case 'H':
          // According to 4.3, RFC2616:
          // All responses to the HEAD request method MUST NOT include a
          // message-body, even though the presence of entity-header fields
          // might lead one to believe they do.
          if (HttpMethod.HEAD.equals(method)) {
            return true;

            // The following code was inserted to work around the servers
            // that behave incorrectly.  It has been commented out
            // because it does not work with well behaving servers.
            // Please note, even if the 'Transfer-Encoding: chunked'
            // header exists in the HEAD response, the response should
            // have absolutely no content.
            //
            //// Interesting edge case:
            //// Some poorly implemented servers will send a zero-byte
            //// chunk if Transfer-Encoding of the response is 'chunked'.
            ////
            //// return !msg.isChunked();
          }
          break;
        case 'C':
          // Successful CONNECT request results in a response with empty body.
          if (statusCode == 200) {
            if (HttpMethod.CONNECT.equals(method)) {
              // Proxy connection established - Parse HTTP only if configured by parseHttpAfterConnectRequest,
              // else pass through.
              if (!parseHttpAfterConnectRequest) {
                done = true;
                queue.clear();
              }
              return true;
            }
          }
          break;
      }

      return super.isContentAlwaysEmpty(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
      throws Exception {
      super.channelInactive(ctx);

      if (failOnMissingResponse) {
        long missingResponses = requestResponseCounter.get();
        if (missingResponses > 0) {
          ctx.fireExceptionCaught(new PrematureChannelClosureException(
            "channel gone inactive with " + missingResponses +
              " missing response(s)"));
        }
      }
    }
  }

}
