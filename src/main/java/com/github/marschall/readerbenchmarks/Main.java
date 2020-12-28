package com.github.marschall.readerbenchmarks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


public class Main {

  public static void main(String[] args) throws RunnerException, IOException {
    String fileName = args.length > 0 ? args[0] + ".csv" : "4926314.csv";
    Options options = new OptionsBuilder()
        .include("com\\.github\\.marschall\\.readerbenchmarks\\..*Benchmarks")
        .forks(1)
        .warmupIterations(3)
        .measurementIterations(5)
        .resultFormat(ResultFormatType.CSV)
        .output(fileName)
        .addProfiler("gc")
        .build();
    new Runner(options).run();

//    Path.of("jmh-result.csv");
    Path jmhResult = Paths.get("jmh-result.csv");
    if (Files.exists(jmhResult) && (args.length > 0)) {
      Files.move(jmhResult, Paths.get(args[0] + "-jmh-result.csv"), StandardCopyOption.REPLACE_EXISTING);
    }
  }

}
