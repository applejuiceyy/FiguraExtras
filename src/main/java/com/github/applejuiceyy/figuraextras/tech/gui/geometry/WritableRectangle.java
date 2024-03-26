package com.github.applejuiceyy.figuraextras.tech.gui.geometry;

public interface WritableRectangle {
    void setX(int x);

    void setY(int y);

    void setWidth(int width);

    void setHeight(int height);

    default void copyFrom(ReadableRectangle other) {
        setX(other.getX());
        setY(other.getY());
        setWidth(other.getWidth());
        setHeight(other.getHeight());
    }
}
