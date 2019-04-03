package magicalne.github.io.util.integration;

import magicalne.github.io.util.MarketTradeData;
import magicalne.github.io.wire.bitmex.SideEnum;
import magicalne.github.io.wire.bitmex.TradeEntry;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MarketTradeDataPerformance {
  @Test
  public void launchBenchmark() throws Exception {

    Options opt = new OptionsBuilder()
      // Specify which benchmarks to run.
      // You can be more specific if you'd like to run only one benchmark per test.
      .include(this.getClass().getName() + ".*")
      // Set the following options as needed
      .mode (Mode.AverageTime)
      .timeUnit(TimeUnit.MICROSECONDS)
      .warmupTime(TimeValue.seconds(1))
      .warmupIterations(2)
      .measurementTime(TimeValue.seconds(1))
      .measurementIterations(2)
      .threads(2)
      .forks(1)
      .shouldFailOnError(true)
      .shouldDoGC(true)
//      .jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
      //.addProfiler(WinPerfAsmProfiler.class)
      .build();

    new Runner(opt).run();
  }

  // The JMH samples are the best documentation for how to use it
  // http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/
  @State (Scope.Thread)
  public static class BenchmarkState {
    private MarketTradeData marketTradeData;

    @Setup(Level.Trial)
    public void initialize() throws IllegalAccessException, InstantiationException {

      marketTradeData = new MarketTradeData(0);
      Random rand = new Random();

      for (int i = 0; i < 1000; i++) {
        LinkedList<TradeEntry> trades = new LinkedList<>();
        for (int j = 0; j < 10; j++) {
          TradeEntry tradeEntry = new TradeEntry();
          tradeEntry.setSide(rand.nextBoolean() ? SideEnum.Buy : SideEnum.Sell);
          tradeEntry.setPrice(rand.nextDouble());
          trades.add(tradeEntry);
        }
        marketTradeData.insert(trades);
      }
    }
  }

  @Benchmark
  public void benchmark(BenchmarkState state, Blackhole bh) {

    MarketTradeData marketTradeData = state.marketTradeData;
    Random rand = new Random();

    for (int i = 0; i < 5000; i++) {
      LinkedList<TradeEntry> trades = new LinkedList<>();
      for (int j = 0; j < 10; j++) {
        TradeEntry tradeEntry = new TradeEntry();
        tradeEntry.setSide(rand.nextBoolean() ? SideEnum.Buy : SideEnum.Sell);
        tradeEntry.setPrice(rand.nextDouble());
        trades.add(tradeEntry);
      }
      marketTradeData.insert(trades);
    }
  }

  @Benchmark
  public void singleBenchmark(BenchmarkState state, Blackhole bh) {

    MarketTradeData marketTradeData = state.marketTradeData;
    Random rand = new Random();

    LinkedList<TradeEntry> trades = new LinkedList<>();
    for (int j = 0; j < 10; j++) {
      TradeEntry tradeEntry = new TradeEntry();
      tradeEntry.setSide(rand.nextBoolean() ? SideEnum.Buy : SideEnum.Sell);
      tradeEntry.setPrice(rand.nextDouble());
      trades.add(tradeEntry);
    }
    marketTradeData.insert(trades);
  }
}
