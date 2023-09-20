package com.github.applejuiceyy.figuraextras.window;

import com.github.applejuiceyy.figuraextras.ducks.MinecraftAccess;
import com.github.applejuiceyy.figuraextras.ducks.VirtualScreenAccess;
import com.github.applejuiceyy.figuraextras.ducks.WindowAccess;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.VirtualScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

public class SecondaryWindow implements WindowEventHandler {
    public final Window window;
    public final ContextAgnosticMouseHandler mouseHandler;

    public final RenderTarget renderTarget;
    private final Callback callback;

    private final int ownFramebuffer;

    private TextureTarget owoLibCompatBlurProgramTexture;
    private OptionalInt lockedGuiScale = OptionalInt.empty();


    public SecondaryWindow(Function<VirtualScreen, Window> creator, Callback callback) {
        this.callback = callback;
        Minecraft minecraft = Minecraft.getInstance();

        VirtualScreen screen = ((MinecraftAccess) minecraft).figuraExtrass$getScreenManager();

        ((VirtualScreenAccess) (Object) screen).figuraExtrass$setEventListener(this);
        window = creator.apply(screen);
        recalculateGuiScale(false);
        ((WindowAccess) (Object) window).figuraExtrass$setShouldTerminateGLFWOnExit(false);
        GLFW.glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());
        ((VirtualScreenAccess) (Object) screen).figuraExtrass$setEventListener(null);


        InputConstants.setupKeyboardCallbacks(
                window.getWindow(), (windowHandle, key, scancode, action, modifiers) -> minecraft.execute(() -> {
                    if (action == 0) {
                        callback.keyReleased(key, scancode, modifiers);
                    } else {
                        callback.keyPressed(key, scancode, modifiers);
                    }
                }), (windowHandle, codePoint, modifiers) -> minecraft.execute(() -> {
                    if (Character.charCount(codePoint) == 1) {
                        callback.charTyped((char) codePoint, modifiers);
                    } else {
                        char[] var6 = Character.toChars(codePoint);
                        for (char c : var6) {
                            callback.charTyped(c, modifiers);
                        }
                    }
                }));

        mouseHandler = new ContextAgnosticMouseHandler(window, new ContextAgnosticMouseHandler.Callback() {
            @Override
            public void onCLick(double d, double e, int button) {
                callback.mouseClicked(d, e, button);
            }

            @Override
            public void onRelease(double d, double e, int button) {
                callback.mouseReleased(d, e, button);
            }

            @Override
            public void onScroll(double e, double f, double d) {
                callback.mouseScrolled(e, f, d);
            }

            @Override
            public void filesDropped(List<Path> paths) {
                callback.filesDropped(paths);
            }

            @Override
            public void mouseMoved(double d, double e) {
                callback.mouseMoved(d, e);
            }

            @Override
            public void mouseDragged(double d, double e, int activeButton, double f, double g) {
                callback.mouseDragged(d, e, activeButton, f, g);
            }
        });
        mouseHandler.setup();
        renderTarget = new RenderTarget(true) {
        };
        renderTarget.createBuffers(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
        renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        renderTarget.clear(Minecraft.ON_OSX);


        GLFW.glfwMakeContextCurrent(window.getWindow());
        ownFramebuffer = GlStateManager.glGenFramebuffers();
        bindFramebufferTexture();
        GLFW.glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());

