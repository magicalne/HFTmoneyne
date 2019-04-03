package magicalne.github.io.util;

import com.google.common.math.Stats;
import magicalne.github.io.wire.bitmex.OrderBookEntry;
import magicalne.github.io.wire.bitmex.SideEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ConcurrentSkipListMapPerformanceTest {

  private ConcurrentSkipListMap<Long, OrderBookEntry> test;

  @Before
  public void setUp() {
    test = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    for (int i = 0; i < 25; i ++) {
      OrderBookEntry entry = new OrderBookEntry("test", i, SideEnum.Buy, 10, 3000);
      this.test.put((long) i, entry);
    }
  }

  @Test
  public void test() {
    ScheduledExecutorService insert = Executors.newSingleThreadScheduledExecutor();
    insert.scheduleAtFixedRate(() -> {
      test.put(1L, new OrderBookEntry("test", 1, SideEnum.Buy, 10, 5000));
    }, 0, 10, TimeUnit.MICROSECONDS);
    List<Long> durations = new LinkedList<>();
    for (int i = 0; i < 10000; i ++) {
      long start = System.nanoTime();
      Map.Entry<Long, OrderBookEntry> e = test.lastEntry();
      e.getValue();
      long end = System.nanoTime();
      durations.add(end - start);
    }
    insert.shutdown();
    Stats stats = Stats.of(durations);
    System.out.println(stats.toString());
  }
}
