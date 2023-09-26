package com.github.applejuiceyy.figuraextras.components;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;

public class RangeSliderComponent extends BaseComponent {
    private int max = 100;
    private int min = 0;
    public int lowerKnob = 0;
    public int higherKnob = 100;
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

    }

    public void setMin(int min) {
        this.min = min;
        constraintsUpdated();
        if (higherKnob < min) {
            higherKnob = min;
        }
    }

    public void setMinSpacing(int minSpacing) {
        this.minSpacing = minSpacing;
        constraintsUpdated();
    }

    public void constraintsUpdated() {
        if (higherKnob > max) {
            higherKnob = max;
        }
        if (lowerKnob < min) {
            lowerKnob = min;
        }

        int currentSpacing = higherKnob - lowerKnob;
        if (currentSpacing < minSpacing) {
            int diff = currentSpacing - minSpacing;
            int newHigh = higherKnob + min;
            if (newHigh > max) {
                int highDiff = newHigh - max;
            }
        }
    }

    private int knobPosition(int knob) {
        return (int) ((knob - min) / (float) max * width);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        int lowerKnob = knobPosition(this.lowerKnob);
        int higherKnob = knobPosition(this.higherKnob);

        context.fill(lowerKnob + x, y + height / 2 - 8, higherKnob + x, y + height / 2 - 2, 0xffff0000);

        context.fill(lowerKnob + x - 1, y, lowerKnob + x + 1, y + height, 0xffffffff);
        context.fill(higherKnob + x - 1, y, higherKnob + x + 1, y + height, 0xffffffff);

        context.fill(lowerKnob + x - 5, (int) (y + height * (2 / 3f)), lowerKnob + x + 5, y + height, 0xffffffff);
        context.fill(higherKnob + x - 5, (int) (y + height * (2 / 3f)), higherKnob + x + 5, y + height, 0xffffffff);

    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        int lowerKnob = knobPosition(this.lowerKnob);
        int higherKnob = knobPosition(this.higherKnob);
        draggingHigherKnob = false;
        draggingLowerKnob = false;
        offset = 0;
        int w = 1;
        if (mouseY > height * (2 / 3f)) {
            w = 5;
        }
        if (mouseY > 0 && mouseY < height && mouseX > lowerKnob - w && mouseX < higherKnob + w) {
            dragging = true;
            startLowerKnob = this.lowerKnob;
            startHigherKnob = this.higherKnob;
            if (mouseX < lowerKnob + w) {
                draggingLowerKnob = true;
            } else if (mouseX < higherKnob - w) {
                draggingHigherKnob = true;
                draggingLowerKnob = true;
            } else {
                draggingHigherKnob = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return true;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (dragging) {
            offset += deltaX * (max - min) / width;
            if (draggingHigherKnob) {
                higherKnob = (int) (startHigherKnob + offset);
                if (higherKnob > max) {
                    higherKnob = max;
                }
                if (higherKnob < min + minSpacing) {
                    higherKnob = min + minSpacing;
                }
                if (lowerKnob > higherKnob - minSpacing) {
                    lowerKnob = higherKnob - minSpacing;
                }
            }
            if (draggingLowerKnob) {
                lowerKnob = (int) (startLowerKnob + offset);
                if (lowerKnob < min) {
                    lowerKnob = min;
                }
                if (lowerKnob > max - minSpacing) {
                    lowerKnob = max - minSpacing;
                }
                if (higherKnob < lowerKnob + minSpacing) {
                    higherKnob = lowerKnob + minSpacing;
                }
            }
        }
        return dragging;
    }

    @Override
    public boolean onMouseUp(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }
}
