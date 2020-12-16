package com.github.marschall.readerbenchmarks;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Level.Trial;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MICROSECONDS)
public class ReaderBenchmarks {

  @State(Benchmark)
  public static class ReaderState {

    private Reader reader;

    private CharBuffer heapBuffer;

    private CharBuffer directBuffer;

    @Param({"128", "1024", "1048576"})
    int targetBufferSize;

    @Param({"128", "1024", "1048576"})
    int transferSize;

    @Setup(Trial)
    public void doSetup() throws IOException {
      this.reader = new ConstantReader(this.transferSize);

      this.heapBuffer = CharBuffer.allocate(this.targetBufferSize);
      this.directBuffer = ByteBuffer.allocateDirect(this.targetBufferSize * 2).asCharBuffer();
    }

  }

  @Benchmark
  public void readHeapBuffer(ReaderState state, Blackhole blackhole) throws IOException {
    state.heapBuffer.clear();
    int remaining = state.targetBufferSize;
    do {
      int nread = state.reader.read(state.heapBuffer);
      blackhole.consume(nread);
      if (nread > 0) {
        remaining -= nread;
      }
      if (nread == -1) {
        break;
      }
    } while (remaining > 0);
  }

  @Benchmark
  public void readDirectBuffer(ReaderState state, Blackhole blackhole) throws IOException {
    state.directBuffer.clear();
    int remaining = state.targetBufferSize;
    do {
      int nread = state.reader.read(state.directBuffer);
      blackhole.consume(nread);
      if (nread > 0) {
        remaining -= nread;
      }
      if (nread == -1) {
        break;
      }
    } while (remaining > 0);
  }

  /**
   * A simple infinite reader for benchmark purposes that always returns the same character.
   */
  static final class ConstantReader extends Reader {

    private final int transferSize;

    ConstantReader(int transferSize) {
      this.transferSize = transferSize;
    }

    // intentionally don't override #read(CharBuffer) as this is the method we want to benchmark

    @Override
    public int read() throws IOException {
      return 'J';
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      int fillLen = Math.min(len, this.transferSize);
      // in theory we could leave this out as we only want to benchmark the #read(CharBuffer) method
      Arrays.fill(cbuf, off, off + fillLen, 'a');
      return fillLen;
    }

    @Override
    public void close() throws IOException {
      // ignore
    }

  }

}
