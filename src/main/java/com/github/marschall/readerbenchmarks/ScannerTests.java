package com.github.marschall.readerbenchmarks;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

import java.util.function.IntFunction;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MICROSECONDS)
@State(Benchmark)
public class ScannerTests {

  private static final Pattern J_PATTERN = Pattern.compile("J");

  private VarHanleConstantInputStream varHanleConstantInputStream;

  private ForConstantInputStream forConstantInputStream;

  private byte[] b;

  private Scanner scanner;

  @Setup
  public void doSetup() {
    int bufferSize = 1024;
    this.b = new byte[bufferSize];
    this.varHanleConstantInputStream = new VarHanleConstantInputStream(bufferSize);
    this.forConstantInputStream = new ForConstantInputStream(bufferSize);
    this.scanner = new Scanner("J J");
    this.scanner.useDelimiter(" ");
  }

  @Benchmark
  public byte[] varHanleConstantInputStream() throws IOException {
    this.varHanleConstantInputStream.read(this.b);
    return this.b;
  }

  @Benchmark
  public byte[] forConstantInputStream() throws IOException {
    this.forConstantInputStream.read(this.b);
    return this.b;
  }

  public void scan(Blackhole blackhole) {
    if (scanner.hasNext(J_PATTERN)) {
      blackhole.consume(scanner.next(J_PATTERN));
    }
  }

  public static void main(String[] args) throws IOException, RunnerException {
//    runScanner();
    runJmh();
  }

  private static void runScanner() throws IOException {
    try (ForConstantInputStream inputStream = new ForConstantInputStream(6)) {
      byte[] data = new byte[15];
      Arrays.fill(data, (byte) 'x');
      inputStream.read(data, 1, 6);
      inputStream.read(data, 7, 1);
      inputStream.read(data, 8, 6);

      System.out.println(new String(data,StandardCharsets.US_ASCII));
    }

    try (InputStream inputStream = new VarHanleConstantInputStream(128)) {
      byte[] data = new byte[8 * 3];
      inputStream.read(data, 0, 6);
      inputStream.read(data, 6, 11);
      inputStream.read(data, 17, 7);
      System.out.println(new String(data,StandardCharsets.US_ASCII));
    }


    // run scanner
    runScanner(ForConstantInputStream::new);
    runScanner(VarHanleConstantInputStream::new);
    runScanner(HandUnrolledConstantInputStream::new);
  }

  private static void runScanner(IntFunction<InputStream> inputStreamFactory) {
    try (Scanner scanner = new Scanner(inputStreamFactory.apply(128), StandardCharsets.US_ASCII)) {
      int i = 0;
      scanner.useDelimiter(" ");
      Pattern pattern = Pattern.compile("J");
      while ((i < 10) && scanner.hasNext(pattern)) {
        String token = scanner.next(pattern);
        System.out.println(token);
        i += 1;
      }
    }
  }

  private static void runJmh() throws RunnerException {
    Options options = new OptionsBuilder()
        .include("com\\.github\\.marschall\\.readerbenchmarks\\.ScannerTests")
        .forks(1)
        .warmupIterations(3)
        .measurementIterations(5)
        //        .resultFormat(ResultFormatType.TEXT)
        .build();
    new Runner(options).run();
  }

  /**
   * A simple infinite input stream for benchmark purposes that always returns the sequence 0x4A 0x20.
   * <p>
   * Implemented using a {@link VarHandle} to write eight bytes at a time.
   */
  static final class VarHanleConstantInputStream extends InputStream {

    static final VarHandle LONG_ARRAY_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());

    private static final long BIG_ENDIAN_PATTERN = 0x4A_20_4A_20_4A_20_4A_20L;

    private static final long LITTLE_ENDIAN_PATTERN = 0x20_4A_20_4A_20_4A_20_4AL;

    private static final long EVEN_PATTERN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? LITTLE_ENDIAN_PATTERN : BIG_ENDIAN_PATTERN;

    private static final long ODD_PATTERN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? BIG_ENDIAN_PATTERN : LITTLE_ENDIAN_PATTERN;

    private final int transferSize;

    private long totalRead;

    VarHanleConstantInputStream(int transferSize) {
      this.transferSize = transferSize;
    }

