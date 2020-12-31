package com.github.marschall.readerbenchmarks;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openjdk.jmh.annotations.Level.Trial;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
@OutputTimeUnit(MILLISECONDS)
public class InputStreamReaderBenchmarks {

  @State(Benchmark)
  public static class ReaderState {

    private CharBuffer heapBuffer;

    private CharBuffer directBuffer;

    private InputStreamReader reader;

    @Param({"128", "1024", "1048576"})
    int targetBufferSize;

    @Param({"128", "1024", "1048576"})
    int transferSize;

    @Param({"US-ASCII", "ISO-8859-1", "UTF-8"})
    String charsetName;

    @Setup(Trial)
    public void doSetup() throws IOException {
      this.reader = new InputStreamReader(new ConstantInputStream(this.transferSize), this.charsetName);

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
   * A simple infinite input stream for benchmark purposes that always returns the byte.
   */
  static final class ConstantInputStream extends InputStream {

    private final int transferSize;

    ConstantInputStream(int transferSize) {
      this.transferSize = transferSize;
    }

    @Override
    public int read() throws IOException {
      return (byte) 'J';
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      // lacks argument checks as this code is only used for benchmarks
      // we assume JDK classes call us correctly
      int fillLen = Math.min(len, this.transferSize);
      Arrays.fill(b, off, off + fillLen, (byte) 'J');
      return fillLen;
    }

    @Override
    public int available() throws IOException {
      return this.transferSize;
    }

  }

}
