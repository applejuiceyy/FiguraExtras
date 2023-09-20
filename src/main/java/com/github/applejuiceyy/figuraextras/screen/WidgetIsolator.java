package com.github.applejuiceyy.figuraextras.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Tuple;
import org.joml.Matrix4f;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class WidgetIsolator {
    private final ParentComponent component;
    ArrayList<Tuple<Vector2d, Component>> isolatedWidgets = new ArrayList<>();
    Tuple<Vector2d, Component> dragging = null;
    boolean isIsolating = false;

    public WidgetIsolator(ParentComponent component) {
        this.component = component;
    }

    public void addWidget(Component widget) {
        isolatedWidgets.add(new Tuple<>(new Vector2d(0, 0), widget));
    }

    public void startIsolating() {
        isIsolating = true;
    }

    private boolean isIn(Tuple<Vector2d, Component> thing, double mouseX, double mouseY) {
        return thing.getB().isInBoundingBox(mouseX + thing.getB().x() - thing.getA().x, mouseY + thing.getB().y() - thing.getA().y);
    }

    public boolean hitTest(double mouseX, double mouseY) {
        for (Tuple<Vector2d, Component> thing : isolatedWidgets) {
            if (isIn(thing, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        for (Iterator<Tuple<Vector2d, Component>> iterator = isolatedWidgets.iterator(); iterator.hasNext(); ) {
            Tuple<Vector2d, Component> thing = iterator.next();
            if (isIn(thing, mouseX, mouseY)) {
                if ((thing.getB().width() - (mouseX - thing.getA().x)) + (mouseY - thing.getA().y) < 10) {
                    iterator.remove();
                    continue;
                }
                dragging = thing;
                return true;
            }
        }

        if (!isIsolating) return false;
        Component component = this.component.childAt((int) mouseX, (int) mouseY);
        if (component == null) {
            return false;
        }
        addWidget(component);
        isIsolating = false;
        return true;
    }

    public void render(OwoUIDrawContext context, double mouseX, double mouseY, float delta) {
        PoseStack stack = context.pose();

        for (Tuple<Vector2d, Component> thing : isolatedWidgets) {
            Component isolatedWidget = thing.getB();
            Vector2d position = thing.getA();
            stack.pushPose();
            stack.translate(-isolatedWidget.x() + position.x, -isolatedWidget.y() + position.y, 0);
            isolatedWidget.draw(context, -999, -999, delta, Minecraft.getInstance().getDeltaFrameTime());

            stack.popPose();
            Matrix4f pose = stack.last().pose();

            if (isIn(thing, mouseX, mouseY)) {
                RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

                bufferBuilder.vertex(pose, (float) (position.x + isolatedWidget.width()), (float) position.y, 0).color(0xffaa0000).endVertex();
                bufferBuilder.vertex(pose, (float) (position.x + isolatedWidget.width() - 10), (float) position.y, 0).color(0xffaa0000).endVertex();
                bufferBuilder.vertex(pose, (float) (position.x + isolatedWidget.width()), (float) (position.y + 10), 0).color(0xffaa0000).endVertex();

                BufferUploader.drawWithShader(bufferBuilder.end());
            }
        }

        if (hitTest(mouseX, mouseY)) {
            setStyle(CursorStyle.MOVE);
        }

        if (!isIsolating) {
            return;
        }
        Component component = this.component.childAt((int) mouseX, (int) mouseY);
        if (component == null) {
            return;
        }
        context.drawInsets(component.x(), component.y(), component.width(), component.height(), Insets.of(5), 0xff3366ff);
    }

    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return false;
    }

    public boolean onMouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        if (dragging != null) {
            dragging.getA().add(dX, dY);
            setStyle(CursorStyle.MOVE);
            return true;
        }
        return false;
    }

    abstract void setStyle(CursorStyle style);
}
