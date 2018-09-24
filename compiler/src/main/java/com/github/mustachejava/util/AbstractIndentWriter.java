package com.github.mustachejava.util;

import java.io.IOException;

public abstract class AbstractIndentWriter extends IndentWriter {
    protected final IndentWriter inner;

    public AbstractIndentWriter(IndentWriter inner) {
        this.inner = inner;
    }

    @Override
    public void writeLines(char[][] lines) throws IOException {
        inner.writeLines(lines);
    }

    @Override
    public void flushIndent() throws IOException {
        inner.flushIndent();
    }

    @Override
    public void setPrependIndent() {
        inner.setPrependIndent();
    }

    @Override
    public void write(char[] chars, int i, int i1) throws IOException {
        inner.write(chars, i, i1);
    }

    @Override
    public void flush() throws IOException {
        inner.flush();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }
}
