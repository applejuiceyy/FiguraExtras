package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultCancellableEvent {
    boolean propagating = true;
    boolean cancelDefault = false;

    List<Element> propagationPath;

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

    public List<Element> getPropagationPath() {
        return Collections.unmodifiableList(propagationPath);
    }

    void setPropagationPath(List<Element> elements) {
        propagationPath = elements;
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

    public static class CausedEvent<T> extends DefaultCancellableEvent {
        public final T motivation;

        public CausedEvent(T motivation) {
            this.motivation = motivation;
        }
    }

    public static class ToolTipEvent extends DefaultCancellableEvent.MousePositionEvent {
        private final Runnable invalidator;
        private final List<Component> components = new ArrayList<>();
        Event<Runnable> disposing = Event.runnable();
        public final Event<Runnable>.Source dispose = disposing.getSource();
        private Flow flow = null;
        private GuiState state = null;
        private List<Element> componentElementMapping = null;

        public ToolTipEvent(double x, double y, Runnable invalidator) {
            super(x, y);
            this.invalidator = invalidator;
        }

        public Runnable add(Component component) {
            if (flow != null) {
                Label label = new Label();
                label.setText(component);
                flow.add(label);
                return () -> flow.remove(label);
            } else {
                components.add(component);
                return () -> {
                    int idx = components.indexOf(component);
                    if (flow != null) {
                        flow.remove(componentElementMapping.remove(idx));
                    }
                    components.remove(idx);
                };
            }
        }

        public Flow getRoot() {
            if (flow == null) {
                flow = new Flow();
                state = new GuiState(flow);
                state.setShouldDoTooltips(false);
                state.setUseBackingRenderTarget(false);
                componentElementMapping = new ArrayList<>();
                components.forEach(component -> {
                    Label label = new Label();
                    label.setText(component);
                    flow.add(label);
                    componentElementMapping.add(label);
                });
            }
            return flow;
        }

        public void invalidate() {
            invalidator.run();
        }

        Either<List<Component>, Flow> get() {
            return flow == null ? Either.left(components) : Either.right(flow);
        }
    }
}
