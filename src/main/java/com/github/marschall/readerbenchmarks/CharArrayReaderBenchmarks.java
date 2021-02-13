package com.github.marschall.readerbenchmarks;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.CharArrayReader;
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

@BenchmarkMode(Throughput)
@OutputTimeUnit(MICROSECONDS)
@State(Benchmark)
public class CharArrayReaderBenchmarks {

  private CharBuffer charBuffer;

  @Param({"true", "false"})
  boolean onHeap;

  @Param({"true", "false"})
  boolean latin1;

  @Param({"128", "1024"})
  int transferSize;

  private Reader charArrayReader;

  @Setup
  public void setup() throws IOException {
    if (this.onHeap) {
      this.charBuffer = CharBuffer.allocate(this.transferSize);
    } else {
      this.charBuffer = ByteBuffer.allocateDirect(this.transferSize).asCharBuffer();
    }
    char[] c;
    if (this.latin1) {
      c = this.createLatin1Array(this.transferSize);
    } else {
      c = this.createNonLatin1Array(this.transferSize);
    }
    this.charArrayReader = new CharArrayReader(c);
    this.charArrayReader.mark(this.transferSize);
  }

  private char[] createLatin1Array(int size) {
    char[] c = new char[size];
    Arrays.fill(c, 'A');
    return c;
  }

  private char[] createNonLatin1Array(int size) {
    char[] c = new char[size];
    Arrays.fill(c, '\u20AC');
    return c;
  }

  @Benchmark
  public int read() throws IOException {
    this.charBuffer.clear();
    this.charArrayReader.reset();
    return this.charArrayReader.read(this.charBuffer);
  }

}
