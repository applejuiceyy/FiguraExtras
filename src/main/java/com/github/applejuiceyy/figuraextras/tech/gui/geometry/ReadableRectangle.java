package com.github.applejuiceyy.figuraextras.tech.gui.geometry;

import org.jetbrains.annotations.Nullable;

public interface ReadableRectangle {
    int getX();

    int getY();

    int getWidth();

    int getHeight();

    default ImmutableRectangle immutable() {
        return new ImmutableRectangle(getX(), getY(), getWidth(), getHeight());
    }

    default boolean isEmpty() {
        return getWidth() == 0 && getHeight() == 0;
    }

    default boolean intersects(double x, double y) {
        return x >= this.getX()
                && y >= this.getY()
                && x < (this.getX() + getWidth())
                && y < (this.getY() + getHeight());
    }

    default @Nullable Rectangle intersection(ReadableRectangle rectangle) {
        int i = Math.max(this.getX(), rectangle.getX());
        int j = Math.max(this.getY(), rectangle.getY());
        int k = Math.min(this.getX() + this.getWidth(), rectangle.getX() + rectangle.getWidth());
        int l = Math.min(this.getY() + this.getHeight(), rectangle.getY() + rectangle.getHeight());
        return i < k && j < l ? Rectangle.of(i, j, k - i, l - j) : null;
    }

    default Rectangle reunion(ReadableRectangle rectangle) {
        int i = Math.min(this.getX(), rectangle.getX());
        int j = Math.min(this.getY(), rectangle.getY());
        int k = Math.max(this.getX() + this.getWidth(), rectangle.getX() + rectangle.getWidth());
        int l = Math.max(this.getY() + this.getHeight(), rectangle.getY() + rectangle.getHeight());
        return Rectangle.of(i, j, k - i, l - j);
    }

    default boolean equal(ReadableRectangle other) {
        return other.getX() == getX() &&
                other.getY() == getY() &&
                other.getWidth() == getWidth() &&
                other.getHeight() == getHeight();
    }

    default Rectangle copy() {
        Rectangle rect = Rectangle.empty();
        rect.copyFrom(this);
        return rect;
    }

    default ImmutableRectangle copyImmutable() {
        return Rectangle.ofImmutable(getX(), getY(), getWidth(), getHeight());
    }

    default Rectangle makeMutable() {
        return copy();
    }
}
