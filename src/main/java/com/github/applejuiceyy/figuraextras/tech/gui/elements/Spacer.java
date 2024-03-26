package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;

public class Spacer extends Element {
    private final int width;
    private final int height;

    public Spacer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static Spacer nothing() {
        return new Spacer(0, 0);
    }

    @Override
    public int computeOptimalWidth() {
        return width;
    }

    @Override
    public int computeOptimalHeight(int width) {
        return height;
    }
}
