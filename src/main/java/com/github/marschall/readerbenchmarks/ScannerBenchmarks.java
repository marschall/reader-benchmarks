package com.github.marschall.readerbenchmarks;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MILLISECONDS)
@State(Benchmark)
public class ScannerBenchmarks {

  private static final Pattern J_PATTERN = Pattern.compile("J");

  @Param({"128", "1024", "1048576"})
  int transferSize;

  private Scanner scanner;

  @Setup
  public void doSetup() {
    this.scanner = new Scanner(new VarHanleConstantInputStream(this.transferSize));
    this.scanner.useDelimiter(" ");
  }


  @Benchmark
  public void scan(Blackhole blackhole) {
    if (this.scanner.hasNext()) {
      blackhole.consume(this.scanner.next(J_PATTERN));
    }
  }

  /**
   * A simple infinite input stream for benchmark purposes that always returns the sequence 0x4A (J) 0x20 (space).
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
        LONG_ARRAY_HANDLE.set(b, varHandleBase + (i * 8), pattern);
      }
      remaining -= loopIterations * 8;

      // epilogue, fill rest
      int epilogueBase = varHandleBase + (loopIterations * 8);
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

}
