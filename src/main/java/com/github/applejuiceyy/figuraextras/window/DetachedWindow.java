package com.github.applejuiceyy.figuraextras.window;

import com.github.applejuiceyy.figuraextras.ducks.MinecraftAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.WindowDuck;
import com.github.applejuiceyy.figuraextras.screen.MainInfoScreen;
import com.github.applejuiceyy.figuraextras.screen.ScreenContainer;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Tuple;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.WindowProcI;

import java.util.OptionalInt;
import java.util.function.Supplier;

public class DetachedWindow implements WindowContext {
    public final SecondaryWindow window;
    public ScreenContainer screenContainer = null;

    Window mainWindow;
    WindowContentPopOutHost host;

    public DetachedWindow() {
        // garbage code 0/10
        mainWindow = Minecraft.getInstance().getWindow();

        var ref = new Object() {
            ContextAgnosticMouseHandler handler = null;
        };

        window = new SecondaryWindow((screen) -> {
            WindowDuck.hints = () -> GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
            Window window = screen.newWindow(new DisplayData(300, 200, OptionalInt.empty(), OptionalInt.empty(), false), null, "Debug Window");
            WindowDuck.hints = null;

            if (Util.getPlatform() == Util.OS.WINDOWS) {
                long nativeHwnd = GLFWNativeWin32.glfwGetWin32Window(window.getWindow());
                long mainNative = GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().getWindow());
                User32.SetWindowLongPtr(nativeHwnd, User32.GWL_HWNDPARENT, mainNative);
                long original = User32.GetWindowLongPtr(nativeHwnd, User32.GWL_WNDPROC);

                WindowProcI proc = (hwnd, message, wparam, lparam) -> {
                    long result = User32.nCallWindowProc(original, hwnd, message, wparam, lparam);
                    if (hwnd == nativeHwnd && result == User32.HTCLIENT && message == User32.WM_NCHITTEST) {
                        double[] x = {0}, y = {0};
                        // I don't trust mouseHandler to be giving accurate information at this stage
                        GLFW.glfwGetCursorPos(window.getWindow(), x, y);
                        Tuple<Double, Double> scaled = ref.handler.toGuiScale(x[0], y[0]);
                        if (screenContainer.getScreen() instanceof WindowContextReceiver receiver &&
                                !receiver.testTransparency(scaled.getA(), scaled.getB())) {
                            return User32.HTTRANSPARENT;
                        }
                    }
                    return result;
                };

                User32.SetWindowLongPtr(nativeHwnd, User32.GWL_WNDPROC, proc.address());
            }

            return window;
        }, new SecondaryWindow.Callback() {
            @Override
            public void keyReleased(int key, int scancode, int modifiers) {
                screenContainer.withContext(() -> {
                    screenContainer.doEvent("beforeKeyRelease", key, scancode, modifiers);
                    screenContainer.getScreen().keyReleased(key, scancode, modifiers);
                    screenContainer.doEvent("afterKeyRelease", key, scancode, modifiers);
                });
            }

            @Override
            public void keyPressed(int key, int scancode, int modifiers) {
                screenContainer.withContext(() -> {
                    screenContainer.doEvent("beforeKeyPress", key, scancode, modifiers);
                    screenContainer.getScreen().keyPressed(key, scancode, modifiers);
                    screenContainer.doEvent("afterKeyPress", key, scancode, modifiers);
                });
            }

            @Override
            public void charTyped(char codePoint, int modifiers) {
                screenContainer.withContext(() -> {
                    screenContainer.getScreen().charTyped(codePoint, modifiers);
                });
            }

            @Override
            public void mouseClicked(double x, double y, int button) {
                screenContainer.withContext(() -> {
                    screenContainer.doEvent("beforeMouseClick", x, y, button);
                    if (host.onMouseDown(x, y, button)) {
                        return;
                    }
                    screenContainer.getScreen().mouseClicked(x, y, button);
                    screenContainer.doEvent("afterMouseClick", x, y, button);
                });
            }

            @Override
            public void mouseReleased(double x, double y, int button) {
                screenContainer.withContext(() -> {
                    screenContainer.doEvent("beforeMouseRelease", x, y, button);
                    if (host.onMouseRelease(x, y, button)) {
                        return;
                    }
                    screenContainer.getScreen().mouseReleased(x, y, button);
                    screenContainer.doEvent("afterMouseRelease", x, y, button);
                });
            }

            @Override
            public void mouseScrolled(double x, double y, double d) {
                screenContainer.withContext(() -> {
                    screenContainer.doEvent("beforeMouseScroll", x, y, 0, d);
                    screenContainer.getScreen().mouseScrolled(x, y, d);
                    screenContainer.doEvent("afterMouseScroll", x, y, 0, d);
                });
            }

            @Override
            public void mouseMoved(double x, double y) {
                screenContainer.withContext(() -> screenContainer.getScreen().mouseMoved(x, y));
            }

            @Override
            public void mouseDragged(double x, double y, int activeButton, double mx, double my) {
                screenContainer.withContext(() -> {
                    if (host.onMouseDrag(x, y, activeButton, mx, my)) {
                        return;
                    }
                    screenContainer.getScreen().mouseDragged(x, y, activeButton, mx, my);
                });
            }

            @Override
            public void resizeDisplay(int guiScaledWidth, int guiScaledHeight) {
                screenContainer.withContext(() -> screenContainer.getScreen().resize(Minecraft.getInstance(), guiScaledWidth, guiScaledHeight));
            }

            @Override
            public void windowActive(boolean focused) {
                if (screenContainer.getScreen() instanceof WindowContextReceiver receiver) {
                    receiver.windowActive(focused);
                }
            }
        });
        host = new WindowContentPopOutHost(window.window);
        ref.handler = window.mouseHandler;
        screenContainer = new ScreenContainer(MainInfoScreen::new) {
            @Override
            public int getInnerWidth() {
                return window.window.getGuiScaledWidth();
            }

            @Override
            public int getInnerHeight() {
                return window.window.getGuiScaledHeight();
            }

            @Override
            protected <T> T _withContext(Supplier<T> running) {
                return ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$withWindow(window.window, window.renderTarget, running);
            }

            @Override
            protected void newScreen() {
                if (this.getScreen() instanceof WindowContextReceiver receiver) {
                    receiver.acknowledge(DetachedWindow.this);
                }
            }
        };
    }

    public void render(GuiGraphics guiGraphics) {
        screenContainer.doEvent("beforeRender", guiGraphics, (int) window.mouseHandler.xpos(), (int) window.mouseHandler.ypos(), Minecraft.getInstance().getDeltaFrameTime());
        screenContainer.getScreen().render(guiGraphics, (int) window.mouseHandler.xpos(), (int) window.mouseHandler.ypos(), Minecraft.getInstance().getDeltaFrameTime());
        screenContainer.doEvent("afterRender", guiGraphics, (int) window.mouseHandler.xpos(), (int) window.mouseHandler.ypos(), Minecraft.getInstance().getDeltaFrameTime());
        host.render(guiGraphics, (int) window.mouseHandler.xpos(), (int) window.mouseHandler.ypos(), Minecraft.getInstance().getDeltaFrameTime());
        guiGraphics.flush();
    }

    public void tick() {
        screenContainer.doEvent("beforeTick");
        screenContainer.getScreen().tick();
        screenContainer.doEvent("beforeTick");
    }

    public void close() {
        window.close();
        screenContainer.dispose();
    }

    public boolean closeIfRequested() {
        if (window.closeIfRequested()) {
            screenContainer.dispose();
            return true;
        }
        return false;
    }


    @Override
    public void lockGuiScale(int scale) {
        window.lockGuiScale(scale);
    }

    @Override
    public void unlockGuiScale() {
        window.unlockGuiScale();
    }

    @Override
    public OptionalInt getLockedGuiScale() {
        return window.getLockedGuiScale();
    }

    @Override
    public boolean canSetGuiScale() {
        return true;
    }

    @Override
    public int getRecommendedGuiScale() {
        return window.window.calculateScale(-1, Minecraft.getInstance().isEnforceUnicode());
    }

    @Override
    public boolean allowsCustomTitleBar() {
        return false;
    }

    @Override
    public void setShowTitleBar(boolean show) {

    }

    @Override
    public WindowContentPopOutHost getContentPopOutHost() {
        return host;
    }

    @Override
    public boolean isCompletelyOverlaying() {
        int[] myX = {0}, myY = {0}, myW = {0}, myH = {0}, otX = {0}, otY = {0}, otW = {0}, otH = {0};

        GLFW.glfwGetWindowPos(window.window.getWindow(), myX, myY);
        GLFW.glfwGetWindowPos(mainWindow.getWindow(), otX, otY);
        GLFW.glfwGetWindowSize(window.window.getWindow(), myW, myH);
        GLFW.glfwGetWindowSize(mainWindow.getWindow(), otW, otH);

        double error = distance(myX[0], myY[0], otX[0], otY[0]) + distance(myW[0], myH[0], otW[0], otH[0]);

        return error < 10;
    }

    private double distance(
            double x1,
            double y1,
            double x2,
            double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }
}