        createOwoLibBlurShaderCompatibility();
    }

    private void unbindFramebufferTexture() {
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, ownFramebuffer);
        GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, GlConst.GL_TEXTURE_2D, 0, 0);
        GL20.glDeleteTextures(renderTarget.getColorTextureId());
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
    }

    private void bindFramebufferTexture() {
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, ownFramebuffer);
        GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, GlConst.GL_TEXTURE_2D, renderTarget.getColorTextureId(), 0);

        int i = GlStateManager.glCheckFramebufferStatus(GlConst.GL_FRAMEBUFFER);
        if (i != 36053) {
            if (i == 36054) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == 36055) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == 36059) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == 36060) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else if (i == 36061) {
                throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
            } else if (i == 1285) {
                throw new RuntimeException("GL_OUT_OF_MEMORY");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }

        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
    }

    public void updateDisplay() {
        GLFW.glfwMakeContextCurrent(window.getWindow());

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, ownFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBlitFramebuffer(0, 0, window.getWidth(), window.getHeight(), 0, 0, window.getWidth(), window.getHeight(),
                GlConst.GL_COLOR_BUFFER_BIT, GlConst.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);

        GLFW.glfwSwapBuffers(window.getWindow());

        GLFW.glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());
    }

    @Override
    public void setWindowActive(boolean focused) {
        callback.windowActive(focused);
    }

    @Override
    public void resizeDisplay() {
        GLFW.glfwMakeContextCurrent(window.getWindow());
        unbindFramebufferTexture();
        GLFW.glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());

        recalculateGuiScale(true);


        renderTarget.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
        if (owoLibCompatBlurProgramTexture != null) {
            owoLibCompatBlurProgramTexture.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
        }


        GLFW.glfwMakeContextCurrent(window.getWindow());
        bindFramebufferTexture();
        GLFW.glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());
    }

    public void recalculateGuiScale(boolean doCallback) {
        int i = lockedGuiScale.isEmpty() ? this.window.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()) : lockedGuiScale.getAsInt();
        this.window.setGuiScale(i);
        if (doCallback) {
            callback.resizeDisplay(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
        }
    }

    public void lockGuiScale(int step) {
        this.lockedGuiScale = OptionalInt.of(step);
        recalculateGuiScale(true);
    }

    public void unlockGuiScale() {
        this.lockedGuiScale = OptionalInt.empty();
        recalculateGuiScale(true);
    }

    public OptionalInt getLockedGuiScale() {
        return lockedGuiScale;
    }

    public boolean closeIfRequested() {
        if (GLFW.glfwWindowShouldClose(window.getWindow())) {
            close();
            return true;
        }
        return false;
    }

    @Override
    public void cursorEntered() {
        mouseHandler.cursorEntered();
        callback.cursorEntered();
    }

    public void close() {
        callback.destroy();
        unbindFramebufferTexture();
        GLFW.glfwMakeContextCurrent(window.getWindow());
        GlStateManager._glDeleteFramebuffers(ownFramebuffer);
        GLFW.glfwMakeContextCurrent(Minecraft.getInstance().getWindow().getWindow());
        if (owoLibCompatBlurProgramTexture != null) {
            owoLibCompatBlurProgramTexture.destroyBuffers();
        }
        window.close();
    }

    public void createOwoLibBlurShaderCompatibility() {
        ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$withWindow(window, renderTarget, () -> {
            owoLibCompatBlurProgramTexture = new TextureTarget(window.getWidth(), window.getHeight(), false, Minecraft.ON_OSX);
        });
    }

    public TextureTarget getOwoLibBlurShaderCompatibilityTexture() {
        return owoLibCompatBlurProgramTexture;
    }

    public void setupTransforms() {
        Util.setupTransforms(window);
    }

    public void endTransforms() {
        Util.endTransforms();
    }

    public interface Callback {

        default void keyReleased(int key, int scancode, int modifiers) {
        }

        default void keyPressed(int key, int scancode, int modifiers) {
        }

        default void charTyped(char codePoint, int modifiers) {
        }

        default void mouseClicked(double x, double y, int button) {
        }

        default void mouseReleased(double x, double y, int button) {
        }

        default void mouseScrolled(double x, double y, double d) {
        }

        default void filesDropped(List<Path> paths) {
        }

        default void mouseMoved(double x, double y) {
        }

        default void mouseDragged(double x, double y, int activeButton, double mx, double my) {
        }

        default void resizeDisplay(int guiScaledWidth, int guiScaledHeight) {
        }

        default void cursorEntered() {
        }

        default void windowActive(boolean focused) {
        }

        default void destroy() {
        }
    }
}
