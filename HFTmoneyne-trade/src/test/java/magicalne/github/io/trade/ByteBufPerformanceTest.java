package magicalne.github.io.trade;

import com.google.common.math.Stats;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.internal.PlatformDependent;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class ByteBufPerformanceTest {

  @Test
  public void pooledDirectByteBufTest() {
    PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
    System.out.println("Is direct? " + PlatformDependent.directBufferPreferred());
    System.out.println("Access retained duplicated");
    List<ByteBuf> list = new LinkedList<>();
    for (int i = 0; i < 1000; i ++) {
      ByteBuf buffer = allocator.buffer(4);
      buffer.writeInt(i);
      list.add(buffer);
    }
    List<Long> durations = new LinkedList<>();
    for (ByteBuf byteBuf : list) {
      long start = System.nanoTime();
      ByteBuf duplicate = byteBuf.retainedDuplicate();
      duplicate.release();
      long end = System.nanoTime();
      durations.add(end - start);
      byteBuf.release();
    }
    Stats stats = Stats.of(durations);
    System.out.println(stats.toString());
    System.out.println("#########################");
    System.out.println("Access duplicated and retain");
    list.clear();
    for (int i = 0; i < 1000; i ++) {
      ByteBuf buffer = allocator.buffer(4);
      buffer.writeInt(i);
      list.add(buffer);
    }
    durations.clear();
    for (ByteBuf byteBuf : list) {
      long start = System.nanoTime();
      ByteBuf duplicate = byteBuf.duplicate().retain();
      duplicate.release();
      long end = System.nanoTime();
      durations.add(end - start);
      byteBuf.release();
    }
    stats = Stats.of(durations);
    System.out.println(stats.toString());
  }

  @Test
  public void unpooledDirectByteBufTest() {
    UnpooledByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
    System.out.println("Is direct? " + PlatformDependent.directBufferPreferred());
    System.out.println("Access retained duplicated");
    List<ByteBuf> list = new LinkedList<>();
    for (int i = 0; i < 1000; i ++) {
      ByteBuf buffer = allocator.buffer(4);
      buffer.writeInt(i);
      list.add(buffer);
    }
    List<Long> durations = new LinkedList<>();
    for (ByteBuf byteBuf : list) {
      long start = System.nanoTime();
      ByteBuf duplicate = byteBuf.retainedDuplicate();
      duplicate.release();
      long end = System.nanoTime();
      durations.add(end - start);
      byteBuf.release();
    }
    Stats stats = Stats.of(durations);
    System.out.println(stats.toString());
    System.out.println("#########################");
    System.out.println("Access duplicated and retain");
    list.clear();
    for (int i = 0; i < 1000; i ++) {
      ByteBuf buffer = allocator.buffer(4);
      buffer.writeInt(i);
      list.add(buffer);
    }
    durations.clear();
    for (ByteBuf byteBuf : list) {
      long start = System.nanoTime();
      ByteBuf duplicate = byteBuf.duplicate().retain();
      duplicate.release();
      long end = System.nanoTime();
      durations.add(end - start);
      byteBuf.release();
    }
    stats = Stats.of(durations);
    System.out.println(stats.toString());
  }
}
