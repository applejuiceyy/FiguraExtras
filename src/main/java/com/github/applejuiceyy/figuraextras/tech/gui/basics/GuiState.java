package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.tech.gui.stack.Stacks;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class GuiState implements Renderable, GuiEventListener, LayoutElement, NarratableEntry {

    public final ElementOrder elementOrder = new ElementOrder();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonEvent>> mouseDown = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonEvent>> mouseUp = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionEvent>> mouseMove = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonDeltaEvent>> mouseDragged = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionAmountEvent>> mouseScrolled = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.KeyEvent>> keyPressed = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.KeyEvent>> keyReleased = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.CharEvent>> charTyped = Event.consumer();
    private final List<ParentElement<?>> reprocessDirty = new ArrayList<>();
    private final Element root;
    private final List<Runnable> afterReflow = new ArrayList<>();
    private final List<Runnable> afterPriority = new ArrayList<>();
    private Element focused;
    private boolean thisFocused;
    private boolean priorityDirty = false;
    private boolean clipDirty = false;
    private List<Element> currentHoverStack = List.of();
    private List<Element> mouseDownStack = List.of();

    public GuiState(Element root) {
        this.root = root;
    }

    public Element getRoot() {
        return root;
    }

    @Nullable
    public Element getFocused() {
        return focused;
    }

    public void work() {
        do {
            do {
                reflow();
                List<Runnable> copy = new ArrayList<>(afterReflow);
                afterReflow.clear();
                copy.forEach(Runnable::run);
            } while (!afterReflow.isEmpty());

            if (priorityDirty) {
                elementOrder.update(getRoot());
                priorityDirty = false;
            }

            List<Runnable> copy = new ArrayList<>(afterPriority);
            afterPriority.clear();
            copy.forEach(Runnable::run);
        } while (priorityDirty || !afterPriority.isEmpty());

        if (clipDirty) {
            updateClippingBoxes(root, null);
            clipDirty = false;
        }
    }

    // region flow

    private void reflow() {
        reprocessDirty.sort(Comparator.comparing(Element::getDepth));
        while (!reprocessDirty.isEmpty()) {
            reprocessDirty.get(0).positionElements();
        }
    }

    private void updateClippingBoxes(Element element, @Nullable Rectangle currentBox) {
        if (element.shouldClip()) {
            currentBox = currentBox == null ? element : currentBox.intersection(element);
            element.clippingBox = currentBox;
        }

        if (element instanceof ParentElement<?> parentElement) {
            for (Element child : parentElement.getElements()) {
                updateClippingBoxes(child, currentBox);
            }
        }
    }

    public void addChildrenReprocessingTask(ParentElement<?> element) {
        if (!reprocessDirty.contains(element))
            reprocessDirty.add(element);
    }

    public void removeChildrenReprocessingTask(ParentElement<?> element) {
        reprocessDirty.remove(element);
    }

    public void afterReflow(Runnable runnable) {
        afterReflow.add(runnable);
    }

    public void afterPriority(Runnable runnable) {
        afterPriority.add(runnable);
    }

    public void markPriorityDirty() {
        priorityDirty = true;
    }

    public void markClipDirty() {
        clipDirty = true;
    }

    // endregion
    // region rendering
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        work();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(getX(), getY(), 0);
        renderElements(elementOrder.tree, graphics, mouseX, mouseY, delta);
        /*
        for (Element element : elementOrder) {
            if(element.intersects(mouseX, mouseY)
                    && (element.clippingBox == null || element.clippingBox.intersects(mouseX, mouseY))) {
                element.renderSelfDebug(graphics, mouseX, mouseY, delta);
                if(element instanceof ParentElement<?> parentElement) {
                    pose.translate(-parentElement.xView.get(), -parentElement.yView.get(), 0);
                    parentElement.renderLayoutDebug(graphics, mouseX, mouseY, delta);
                }

                break;
            }
        }*/
        pose.popPose();
    }

    private void renderElements(List<ElementOrder.Node> tree, GuiGraphics graphics, int mouseX, int mouseY, float delta) {

        for (int i = tree.size() - 1; i >= 0; i--) {
            ElementOrder.Node node = tree.get(i);
            Element bounding;

            Optional<ElementOrder.Inner> left = node.node().left();
            Optional<Element> right = node.node().right();

            if (right.isPresent()) {
                bounding = right.get();
                setClippingBox(bounding.clippingBox);

                RenderTarget self = null;
                if (bounding.getSurface().usesSelfRender()) {
                    Stacks.RENDER_TARGETS.push();
                    bounding.render(graphics, mouseX, mouseY, delta);
                    self = Stacks.RENDER_TARGETS.pop(false);
                }

                bounding.getSurface().render(bounding, graphics, mouseX, mouseY, delta, null, self);
                graphics.pose().translate(0, 0, 0.01);

                if (bounding.getSurface().usesSelfRender()) {
                    Stacks.RENDER_TARGETS.reclaim(self);
                } else {
                    bounding.render(graphics, mouseX, mouseY, delta);
                    graphics.pose().translate(0, 0, 0.01);
                }
            } else if (left.isPresent()) {
                ElementOrder.Inner inner = left.get();
                bounding = inner.owner();
                setClippingBox(bounding.clippingBox);
                RenderTarget children = null;
                RenderTarget self = null;
                if (bounding.getSurface().usesChildren()) {
                    Stacks.RENDER_TARGETS.push();
                    renderElements(inner.children(), graphics, mouseX, mouseY, delta);
                    children = Stacks.RENDER_TARGETS.pop(false);
                }

                if (bounding.getSurface().usesSelfRender()) {
                    Stacks.RENDER_TARGETS.push();
                    bounding.render(graphics, mouseX, mouseY, delta);
                    self = Stacks.RENDER_TARGETS.pop(false);
                }

                bounding.getSurface().render(bounding, graphics, mouseX, mouseY, delta, children, self);

                if (bounding.getSurface().usesChildren()) {
                    Stacks.RENDER_TARGETS.reclaim(children);
                } else {
                    renderElements(inner.children(), graphics, mouseX, mouseY, delta);
                    graphics.pose().translate(0, 0, 0.01);
                }

                if (bounding.getSurface().usesSelfRender()) {
                    Stacks.RENDER_TARGETS.reclaim(self);
                } else {
                    bounding.render(graphics, mouseX, mouseY, delta);
                    graphics.pose().translate(0, 0, 0.01);
                }
            } else {
                throw new RuntimeException();
            }
        }
    }

    private void setClippingBox(@Nullable Rectangle rect) {
        if (rect != null) {
            Window window = Minecraft.getInstance().getWindow();
            int i = window.getHeight();
            double d = window.getGuiScale();
            double e = (double) rect.getX() * d;
            double f = (double) i - (double) (rect.getY() + rect.getHeight()) * d;
            double g = (double) rect.getWidth() * d;
            double h = (double) rect.getHeight() * d;
            RenderSystem.enableScissor((int) e, (int) f, Math.max(0, (int) g), Math.max(0, (int) h));
        } else {
            RenderSystem.disableScissor();
        }
    }

    private <V extends DefaultCancellableEvent> void doSweepEvent(Iterable<Element> elements, Function<Element, Event<Consumer<V>>> eventFetcher, Event<Consumer<V>> rootEvent, BiFunction<Element, V, Boolean> defaultBehaviour, V event) {
        rootEvent.getSink().run(e -> e.accept(event));

        if (!event.isPropagating()) {
            return;
        }

        for (Element element : elements) {
            Event<Consumer<V>> apply = eventFetcher.apply(element);
            apply.getSink().run(runners -> runners.accept(event));
            if (!event.isPropagating()) {
                break;
            }
        }
        if (!event.cancellingDefault()) {
            for (Element element : elements) {
                if (defaultBehaviour.apply(element, event)) {
                    return;
                }
            }
        }
    }

    // endregion
    // region events

    private <T extends DefaultCancellableEvent.MousePositionEvent> @Nullable List<Element> defaultMouseOverEvent(double mouseX, double mouseY, Function<Element, Event<Consumer<T>>> eventGetter, Event<Consumer<T>> rootEvent, BiFunction<Element, T, Boolean> defaultBehaviour, T event) {
        for (Element element : elementOrder) {
            if (element.intersects(mouseX, mouseY)
                    && (element.clippingBox == null || element.clippingBox.intersects(mouseX, mouseY))) {
                List<Element> path = new ArrayList<>();
                root.collectPath(element, path::add);
                doSweepEvent(path, eventGetter, rootEvent, defaultBehaviour, event);
                return path;
            }
        }
        return null;
    }

    private void doHovering(List<Element> path, double mouseX, double mouseY) {
        path = path == null ? List.of() : Lists.reverse(path);

        int breakoffPoint = 0;
        for (; breakoffPoint < Math.min(currentHoverStack.size(), path.size()); breakoffPoint++) {
            if (path.get(breakoffPoint) != currentHoverStack.get(breakoffPoint)) {
                break;
            }
        }

        for (int i1 = currentHoverStack.size() - 1; i1 >= breakoffPoint; i1--) {
            currentHoverStack.get(i1).hovering.set(false);
            currentHoverStack.get(i1).hoveringWithin.set(false);
            currentHoverStack.get(i1).activeHovering.set(false);
        }

        for (int i1 = breakoffPoint; i1 < path.size(); i1++) {
            path.get(i1).hovering.set(true);
        }
        int activeHovering = path.size() - 1;
        for (; activeHovering >= 0; activeHovering--) {
            Element element = path.get(activeHovering);
            element.hoveringWithin.set(false);
            if (element.hitTest(mouseX, mouseY)) {
                element.activeHovering.set(true);
                activeHovering--;
                break;
            } else {
                element.activeHovering.set(false);
            }
        }
        for (; activeHovering >= 0; activeHovering--) {
            path.get(activeHovering).hoveringWithin.set(true);
            path.get(activeHovering).activeHovering.set(false);
        }

        currentHoverStack = path;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        List<Element> path = defaultMouseOverEvent(mouseX, mouseY, e -> e.mouseMove, mouseMove, Element::defaultMouseMoveBehaviour,
                new DefaultCancellableEvent.MousePositionEvent(mouseX, mouseY)
        );

        if (mouseDownStack == null) {
            doHovering(path, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseDownStack = defaultMouseOverEvent(mouseX, mouseY, e -> e.mouseDown, mouseDown, Element::defaultMouseDownBehaviour, new DefaultCancellableEvent.MousePositionButtonEvent(mouseX, mouseY, button));
        if (mouseDownStack == null) {
            return false;
        }
        for (Element element : mouseDownStack) {
            element.active.set(true);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseDownStack != null) {
            doSweepEvent(mouseDownStack, e -> e.mouseUp, mouseUp,
                    Element::defaultMouseUpBehaviour,
                    new DefaultCancellableEvent.MousePositionButtonEvent(mouseX, mouseY, button));
            for (Element element : mouseDownStack) {
                element.active.set(false);
            }

            List<Element> path = new ArrayList<>();
            for (Element element : elementOrder) {
                if (element.intersects(mouseX, mouseY)
                        && (element.clippingBox == null || element.clippingBox.intersects(mouseX, mouseY))) {
                    root.collectPath(element, path::add);
                    break;
                }
            }
            doHovering(path, mouseX, mouseY);
            mouseDownStack = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseDownStack != null) {
            doSweepEvent(mouseDownStack, e -> e.mouseDragged, mouseDragged,
                    Element::defaultMouseDraggedBehaviour,
                    new DefaultCancellableEvent.MousePositionButtonDeltaEvent(mouseX, mouseY, button, deltaX, deltaY));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return defaultMouseOverEvent(mouseX, mouseY, e -> e.mouseScrolled, mouseScrolled, Element::defaultMouseScrolledBehaviour,
                new DefaultCancellableEvent.MousePositionAmountEvent(mouseX, mouseY, amount)
        ) != null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFocused() == null) return false;
        List<Element> path = new ArrayList<>();
        root.collectPath(getFocused(), path::add);
        doSweepEvent(path, e -> e.keyPressed, keyPressed,
                Element::defaultKeyPressedBehaviour,
                new DefaultCancellableEvent.KeyEvent(keyCode, scanCode, modifiers));
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (getFocused() == null) return false;
        List<Element> path = new ArrayList<>();
        root.collectPath(getFocused(), path::add);
        doSweepEvent(path, e -> e.keyReleased, keyReleased,
                Element::defaultKeyReleasedBehaviour,
                new DefaultCancellableEvent.KeyEvent(keyCode, scanCode, modifiers));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (getFocused() == null) return false;
        List<Element> path = new ArrayList<>();
        root.collectPath(getFocused(), path::add);
        doSweepEvent(path, e -> e.charTyped, charTyped,
                Element::defaultCharTypedBehaviour,
                new DefaultCancellableEvent.CharEvent(chr, modifiers));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return root.intersects(mouseX, mouseY);
    }

    // endregion

    @Override
    public boolean isFocused() {
        return thisFocused;
    }

    public void setFocused(Element element) {
        if (focused != null) {
            focused.focused.set(false);
            focused = null;
        }
        if (element != null) {
            element.focused.set(true);
            focused = element;
        }
    }

    @Override
    public void setFocused(boolean focused) {
        this.thisFocused = focused;
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public int getX() {
        return root.getX();
    }

    @Override
    public void setX(int x) {
        root.setX(x);
    }

    @Override
    public int getY() {
        return root.getY();
    }

    @Override
    public void setY(int y) {
        root.setY(y);
    }

    @Override
    public int getWidth() {
        return root.getWidth();
    }

    public void setWidth(int width) {
        root.setWidth(width);
    }

    @Override
    public int getHeight() {
        return root.getHeight();
    }

    public void setHeight(int height) {
        root.setHeight(height);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> widget) {
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
    }
}
