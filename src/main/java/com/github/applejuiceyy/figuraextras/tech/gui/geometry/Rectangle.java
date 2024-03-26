package com.github.applejuiceyy.figuraextras.tech.gui.geometry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface Rectangle extends ReadableRectangle, WritableRectangle {
    ImmutableRectangle EMPTY_IMMUTABLE = new ImmutableRectangle(0, 0, 0, 0);

    static Rectangle of(int x, int y, int width, int height) {
        return new Default(x, y, width, height);
    }

    static Rectangle ofSize(int width, int height) {
        return of(0, 0, width, height);
    }

    static ImmutableRectangle ofImmutable(int x, int y, int width, int height) {
        if (x == 0 && y == 0 && width == 0 && height == 0)
            return EMPTY_IMMUTABLE;
        return new ImmutableRectangle(x, y, width, height);
    }

    static ImmutableRectangle ofImmutableSize(int width, int height) {
        return ofImmutable(0, 0, width, height);
    }

    static Rectangle empty() {
        return new Default(0, 0, 0, 0);
    }

    @Contract("null,null->null;!null,null->param1;null,!null->param2")
    static @Nullable ReadableRectangle expansiveIntersectionOf(@Nullable ReadableRectangle a, @Nullable ReadableRectangle b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.intersection(b);
    }

    @Contract("null,null->null;!null,null->null;null,!null->null")
    static @Nullable ReadableRectangle restrictiveIntersectionOf(@Nullable ReadableRectangle a, @Nullable ReadableRectangle b) {
        if (a == null || b == null) {
            return null;
        }
        return a.intersection(b);
    }

    @Contract("null,null->null;!null,null->param1;null,!null->param2;!null,!null->new")
    static @Nullable ReadableRectangle reunionOf(@Nullable ReadableRectangle a, @Nullable ReadableRectangle b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.reunion(b);
    }

    @Override
    default Rectangle makeMutable() {
        return this;
    }

    class Default implements Rectangle {
        private int x;
        private int y;
        private int width;
        private int height;

        public Default(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public void setX(int x) {
            this.x = x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public void setY(int y) {
            this.y = y;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public void setWidth(int width) {
            this.width = width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void setHeight(int height) {
            this.height = height;
        }
    }
}
