package com.github.applejuiceyy.figuraextras.screen.contentpopout;

import com.github.applejuiceyy.figuraextras.ducks.statics.WindowDuck;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.window.SecondaryWindow;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.WindowProcI;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class MonitorContentPopOutHost implements ContentPopOutHost {
    ArrayList<Instance> window = new ArrayList<>();
    long mainWindowHandle;
    Instance potentialReinserting = null;

    public MonitorContentPopOutHost(Window window) {
        mainWindowHandle = window.getWindow();
    }

    public boolean canReinsert() {
        return potentialReinserting != null;
    }

    public void reinsert(Consumer<Observers.Observer<Component>> consumer) {
        if (potentialReinserting == null) {
            return;
        }

        consumer.accept(potentialReinserting.component);
        potentialReinserting.secondaryWindow.close();
        potentialReinserting.sub.stop();
        potentialReinserting.glfwCursorEnterCallback.close();
        window.remove(potentialReinserting);
        potentialReinserting = null;
    }

    @Override
    public void add(Observers.Observer<Component> component) {
        Minecraft.getInstance().tell(() -> new Instance(component));
    }

    public void update() {
        Font font = Minecraft.getInstance().font;

        for (Instance bWindow : window) {
            Component component = bWindow.component.get();
            SecondaryWindow window = bWindow.secondaryWindow;

            int expectedWidth = font.width(component);
            int expectedHeight = font.wordWrapHeight(component, 999);

            int targetWidth = (int) Math.ceil(expectedWidth * window.window.getGuiScale());
            int targetHeight = (int) Math.ceil(expectedHeight * window.window.getGuiScale());

            int[] sw = {0}, sh = {0};

            GLFW.glfwGetWindowSize(window.window.getWindow(), sw, sh);

            if (sw[0] != targetWidth || sh[0] != targetHeight) {
                GLFW.glfwSetWindowSize(window.window.getWindow(), targetWidth, targetHeight);

                window.renderTarget.clear(Minecraft.ON_OSX);
                window.updateDisplay();
            }

            window.renderTarget.clear(Minecraft.ON_OSX);
            window.renderTarget.bindWrite(true);

            window.setupTransforms();
            GuiGraphics guiGraphics = new GuiGraphics(
                    Minecraft.getInstance(),
                    Minecraft.getInstance().renderBuffers().bufferSource()
            );

            guiGraphics.drawString(font, bWindow.component.get(), 0, 0, 0xffffff);

            if (bWindow.showCorners) {
                RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

                RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

                bufferBuilder.vertex(0, 0, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(0, 5, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(5, 0, 0).color(0xff00ff00).endVertex();

                bufferBuilder.vertex(0, expectedHeight - 5, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(0, expectedHeight, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(5, expectedHeight, 0).color(0xff00ff00).endVertex();

                bufferBuilder.vertex(expectedWidth, 0, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(expectedWidth - 5, 0, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(expectedWidth, 5, 0).color(0xffff0000).endVertex();


                bufferBuilder.vertex(expectedWidth, expectedHeight - 5, 0).color(0xffffff00).endVertex();
                bufferBuilder.vertex(expectedWidth - 5, expectedHeight, 0).color(0xffffff00).endVertex();
                bufferBuilder.vertex(expectedWidth, expectedHeight, 0).color(0xffffff00).endVertex();

                BufferUploader.drawWithShader(bufferBuilder.end());
            }

            guiGraphics.flush();

            window.endTransforms();

            window.renderTarget.unbindWrite();
            window.updateDisplay();
        }
    }

    class Instance {
        GLFWCursorEnterCallback glfwCursorEnterCallback;
        SecondaryWindow secondaryWindow;
        Observers.Observer<Component> component;
        Observers.UnSubscriber sub;

        boolean showCorners = false;

        Instance(Observers.Observer<Component> component) {
            this.component = component;
            // dummy observe so it keeps on giving
            sub = component.observe(ignored -> {
            });

            secondaryWindow = new SecondaryWindow(screen -> {
                WindowDuck.hints = () -> {
                    GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
                    if (Util.getPlatform() == Util.OS.WINDOWS) {
                        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                        GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
                    }
                };

                Window window = screen.newWindow(new DisplayData(300, 200, OptionalInt.empty(), OptionalInt.empty(), false), null, "e");
                WindowDuck.hints = null;


                if (Util.getPlatform() == Util.OS.WINDOWS) {
                    long nativeHwnd = GLFWNativeWin32.glfwGetWin32Window(window.getWindow());

                    long mainNative = GLFWNativeWin32.glfwGetWin32Window(mainWindowHandle);
                    User32.SetWindowLongPtr(nativeHwnd, User32.GWL_HWNDPARENT, mainNative);

                    long original = User32.GetWindowLongPtr(nativeHwnd, User32.GWL_WNDPROC);

                    WindowProcI proc = (hwnd, message, wparam, lparam) -> {
                        long result = User32.nCallWindowProc(original, hwnd, message, wparam, lparam);
                        if (hwnd == nativeHwnd && result == User32.HTCLIENT && message == User32.WM_NCHITTEST) {
                            double[] xn = {0}, yn = {0};

                            GLFW.glfwGetCursorPos(window.getWindow(), xn, yn);

                            double x = xn[0];
                            double y = yn[0];

                            double scale = secondaryWindow.window.getGuiScale();

                            if (x + y < 5 * scale) {
                                return User32.HTCLIENT;
                            }
                            Component live = component.get();
                            Font font = Minecraft.getInstance().font;

                            double width = font.width(live) * scale;

                            if ((width - x) + y < 5 * scale) {
                                return User32.HTCLIENT;
                            }

                            double height = font.wordWrapHeight(live, 999) * scale;
                            if (x + (height - y) < 5 * scale) {
                                return User32.HTCLIENT;
                            }

                            if ((width - x) + (height - y) < 5 * scale) {
                                return User32.HTCLIENT;
                            }

                            return User32.HTCAPTION;
                        }
                        return result;
                    };

                    User32.SetWindowLongPtr(nativeHwnd, User32.GWL_WNDPROC, proc.address());
                }
                return window;
            }, new SecondaryWindow.Callback() {

                @Override
                public void mouseClicked(double x, double y, int button) {
                    OptionalInt i = secondaryWindow.getLockedGuiScale();
                    if (x + y < 5) {
                        if (i.isPresent()) {
                            secondaryWindow.lockGuiScale(i.getAsInt() + 1);
                        }
                    }

                    Component live = component.get();
                    Font font = Minecraft.getInstance().font;
                    int width = font.width(live);

                    if ((width - x) + y < 5) {
                        Minecraft.getInstance().tell(() -> {
                            secondaryWindow.close();
                            sub.stop();
                            glfwCursorEnterCallback.close();
                            window.remove(Instance.this);
                        });
                        return;
                    }

                    int height = font.wordWrapHeight(live, 999);

                    if (x + (height - y) < 5) {
                        if (i.isPresent()) {
                            secondaryWindow.lockGuiScale(i.getAsInt() == 5 ? 5 : i.getAsInt() - 1);
                        }
                    }

                    if ((width - x) + (height - y) < 5) {
                        potentialReinserting = potentialReinserting == Instance.this ? null : Instance.this;
                    }
                }
            });

            glfwCursorEnterCallback = GLFW.glfwSetCursorEnterCallback(secondaryWindow.window.getWindow(), (w, entered) -> {
                if (secondaryWindow.window.getWindow() == w) {
                    showCorners = entered;
                }
            });

            secondaryWindow.lockGuiScale(5);
            window.add(this);
        }
    }
}
