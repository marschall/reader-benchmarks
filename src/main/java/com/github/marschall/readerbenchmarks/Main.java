package com.github.marschall.readerbenchmarks;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


public class Main {

  public static void main(String[] args) throws RunnerException {
    Options options = new OptionsBuilder()
        .include(".*((ReaderBenchmarks)|(InputStreamReaderBenchmarks)).*")
        .forks(1)
        .warmupIterations(3)
        .measurementIterations(5)
        .resultFormat(ResultFormatType.TEXT)
//        .addProfiler(HotspotMemoryProfiler.class)
        .jvmArgsAppend("-Djmh.blackhole.mode=COMPILER")
        .build();
    new Runner(options).run();
  }

}
