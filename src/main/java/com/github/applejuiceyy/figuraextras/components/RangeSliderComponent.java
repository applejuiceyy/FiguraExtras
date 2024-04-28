package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.client.gui.GuiGraphics;

public class RangeSliderComponent extends Element {
    private int max = 100;
    private int min = 0;
    public final Observers.WritableObserver<Integer> lowerKnob = Observers.of(0);
    public final Observers.WritableObserver<Integer> higherKnob = Observers.of(100);
    int minSpacing = 10;

    int startHigherKnob = 0;
    int startLowerKnob = 0;
    float offset = 0;

    boolean dragging;
    boolean draggingLowerKnob;
    boolean draggingHigherKnob;

    public RangeSliderComponent() {

    }

    public void setMax(int max) {
        this.max = max;
        constraintsUpdated();
        enqueueDirtySection(false, false);
    }

    public int getLowerKnob() {
        return lowerKnob.get();
    }

    public RangeSliderComponent setLowerKnob(int lowerKnob) {
        this.lowerKnob.set(lowerKnob);

        enqueueDirtySection(false, false);
        return this;
    }

    public int getHigherKnob() {
        return higherKnob.get();
    }

    public RangeSliderComponent setHigherKnob(int higherKnob) {
        this.higherKnob.set(higherKnob);

        enqueueDirtySection(false, false);
        return this;
    }

    public void setMin(int min) {
        this.min = min;
        constraintsUpdated();
        if (higherKnob.get() < min) {
            higherKnob.set(min);
        }

        enqueueDirtySection(false, false);
    }

    public void setMinSpacing(int minSpacing) {
        this.minSpacing = minSpacing;
        constraintsUpdated();
    }

    public void constraintsUpdated() {
        if (higherKnob.get() > max) {
            higherKnob.set(max);
        }
        if (lowerKnob.get() < min) {
            lowerKnob.set(min);
        }
    }

    private int knobPosition(int knob) {
        return (int) ((knob - min) / (float) max * width.get());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int lowerKnob = knobPosition(this.lowerKnob.get());
        int higherKnob = knobPosition(this.higherKnob.get());

        context.fill(lowerKnob + x.get(), y.get() + height.get() / 2 - 8, higherKnob + x.get(), y.get() + height.get() / 2 - 2, 0xffff0000);

        context.fill(lowerKnob + x.get() - 1, y.get(), lowerKnob + x.get() + 1, y.get() + height.get(), 0xffffffff);
        context.fill(higherKnob + x.get() - 1, y.get(), higherKnob + x.get() + 1, y.get() + height.get(), 0xffffffff);

        context.fill(lowerKnob + x.get() - 5, (int) (y.get() + height.get() * (2 / 3f)), lowerKnob + x.get() + 5, y.get() + height.get(), 0xffffffff);
        context.fill(higherKnob + x.get() - 5, (int) (y.get() + height.get() * (2 / 3f)), higherKnob + x.get() + 5, y.get() + height.get(), 0xffffffff);

    }

    @Override
    protected boolean renders() {
        return true;
    }

    @Override
    public Element.HoverIntent mouseHoverIntent(double mouseX, double mouseY) {
        return HoverIntent.INTERACT;
    }

    @Override
    protected void defaultMouseDownBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {
        int lowerKnob = knobPosition(this.lowerKnob.get());
        int higherKnob = knobPosition(this.higherKnob.get());
        draggingHigherKnob = false;
        draggingLowerKnob = false;
        offset = 0;
        int w = 1;
        if (event.y > height.get() * (2 / 3f)) {
            w = 5;
        }
        if (event.y > getY() && event.y < getHeight() + getY() && event.x > lowerKnob - w + getX() && event.x < higherKnob + w + getX()) {
            dragging = true;
            startLowerKnob = this.lowerKnob.get();
            startHigherKnob = this.higherKnob.get();
            if (event.x < lowerKnob + w + getX()) {
                draggingLowerKnob = true;
            } else if (event.x < higherKnob - w + getX()) {
                draggingHigherKnob = true;
                draggingLowerKnob = true;
            } else {
                draggingHigherKnob = true;
            }
        }
    }


    @Override
    protected void defaultMouseDraggedBehaviour(DefaultCancellableEvent.MousePositionButtonDeltaEvent event) {
        if (dragging) {
            offset += event.deltaX * (max - min) / width.get();
            if (draggingHigherKnob) {
                higherKnob.set((int) (startHigherKnob + offset));
                if (higherKnob.get() > max) {
                    higherKnob.set(max);
                }
                if (higherKnob.get() < min + minSpacing) {
                    higherKnob.set(min + minSpacing);
                }
                if (lowerKnob.get() > higherKnob.get() - minSpacing) {
                    lowerKnob.set(higherKnob.get() - minSpacing);
                }
            }
            if (draggingLowerKnob) {
                lowerKnob.set((int) (startLowerKnob + offset));
                if (lowerKnob.get() < min) {
                    lowerKnob.set(min);
                }
                if (lowerKnob.get() > max - minSpacing) {
                    lowerKnob.set(max - minSpacing);
                }
                if (higherKnob.get() < lowerKnob.get() + minSpacing) {
                    higherKnob.set(lowerKnob.get() + minSpacing);
                }
            }

            enqueueDirtySection(false, false);
        }
    }

    @Override
    protected void defaultMouseUpBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {
        if (dragging) {
            dragging = false;
        }
    }

    @Override
    public int computeOptimalWidth() {
        return 50;
    }

    @Override
    public int computeOptimalHeight(int width) {
        return 20;
    }
}
