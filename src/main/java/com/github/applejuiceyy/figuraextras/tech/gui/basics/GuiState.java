package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.util.Event;
import com.google.common.collect.Lists;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

                Runnable self = () -> bounding.render(graphics, mouseX, mouseY, delta);

                bounding.getSurface().render(bounding, graphics, mouseX, mouseY, delta, null, self);
                graphics.pose().translate(0, 0, 0.01);

                if (!bounding.getSurface().usesSelfRender()) {
                    self.run();
                    graphics.pose().translate(0, 0, 0.01);
                }
            } else if (left.isPresent()) {
                ElementOrder.Inner inner = left.get();
                bounding = inner.owner();
                setClippingBox(bounding.clippingBox);
                Runnable children = () -> renderElements(inner.children(), graphics, mouseX, mouseY, delta);
                Runnable self = () -> bounding.render(graphics, mouseX, mouseY, delta);

                bounding.getSurface().render(bounding, graphics, mouseX, mouseY, delta, children, self);

                if (!bounding.getSurface().usesSelfRender()) {
                    self.run();
                    graphics.pose().translate(0, 0, 0.01);
                }

                if (!bounding.getSurface().usesChildren()) {
                    children.run();
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

    private <V extends DefaultCancellableEvent> void doSweepEvent(Iterable<Element> elements, Function<Element, Event<Consumer<V>>> eventFetcher, @Nullable Event<Consumer<V>> rootEvent, BiConsumer<Element, V> defaultBehaviour, Predicate<Element> terminator, V event) {
        if (rootEvent != null)
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
                defaultBehaviour.accept(element, event);
                if (terminator.test(element)) {
                    return;
                }
            }
        }
    }

    public <T extends DefaultCancellableEvent> List<Element> fire(Element element, Function<Element, Event<Consumer<T>>> eventGetter, Event<Consumer<T>> rootEvent, BiConsumer<Element, T> defaultBehaviour, Predicate<Element> terminator, T event) {
        List<Element> path = new ArrayList<>();
        root.collectPath(element, path::add);
        doSweepEvent(path, eventGetter, rootEvent, defaultBehaviour, terminator, event);
        return path;
    }

    // endregion
    // region events

    private Element findHovered(double mouseX, double mouseY) {
        for (Element element : elementOrder) {
            if (element.intersects(mouseX, mouseY)
                    && (element.clippingBox == null || element.clippingBox.intersects(mouseX, mouseY))
                    && element.hoveringMouseHitTest(mouseX, mouseY)) {
                return element;
            }
        }
        return null;
    }

    private <T extends DefaultCancellableEvent.MousePositionEvent> @Nullable List<Element> defaultMouseOverEvent(double mouseX, double mouseY, Function<Element, Event<Consumer<T>>> eventGetter, Event<Consumer<T>> rootEvent, BiConsumer<Element, T> defaultBehaviour, Predicate<Element> terminator, T event) {
        Element hovered = findHovered(mouseX, mouseY);
        if (hovered != null) {
            return fire(hovered, eventGetter, rootEvent, defaultBehaviour, terminator, event);
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
            if (element.blocksMouseActivation()) {
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
                Element::blocksMouseActivation,
                new DefaultCancellableEvent.MousePositionEvent(mouseX, mouseY));

        if (mouseDownStack == null) {
            doHovering(path, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DefaultCancellableEvent.MousePositionButtonEvent event = new DefaultCancellableEvent.MousePositionButtonEvent(mouseX, mouseY, button);
        mouseDownStack = defaultMouseOverEvent(mouseX, mouseY, e -> e.mouseDown, mouseDown, Element::defaultMouseDownBehaviour, Element::blocksMouseActivation, event);
        if (event.cancellingDefault() || mouseDownStack == null) {
            return mouseDownStack != null;
        }
        for (int i = 0; i < mouseDownStack.size(); i++) {
            Element element = mouseDownStack.get(i);
            if (element.blocksMouseActivation()) {
                doSweepEvent(mouseDownStack.subList(0, i), e -> e.activation, null, Element::defaultActivationBehaviour, e -> true, new DefaultCancellableEvent());
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseDownStack != null) {
            doSweepEvent(mouseDownStack, e -> e.mouseUp, mouseUp,
                    Element::defaultMouseUpBehaviour, Element::blocksMouseActivation,
                    new DefaultCancellableEvent.MousePositionButtonEvent(mouseX, mouseY, button));

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
                    Element::defaultMouseDraggedBehaviour, Element::blocksMouseActivation,
                    new DefaultCancellableEvent.MousePositionButtonDeltaEvent(mouseX, mouseY, button, deltaX, deltaY));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return defaultMouseOverEvent(mouseX, mouseY, e -> e.mouseScrolled, mouseScrolled, Element::defaultMouseScrolledBehaviour,
                Element::blocksMouseActivation,
                new DefaultCancellableEvent.MousePositionAmountEvent(mouseX, mouseY, amount)) != null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFocused() == null) return false;
        List<Element> focusedPath = fire(getFocused(), e -> e.keyPressed, keyPressed,
                Element::defaultKeyPressedBehaviour, e -> true,
                new DefaultCancellableEvent.KeyEvent(keyCode, scanCode, modifiers));
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            doSweepEvent(focusedPath, e -> e.activation, null, Element::defaultActivationBehaviour, e -> true, new DefaultCancellableEvent());
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (getFocused() == null) return false;
        fire(getFocused(), e -> e.keyReleased, keyReleased,
                Element::defaultKeyReleasedBehaviour, e -> true,
                new DefaultCancellableEvent.KeyEvent(keyCode, scanCode, modifiers));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (getFocused() == null) return false;
        fire(getFocused(), e -> e.charTyped, charTyped,
                Element::defaultCharTypedBehaviour, e -> true,
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
