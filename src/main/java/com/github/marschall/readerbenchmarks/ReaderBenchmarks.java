package com.github.marschall.readerbenchmarks;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Level.Trial;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MICROSECONDS)
public class ReaderBenchmarks {

  @State(Benchmark)
  public static class ReaderState {

    private StringReader reader;

    private CharBuffer heapBuffer;

    private CharBuffer directBuffer;

    @Param({"128", "1024", "1048576"})
    int targetBufferSize;

    @Param({"128", "1024", "1048576"})
    int inputSize;

    @Setup(Trial)
    public void doSetup() throws IOException {
      this.reader = new StringReader("a".repeat(this.inputSize));
      this.reader.mark(this.targetBufferSize);

      this.heapBuffer = CharBuffer.allocate(this.targetBufferSize);
      this.directBuffer = ByteBuffer.allocateDirect(this.targetBufferSize * 2).asCharBuffer();
    }

  }

  @Benchmark
  public int readHeapBuffer(ReaderState state) throws IOException {
    state.heapBuffer.clear();
    state.reader.reset();

    return state.reader.read(state.heapBuffer);
  }

  @Benchmark
  public int readDirectBuffer(ReaderState state) throws IOException {
    state.directBuffer.clear();
    state.reader.reset();

    return state.reader.read(state.directBuffer);
  }

}
