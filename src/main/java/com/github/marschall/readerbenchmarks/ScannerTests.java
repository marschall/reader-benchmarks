package com.github.marschall.readerbenchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ScannerTests {

  public static void main(String[] args) throws IOException {
    try (ForConstantInputStream inputStream = new ForConstantInputStream(6)) {
      byte[] data = new byte[15];
      Arrays.fill(data, (byte) 'x');
      inputStream.read(data, 1, 6);
      inputStream.read(data, 7, 1);
      inputStream.read(data, 8, 6);

      System.out.println(new String(data,StandardCharsets.US_ASCII));
    }

    byte[] data = new byte[8];
    Arrays.fill(data, (byte) 'x');
    VarHanleConstantInputStream.fillArray(data, 0);

    System.out.println(new String(data,StandardCharsets.US_ASCII));

    try (Scanner scanner = new Scanner(new ForConstantInputStream(128), StandardCharsets.US_ASCII)) {
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

  /**
   * A simple infinite input stream for benchmark purposes that always returns the sequence 0x4A 0x20.
   */
  static final class VarHanleConstantInputStream extends InputStream {

    static final VarHandle LONG_ARRAY_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());

    private static final long BIG_ENDIAN_PATTERN = 0x4A_20_4A_20_4A_20_4A_20L;

    private static final long LITTLE_ENDIAN_PATTERN = 0x20_4A_20_4A_20_4A_20_4AL;

    static final long PATTERN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? LITTLE_ENDIAN_PATTERN : BIG_ENDIAN_PATTERN;

    private final int transferSize;

    private long totalRead;

    VarHanleConstantInputStream(int transferSize) {
      this.transferSize = transferSize;
    }

    static void fillArray(byte[] b, int off) {
      LONG_ARRAY_HANDLE.set(b, off, PATTERN);
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

      int fillLen = Math.min(len, this.transferSize);
      if ((this.totalRead & 1) == 1) {
        b[off++] = ' ';
        fillLen -= 1;
      }

      for (int i = 0; i < (fillLen - 1); i += 2) {
        b[off + i] = 'J';
        b[off + i + 1] = ' ';
      }

      if ((fillLen & 1) == 1) {
        b[(off + fillLen) - 1] = 'J';
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

      int fillLen = Math.min(len, this.transferSize);
      if ((this.totalRead & 1) == 1) {
        b[off++] = ' ';
        fillLen -= 1;
      }

      for (int i = 0; i < (fillLen - 1); i += 2) {
        b[off + i] = 'J';
        b[off + i + 1] = ' ';
      }

      if ((fillLen & 1) == 1) {
        b[(off + fillLen) - 1] = 'J';
      }

      this.totalRead += fillLen;


      return fillLen;
    }

  }

}
