package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import org.jetbrains.annotations.Nullable;

public interface Rectangle {
    static Rectangle of(int x, int y, int width, int height) {
        return new Default(x, y, width, height);
    }

    static Rectangle ofSize(int width, int height) {
        return of(0, 0, width, height);
    }

    static Rectangle empty() {
        return new Default(0, 0, 0, 0);
    }

    int getX();

    void setX(int x);

    int getY();

    void setY(int y);

    int getWidth();

    void setWidth(int width);

    int getHeight();

    void setHeight(int height);

    default boolean intersects(double x, double y) {
        return x >= this.getX()
                && y >= this.getY()
                && x < (this.getX() + getWidth())
                && y < (this.getY() + getHeight());
    }

    default @Nullable Rectangle intersection(Rectangle rectangle) {
        int i = Math.max(this.getX(), rectangle.getX());
        int j = Math.max(this.getY(), rectangle.getY());
        int k = Math.min(this.getX() + this.getWidth(), rectangle.getX() + rectangle.getWidth());
        int l = Math.min(this.getY() + this.getHeight(), rectangle.getY() + rectangle.getHeight());
        return i < k && j < l ? of(i, j, k - i, l - j) : null;
    }

    default boolean equal(Rectangle other) {
        return other.getX() == getX() &&
                other.getY() == getY() &&
                other.getWidth() == getWidth() &&
                other.getHeight() == getHeight();
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
