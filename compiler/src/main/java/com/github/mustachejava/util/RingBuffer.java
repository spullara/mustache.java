package com.github.mustachejava.util;

/**
* A very simple ring buffer you can use for parsing and keeping a look behind.
*/
public class RingBuffer {
  private final int ringSize;
  private final int ringHash;
  private final char[] ring;
  private int length = 0;

  public RingBuffer(int ringSize) {
    int i = 1;
    while (i < ringSize) {
      i *= 2;
    }
    this.ringSize = i;
    this.ringHash = i - 1;
    ring = new char[i];
  }

  public void append(char c) {
    ring[length++ & ringHash] = c;
  }

  public void clear() {
    length = 0;
  }

  public boolean compare(String s, boolean exact) {
    int len = s.length();
    if (exact && len != length) return false;
    if (len > ringSize) {
      throw new IllegalArgumentException("Ring buffer too small: " + ringSize + " < " + s.length());
    }
    if (length >= len) {
      int j = 0;
      int position = length & ringHash;
      for (int i = position - len; i < position; i++) {
        char c;
        if (i < 0) {
          c = ring[ringSize + i];
        } else {
          c = ring[i];
        }
        if (s.charAt(j++) != c) {
          return false;
        }
      }
    }
    return true;
  }

}
