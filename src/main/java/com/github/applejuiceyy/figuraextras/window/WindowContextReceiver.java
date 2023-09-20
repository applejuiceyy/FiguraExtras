package com.github.applejuiceyy.figuraextras.window;

public interface WindowContextReceiver {
    void acknowledge(WindowContext context);

    default void windowActive(boolean focused) {
    }

    boolean testTransparency(Double a, Double b);
}
