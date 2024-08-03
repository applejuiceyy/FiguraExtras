package com.github.applejuiceyy.figuraextras.window;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import org.lwjgl.glfw.GLFWDropCallback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

// basically a copy of MouseHandler but actually only deals with the mouse
// there's way too much garbage in the original for mixins to apply
public class ContextAgnosticMouseHandler {
    private final Window window;
    private final Callback callback;
    private boolean isLeftPressed;
    private boolean isMiddlePressed;
    private boolean isRightPressed;
    private double xPos;
    private double yPos;
    private int fakeRightMouse;
    private int activeButton = -1;
    private boolean ignoreFirstMove = true;
    private int clickDepth;
    private double mousePressedTime;

    public ContextAgnosticMouseHandler(Window window, Callback callback) {
        this.window = window;
        this.callback = callback;
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    private void onPress(long windowHandle, int button, int action, int modifiers) {
        if (windowHandle == window.getWindow()) {
            boolean bl = action == 1;
            if (Minecraft.ON_OSX && button == 0) {
                if (bl) {
                    if ((modifiers & 2) == 2) {
                        button = 1;
                        ++this.fakeRightMouse;
                    }
                } else if (this.fakeRightMouse > 0) {
                    button = 1;
                    --this.fakeRightMouse;
                }
            }

            if (bl) {
                if (getMinecraft().options.touchscreen().get() && this.clickDepth++ > 0) {
                    return;
                }

                this.activeButton = button;
                this.mousePressedTime = Blaze3D.getTime();
            } else if (this.activeButton != -1) {
                if (getMinecraft().options.touchscreen().get() && --this.clickDepth > 0) {
                    return;
                }

                this.activeButton = -1;
            }

            if (bl) {
                callback.onCLick(this.xPos(), this.yPos(), button);
            } else {
                callback.onRelease(this.xPos(), this.yPos(), button);
            }

            if (button == 0) {
                this.isLeftPressed = bl;
            } else if (button == 2) {
                this.isMiddlePressed = bl;
            } else if (button == 1) {
                this.isRightPressed = bl;
            }
        }
    }

    private void onScroll(long windowHandle, double scrollDeltaX, double scrollDeltaY) {
        if (windowHandle == window.getWindow()) {
            double d = (getMinecraft().options.discreteMouseScroll().get() ? Math.signum(scrollDeltaY) : scrollDeltaY) * getMinecraft().options.mouseWheelSensitivity().get();
            callback.onScroll(this.xPos(), this.yPos(), d);
        }
    }

    private void onDrop(long windowHandle, List<Path> paths) {
        if (windowHandle == window.getWindow()) {
            callback.filesDropped(paths);
        }
    }

    public void setup() {
        InputConstants.setupMouseCallbacks(window.getWindow(), (windowx, x, y) -> {
            getMinecraft().execute(() -> {
                this.onMove(windowx, x, y);
            });
        }, (windowx, button, action, modifiers) -> {
            getMinecraft().execute(() -> {
                this.onPress(windowx, button, action, modifiers);
            });
        }, (windowx, offsetX, offsetY) -> {
            getMinecraft().execute(() -> {
                this.onScroll(windowx, offsetX, offsetY);
            });
        }, (windowx, count, names) -> {
            Path[] paths = new Path[count];

            for (int i = 0; i < count; ++i) {
                paths[i] = Paths.get(GLFWDropCallback.getName(names, i));
            }

            getMinecraft().execute(() -> {
                this.onDrop(windowx, Arrays.asList(paths));
            });
        });
    }

    private void onMove(long windowHandle, double x, double y) {
        if (windowHandle == window.getWindow()) {
            if (this.ignoreFirstMove) {
                this.ignoreFirstMove = false;
                return;
            }

            double d = x / window.getGuiScale();
            double e = y / window.getGuiScale();

            callback.mouseMoved(d, e);
            if (this.activeButton != -1 && this.mousePressedTime > 0.0) {
                double f = (d - this.xPos());
                double g = (e - this.yPos());
                callback.mouseDragged(d, e, this.activeButton, f, g);
            }

            this.xPos = x;
            this.yPos = y;
        }
    }

    public Tuple<Double, Double> toGuiScale(double x, double y) {
        return new Tuple<>(
                x * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth(),
                y * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight()
        );
    }

    public boolean isLeftPressed() {
        return this.isLeftPressed;
    }

    public boolean isMiddlePressed() {
        return this.isMiddlePressed;
    }

    public boolean isRightPressed() {
        return this.isRightPressed;
    }

    public double xPos() {
        return this.xPos / window.getGuiScale();
    }

    public double yPos() {
        return this.yPos / window.getGuiScale();
    }

    public void setIgnoreFirstMove() {
        this.ignoreFirstMove = true;
    }


    // not needed but may decide to fix it
    /*
    public void grabMouse() {
        if (this.minecraft.isWindowActive()) {
            if (!this.mouseGrabbed) {
                if (!Minecraft.ON_OSX) {
                    KeyMapping.setAll();
                }

                this.mouseGrabbed = true;
                this.xpos = (double)(this.minecraft.getWindow().getScreenWidth() / 2);
                this.ypos = (double)(this.minecraft.getWindow().getScreenHeight() / 2);
                InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212995, this.xpos, this.ypos);
                this.minecraft.setScreen((Screen)null);
                this.minecraft.missTime = 10000;
                this.ignoreFirstMove = true;
            }
        }
    }

    public void releaseMouse() {
        if (this.mouseGrabbed) {
            this.mouseGrabbed = false;
            this.xpos = (double)(this.minecraft.getWindow().getScreenWidth() / 2);
            this.ypos = (double)(this.minecraft.getWindow().getScreenHeight() / 2);
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212993, this.xpos, this.ypos);
        }
    }*/

    public void cursorEntered() {
        this.ignoreFirstMove = true;
    }

    public interface Callback {
        void onCLick(double d, double e, int button);

        void onRelease(double d, double e, int button);

        void onScroll(double e, double f, double d);

        void filesDropped(List<Path> paths);

        void mouseMoved(double d, double e);

        void mouseDragged(double d, double e, int activeButton, double f, double g);
    }
}
