package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

abstract public class Element implements Rectangle {

    public final Observers.WritableObserver<Integer> width = Observers.of(0);
    public final Observers.WritableObserver<Integer> height = Observers.of(0);
    public final Observers.WritableObserver<Integer> x = Observers.of(0);
    public final Observers.WritableObserver<Integer> y = Observers.of(0);

    public final Observers.WritableObserver<Boolean> hovering = Observers.of(false);
    public final Observers.WritableObserver<Boolean> activeHovering = Observers.of(false);
    public final Observers.WritableObserver<Boolean> hoveringWithin = Observers.of(false);
    public final Observers.WritableObserver<Boolean> focused = Observers.of(false);
    public final Event<Consumer<DefaultCancellableEvent>> activation = Event.consumer();

    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonEvent>> mouseDown = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonEvent>> mouseUp = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionEvent>> mouseMove = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonDeltaEvent>> mouseDragged = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionAmountEvent>> mouseScrolled = Event.consumer();

    public final Event<Consumer<DefaultCancellableEvent.KeyEvent>> keyPressed = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.KeyEvent>> keyReleased = Event.consumer();

    public final Event<Consumer<DefaultCancellableEvent.CharEvent>> charTyped = Event.consumer();

    Rectangle clippingBox = null;

    private int componentDepth;
    private ParentElement<?> parent = null;
    private GuiState state = null;
    private boolean clip;

    private Surface surface = Surface.EMPTY;

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {

    }

    public boolean blocksMouseActivation() {
        return false;
    }

    public boolean hoveringMouseHitTest(double mouseX, double mouseY) {
        return true;
    }

    protected void defaultActivationBehaviour(DefaultCancellableEvent event) {

    }

    protected void defaultMouseDownBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {

    }

    protected void defaultMouseUpBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {

    }

    protected void defaultMouseMoveBehaviour(DefaultCancellableEvent.MousePositionEvent event) {

    }

    protected void defaultMouseDraggedBehaviour(DefaultCancellableEvent.MousePositionButtonDeltaEvent event) {

    }

    protected void defaultMouseScrolledBehaviour(DefaultCancellableEvent.MousePositionAmountEvent event) {

    }

    protected void defaultKeyPressedBehaviour(DefaultCancellableEvent.KeyEvent event) {

    }

    protected void defaultKeyReleasedBehaviour(DefaultCancellableEvent.KeyEvent event) {

    }

    protected void defaultCharTypedBehaviour(DefaultCancellableEvent.CharEvent event) {

    }

    public void renderSelfDebug(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int color = 0xff5555ff;
        if (clip) {
            color = 0xff0022ff;
        }
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, color);
        graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), color);
        graphics.fill(getX(), getY() + 1, getX() + 1, getY() + getHeight() - 1, color);
        graphics.fill(getX() + getWidth() - 1, getY() + 1, getX() + getWidth(), getY() + getHeight() - 1, color);

        MutableComponent text = Component.empty();
        if (hovering.get()) {
            text.append("hovered ");
        }
        if (activeHovering.get()) {
            text.append("activeHovered ");
        }
        if (hoveringWithin.get()) {
            text.append("hoveredWithin ");
        }
        if (isFocused()) {
            text.append("focused ");
        }
        graphics.fill(getX(), getY() + getHeight(), getX() + Minecraft.getInstance().font.width(text), getY() + 9 + getHeight(), 0xffffffff);
        graphics.drawString(Minecraft.getInstance().font, text, getX(), getY() + getHeight(), 0xff000000, false);
    }

    public boolean isFocused() {
        return getState().getFocused() == this;
    }

    public int getX() {
        return x.get();
    }

    public void setX(int x) {
        this.x.set(x);
    }

    public int getY() {
        return y.get();
    }

    public void setY(int y) {
        this.y.set(y);
    }

    public int getWidth() {
        return width.get();
    }

    public void setWidth(int width) {
        this.width.set(width);
    }

    public int getHeight() {
        return height.get();
    }

    public void setHeight(int height) {
        this.height.set(height);
    }

    public ParentElement<?> getParent() {
        return parent;
    }

    void setParent(ParentElement<?> parent) {
        this.parent = parent;
    }

    public int getDepth() {
        return componentDepth;
    }

    void setDepth(int depth) {
        this.componentDepth = depth;
    }

    public Surface getSurface() {
        return surface;
    }

    public Element setSurface(Surface surface) {
        this.surface = surface;
        getState().markClipDirty();
        return this;
    }

    public Rectangle getInnerSpace() {
        return this;
    }

    protected void markLayoutDirty() {
        if (getParent() != null && getParent().willCauseReflow(this)) {
            getParent().markLayoutDirty();
        }
    }

    public boolean shouldClip() {
        return clip;
    }

    public void setClipping(boolean clip) {
        this.clip = clip;
        getState().markClipDirty();
    }

    public <T extends DefaultCancellableEvent> boolean doSweepingEvent(Function<Element, Event<Consumer<T>>> eventGetter, T event, Function<Iterable<Element>, Element> forwarder) {
        eventGetter.apply(this).getSink().run(e -> e.accept(event));
        return event.isPropagating();
    }

    public boolean collectPath(Element target, Consumer<Element> collector) {
        if (this == target) {
            collector.accept(this);
            return true;
        }
        return false;
    }

    public GuiState getState() {
        if (state == null) {
            state = new GuiState(this);
        }
        return state;
    }

    void setState(GuiState state) {
        this.state = state;
    }

    void visit(BiConsumer<ParentElement<?>, Element> consumer) {
    }

    public abstract int getOptimalWidth();

    public abstract int getOptimalHeight(int width);
}
