package com.github.marschall.readerbenchmarks;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


public class Main {

  public static void main(String[] args) throws RunnerException {
    String fileName = args.length > 0 ? args[0] + ".txt" : "4926314.txt";
    Options options = new OptionsBuilder()
        .include("com\\.github\\.marschall\\.readerbenchmarks\\..*Benchmarks")
        .forks(1)
        .warmupIterations(3)
        .measurementIterations(5)
        .resultFormat(ResultFormatType.TEXT)
        .output(fileName)
//        .addProfiler("hs_gc")
        .addProfiler("gc")
        .build();
    new Runner(options).run();
  }

}
