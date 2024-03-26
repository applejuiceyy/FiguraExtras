package com.github.applejuiceyy.figuraextras.tech.gui.geometry;

public final class ImmutableRectangle implements ReadableRectangle {
    final int x, y, width, height;

    public ImmutableRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public ImmutableRectangle immutable() {
        return this;
    }
}
