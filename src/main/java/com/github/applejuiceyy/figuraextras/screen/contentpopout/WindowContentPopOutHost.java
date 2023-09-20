package com.github.applejuiceyy.figuraextras.screen.contentpopout;

import com.github.applejuiceyy.figuraextras.ducks.MinecraftAccess;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Vector2d;

import java.util.ArrayList;

public class WindowContentPopOutHost implements ContentPopOutHost {
    final Window window;

    ArrayList<Instance> instances = new ArrayList<>();
    Font font = Minecraft.getInstance().font;
    Instance dragging = null;

    public WindowContentPopOutHost(Window window) {
        this.window = window;
    }


    public void add(Observers.Observer<Component> component) {
        instances.add(new Instance(component, component.observe(ignored -> {
        }), new Vector2d(window.getGuiScaledWidth() / 2f, window.getGuiScaledHeight() / 2f)));
    }

    public void render(GuiGraphics graphics, double mouseX, double mouseY, float delta) {
        GlStateManager._clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        for (Instance instance : instances) {
            Component component = instance.component.get();

            graphics.drawString(font, component, (int) instance.position.x, (int) instance.position.y, 0xffffffff);
        }

        if (dragging != null) {
            graphics.drawString(font, Component.literal("Drop here to transfer to monitor"), 12, 1, 0xffffffff);
            graphics.fill(0, 0, 10, 10, 0xffff0000);
        }
        if (((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$getMonitorPopUpHost().canReinsert()) {
            graphics.fill(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), 0x55000000);
            Component component = Component.literal("Click here to transfer to screen");
            graphics.drawString(font, component,
                    window.getGuiScaledWidth() / 2 - font.width(component) / 2,
                    window.getGuiScaledHeight() / 2 - font.wordWrapHeight(component, 100) / 2,
                    0xffffffff
            );
        }
    }

    public boolean onMouseDown(double mouseX, double mouseY, int buttons) {
        if (((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$getMonitorPopUpHost().canReinsert()) {
            ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$getMonitorPopUpHost().reinsert(this::add);
            return true;
        }
        for (Instance instance : instances) {
            Component component = instance.component.get();
            int width = font.width(component);
            int height = font.wordWrapHeight(component, 100);

            if (instance.position.x < mouseX && instance.position.x + width > mouseX &&
                    instance.position.y < mouseY && instance.position.y + height > mouseY) {
                dragging = instance;
                return true;
            }
        }
        return false;
    }

    public boolean onMouseDrag(double mouseX, double mouseY, int buttons, double dx, double dy) {
        if (dragging != null) {
            dragging.position.add(dx, dy);
            return true;
        }
        return false;
    }

    public boolean onMouseRelease(double x, double y, int button) {
        if (dragging != null) {
            if (x > 0 && x < 10 && y > 0 && y < 10) {
                ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$getMonitorPopUpHost().add(dragging.component);
                dragging.sub.stop();
                instances.remove(dragging);
            }
            dragging = null;
            return true;
        }
        return false;
    }

    record Instance(Observers.Observer<Component> component, Observers.UnSubscriber sub, Vector2d position) {
    }
}
