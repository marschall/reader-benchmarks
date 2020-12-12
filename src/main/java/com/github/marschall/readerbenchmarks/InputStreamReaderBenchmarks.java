package com.github.marschall.readerbenchmarks;

import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Level.Trial;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
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
public class InputStreamReaderBenchmarks {

  @State(Benchmark)
  public static class ReaderState {

    private CharsetDecoder decoder;

    private CharBuffer heapBuffer;

    private CharBuffer directBuffer;

    private ByteArrayInputStream inputStream;

    @Param({"128", "1024", "1048576"})
    int targetBufferSize;

    @Param({"128", "1024", "1048576"})
    int inputSize;

    @Setup(Trial)
    public void doSetup() throws IOException {
      byte[] bytes = new byte[this.inputSize];
      Arrays.fill(bytes, (byte) 'A');
      this.inputStream = new ByteArrayInputStream(bytes);
      this.inputStream.mark(this.inputSize);

      this.decoder = US_ASCII.newDecoder()
              .onMalformedInput(REPLACE)
              .onUnmappableCharacter(REPLACE);
      this.heapBuffer = CharBuffer.allocate(this.targetBufferSize);
      this.directBuffer = ByteBuffer.allocateDirect(this.targetBufferSize * 2).asCharBuffer();
    }

  }

  @Benchmark
  public void readHeapBuffer(ReaderState state, Blackhole blackhole) throws IOException {
    state.decoder.reset();
    state.inputStream.reset();
    InputStreamReader reader = new InputStreamReader(state.inputStream, state.decoder);

    if (state.inputSize > state.targetBufferSize) {
      int iterations = state.inputSize / state.targetBufferSize;
      for (int i = 0; i < iterations; i++) {
        state.heapBuffer.clear();
        blackhole.consume(reader.read(state.heapBuffer));
      }
    } else {
      state.heapBuffer.clear();
      blackhole.consume(reader.read(state.heapBuffer));
    }
  }

  @Benchmark
  public void readDirectBuffer(ReaderState state, Blackhole blackhole) throws IOException {
    state.decoder.reset();
    state.inputStream.reset();
    InputStreamReader reader = new InputStreamReader(state.inputStream, state.decoder);

    if (state.inputSize > state.targetBufferSize) {
      int iterations = state.inputSize / state.targetBufferSize;
      for (int i = 0; i < iterations; i++) {
        state.directBuffer.clear();
        blackhole.consume(reader.read(state.directBuffer));
      }
    } else {
      state.directBuffer.clear();
      blackhole.consume(reader.read(state.directBuffer));
    }
  }

}
