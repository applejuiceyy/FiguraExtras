package com.github.applejuiceyy.figuraextras.tech.gui.basics;

public class DefaultCancellableEvent {
    boolean propagating = true;
    boolean cancelDefault = false;

    public void cancelPropagation() {
        propagating = false;
    }

    public boolean isPropagating() {
        return propagating;
    }

    public boolean cancellingDefault() {
        return cancelDefault;
    }

    public void cancelDefault() {
        cancelDefault = true;
    }

    public static class KeyEvent extends DefaultCancellableEvent {
        public final int keyCode, scanCode, modifiers;

        public KeyEvent(int keyCode, int scanCode, int modifiers) {
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.modifiers = modifiers;
        }
    }

    public static class CharEvent extends DefaultCancellableEvent {
        public final char chr;
        public final int modifiers;

        public CharEvent(char chr, int modifiers) {
            this.chr = chr;
            this.modifiers = modifiers;
        }
    }


    public static class MousePositionEvent extends DefaultCancellableEvent {
        public final double x, y;

        public MousePositionEvent(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class MousePositionButtonEvent extends MousePositionEvent {
        public final int button;

        public MousePositionButtonEvent(double x, double y, int button) {
            super(x, y);
            this.button = button;
        }
    }

    public static class MousePositionAmountEvent extends MousePositionEvent {
        public final double amount;

        public MousePositionAmountEvent(double x, double y, double amount) {
            super(x, y);
            this.amount = amount;
        }
    }

    public static class MousePositionButtonDeltaEvent extends MousePositionButtonEvent {
        public final double deltaX, deltaY;

        public MousePositionButtonDeltaEvent(double x, double y, int button, double deltaX, double deltaY) {
            super(x, y, button);
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }
    }
}