    @Override
    public int read() throws IOException {
      char c;
      if ((this.totalRead & 1) == 0L) {
        c = 'J';
      } else {
        c = ' ';
      }
      this.totalRead += 1L;
      return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      // lacks argument checks as this code is only used for benchmarks
      // we assume JDK classes call us correctly
      if (len == 0) {
        return 0;
      }
      boolean even = (this.totalRead & 1) == 0L;

      int fillLen = Math.min(len, this.transferSize);
      int remaining = fillLen;

      // prologue, fill up to alignment
      int prefillLen = 8 - (off & 0b111); // remainder mod 8
      prefillLen = Math.min(prefillLen, remaining);
      if (prefillLen != 8) {
        for (int i = 0; i < prefillLen; i++) {
          byte value;
          if (((i & 1) == 0L) == even) {
            value = 'J';
          } else {
            value = ' ';
          }
          b[off + i] = value;
        }
      } else {
        prefillLen = 0;
      }
      remaining -= prefillLen;

      // unrolled loop
      int loopIterations = remaining >>> 3;
      int varHandleBase = off + prefillLen;
      long pattern = even ? EVEN_PATTERN : ODD_PATTERN;
      for (int i = 0; i < loopIterations; i++) {
        // the VarHandle uses the index of the byte, not the long 
        LONG_ARRAY_HANDLE.set(b, varHandleBase + i * 8, pattern);
      }
      remaining -= loopIterations * 8;

      // epilogue, fill rest
      int epilogueBase = varHandleBase + loopIterations * 8;
      for (int i = 0; i < remaining; i++) {
        byte value;
        if (((i & 1) == 0L) == even) {
          value = 'J';
        } else {
          value = ' ';
        }
        b[epilogueBase + i] = value;
      }

      this.totalRead += fillLen;


      return fillLen;
    }

  }

  /**
   * A simple infinite input stream for benchmark purposes that always returns the sequence 0x4A 0x20.
   * <p>
   * Implemented using a hand unrolled loop to write eight bytes at a time.
   */
  static final class HandUnrolledConstantInputStream extends InputStream {

    private final int transferSize;

    private long totalRead;

    HandUnrolledConstantInputStream(int transferSize) {
      this.transferSize = transferSize;
    }

    @Override
    public int read() throws IOException {
      char c;
      if ((this.totalRead & 1) == 0L) {
        c = 'J';
      } else {
        c = ' ';
      }
      this.totalRead += 1L;
      return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      // lacks argument checks as this code is only used for benchmarks
      // we assume JDK classes call us correctly
      if (len == 0) {
        return 0;
      }
      boolean even = (this.totalRead & 1) == 0L;

      int fillLen = Math.min(len, this.transferSize);
      int remaining = fillLen;

      // prologue, fill up to alignment
      int prefillLen = 8 - (off & 0b111); // remainder mod 8
      prefillLen = Math.min(prefillLen, remaining);
      if (prefillLen != 8) {
        for (int i = 0; i < prefillLen; i++) {
          byte value;
          if (((i & 1) == 0L) == even) {
            value = 'J';
          } else {
            value = ' ';
          }
          b[off + i] = value;
        }
      } else {
        prefillLen = 0;
      }
      remaining -= prefillLen;

      // unrolled loop
      int loopIterations = remaining >>> 3;
      int loopBase = off + prefillLen;
      if (even) {
        for (int i = 0; i < loopIterations; i++) {
          int base = loopBase + i * 8;
          b[base] = 'J';
          b[base + 1] = ' ';
          b[base + 2] = 'J';
          b[base + 3] = ' ';
          b[base + 4] = 'J';
          b[base + 5] = ' ';
          b[base + 6] = 'J';
          b[base + 7] = ' ';
        }
      } else {
        for (int i = 0; i < loopIterations; i++) {
          int base = loopBase + i * 8;
          b[base] = ' ';
          b[base + 1] = 'J';
          b[base + 2] = ' ';
          b[base + 3] = 'J';
          b[base + 4] = ' ';
          b[base + 5] = 'J';
          b[base + 6] = ' ';
          b[base + 7] = 'j';
        }
      }
      remaining -= loopIterations * 8;

      // epilogue, fill rest
      int epilogueBase = loopBase + loopIterations * 8;
      for (int i = 0; i < remaining; i++) {
        byte value;
        if (((i & 1) == 0L) == even) {
          value = 'J';
        } else {
          value = ' ';
        }
        b[epilogueBase + i] = value;
      }

      this.totalRead += fillLen;


      return fillLen;
    }

  }

  /**
   * A simple infinite input stream for benchmark purposes that always returns the sequence 0x4A 0x20.
   */
  static final class ForConstantInputStream extends InputStream {

    private final int transferSize;

    private long totalRead;

    ForConstantInputStream(int transferSize) {
      this.transferSize = transferSize;
    }

    @Override
    public int read() throws IOException {
      char c;
      if ((this.totalRead & 1) == 0L) {
        c = 'J';
      } else {
        c = ' ';
      }
      this.totalRead += 1L;
      return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      // lacks argument checks as this code is only used for benchmarks
      // we assume JDK classes call us correctly
      int fillLen = Math.min(len, this.transferSize);
      
      boolean even = (this.totalRead & 1) == 0L;

      for (int i = 0; i < fillLen; i++) {
        byte value;
        if (((i & 1) == 0L) == even) {
          value = 'J';
        } else {
          value = ' ';
        }
        b[off + i] = value;
      }

      this.totalRead += fillLen;


      return fillLen;
    }

  }

}
