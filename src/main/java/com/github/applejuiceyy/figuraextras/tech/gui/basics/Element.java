package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.tech.gui.geometry.ImmutableRectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.ReadableRectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

abstract public class Element implements Rectangle {

    public final Observers.WritableObserver<Integer> width = Observers.of(0);
    public final Observers.WritableObserver<Integer> height = Observers.of(0);
    public final Observers.WritableObserver<Integer> x = Observers.of(0);
    public final Observers.WritableObserver<Integer> y = Observers.of(0);

    private final Int2IntOpenHashMap cachedOptimalHeight = new Int2IntOpenHashMap();
    boolean hasRendered = false;
    ImmutableRectangle clippingBox = null;
    private @Nullable ReadableRectangle previousRestingPosition = null;
    public final Observers.WritableObserver<Boolean> hovering = Observers.of(false);
    public final Observers.WritableObserver<HoverIntent> hoveringKind = Observers.of(HoverIntent.NONE);
    public final Observers.WritableObserver<Boolean> hoveringWithin = Observers.of(false);
    public final Observers.WritableObserver<Boolean> focused = Observers.of(false);
    public final Event<Consumer<DefaultCancellableEvent.CausedEvent<Either<DefaultCancellableEvent.KeyEvent, DefaultCancellableEvent.MousePositionButtonEvent>>>> activation = Event.consumer();


    public final Event<Consumer<DefaultCancellableEvent.ToolTipEvent>> tooltip = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonEvent>> mouseDown = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonEvent>> mouseUp = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionEvent>> mouseMove = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionButtonDeltaEvent>> mouseDragged = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.MousePositionAmountEvent>> mouseScrolled = Event.consumer();

    public final Event<Consumer<DefaultCancellableEvent.KeyEvent>> keyPressed = Event.consumer();
    public final Event<Consumer<DefaultCancellableEvent.KeyEvent>> keyReleased = Event.consumer();

    public final Event<Consumer<DefaultCancellableEvent.CharEvent>> charTyped = Event.consumer();
    private boolean haveTranslated = false;
    private boolean recurse = false;
    private boolean doSelf = false;

    private int componentDepth;
    private ParentElement<?> parent = null;
    private GuiState state = null;

    private boolean isInTree = false;

    private ArrayList<Consumer<GuiState>> queuedStateOperations = null;
    private boolean clip;

    private int cachedOptimalWidth = -1;

    private Surface surface = Surface.EMPTY;

    {
        x.observe(() -> {
            ifState(state -> {
                this.enqueueDirtySectionImmediately(state, true, false);
                state.clipDirty.enqueue(this);
            });
        });
        y.observe(() -> {
            ifState(state -> {
                this.enqueueDirtySectionImmediately(state, true, false);
                state.clipDirty.enqueue(this);
            });
        });
        width.observe(() -> {
            ifState(state -> {
                this.enqueueDirtySectionImmediately(state, true, false);
                state.clipDirty.enqueue(this);
            });
        });
        height.observe(() -> {
            ifState(state -> {
                this.enqueueDirtySectionImmediately(state, true, false);
                state.clipDirty.enqueue(this);
            });
        });
    }

    protected Processor.AdditionStatus enqueueDirtySectionImmediately(GuiState state, boolean translative, boolean recursive) {
        return enqueueDirtySectionImmediately(state, translative, recursive, true);
    }

    @Nullable
    protected Processor.AdditionStatus enqueueDirtySectionImmediately(GuiState state, boolean translative, boolean recursive, boolean check) {
        boolean should = !check || renders() || surface != Surface.EMPTY;
        if (should || recursive) {
            Processor.AdditionStatus enqueue = state.updateDirtySections.enqueue(this);
            if (!enqueue.rejected) {
                haveTranslated = (haveTranslated || translative) && hasRendered;
                recurse = recurse || recursive;
                doSelf = should || doSelf;
            }
            return enqueue;
        }
        return null;
    }

    protected void enqueueDirtySection(boolean translative, boolean recursive) {
        enqueueDirtySection(translative, recursive, true);
    }

    protected void enqueueDirtySection(boolean translative, boolean recursive, boolean check) {
        ifState(state -> enqueueDirtySectionImmediately(state, translative, recursive, check));
    }

    protected void dequeueDirtySection() {
        ifState(state -> {
            state.updateDirtySections.dequeue(this);
            haveTranslated = false;
            recurse = false;
        });
    }

    private void markPreviousRenderingLocationDirty(Consumer<ReadableRectangle> sectionConsumer) {
        // if(!renders()) return;
        ParentElement<?> parent = getParent();
        if (parent != null && parent.getSettings(this).isInvisible()) return;
        if (previousRestingPosition != null) {
            sectionConsumer.accept(previousRestingPosition);
        }
    }

    protected void markActualRenderingLocationDirty(Consumer<ReadableRectangle> sectionConsumer) {
        if (!doSelf) return;
        ParentElement<?> parent = getParent();
        if (parent != null && parent.getSettings(this).isInvisible()) return;
        ReadableRectangle rectangle = Rectangle.expansiveIntersectionOf(this.immutable(), clippingBox);
        if (rectangle != null) {
            sectionConsumer.accept(rectangle);
        }
        previousRestingPosition = rectangle;
    }

    protected void markRenderingThisAndChildrenDirty(Consumer<ReadableRectangle> sectionConsumer, Processor<Element> processor) {
        markActualRenderingLocationDirty(sectionConsumer);
    }

    void updateDirtySections(Consumer<ReadableRectangle> sectionConsumer, Processor<Element> processor) {
        if (haveTranslated) {
            markPreviousRenderingLocationDirty(sectionConsumer);
            hasRendered = false;
        }
        haveTranslated = false;
        if (recurse) {
            markRenderingThisAndChildrenDirty(sectionConsumer, processor);
        } else {
            markActualRenderingLocationDirty(sectionConsumer);
        }
        recurse = false;
        doSelf = false;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {

    }

    protected boolean renders() {
        try {
            return !this.getClass()
                    .getMethod("render", GuiGraphics.class, int.class, int.class, float.class)
                    .getDeclaringClass()
                    .equals(Element.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public HoverIntent mouseHoverIntent(double mouseX, double mouseY) {
        return HoverIntent.NONE;
    }

    public boolean hoveringMouseHitTest(double mouseX, double mouseY) {
        return true;
    }

    protected void defaultToolTipBehaviour(DefaultCancellableEvent.ToolTipEvent event) {

    }

    protected void defaultActivationBehaviour(DefaultCancellableEvent.CausedEvent<Either<DefaultCancellableEvent.KeyEvent, DefaultCancellableEvent.MousePositionButtonEvent>> event) {

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
        if (hoveringKind.get() != HoverIntent.NONE) {
            text.append("intentiousHovering ");
        }
        if (hoveringWithin.get()) {
            text.append("hoveredWithin ");
        }
        if (isFocused()) {
            text.append("focused ");
        }
        graphics.fill(getX(), getY() + getHeight(), getX() + Minecraft.getInstance().font.width(text), getY() + 9 + getHeight(), 0xffffffff);
        graphics.drawString(Minecraft.getInstance().font, text, getX(), getY() + getHeight(), 0xff000000, false);

        String str = this.getClass().getSimpleName();
        graphics.drawString(Minecraft.getInstance().font, str, getX(), getY(), 0xffffffff, false);
    }

    protected void optimalSizeChanged() {
        cachedOptimalWidth = -1;
        cachedOptimalHeight.clear();
        ParentElement<?> parent = getParent();
        if (parent != null) {
            parent.childOptimalSizeChanged(this);
        }
    }

    public boolean isFocused() {
        return getState() != null && getState().getFocused() == this;
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

    public boolean isInTree() {
        return isInTree;
    }

    void setInTree(boolean inTree) {
        if (!inTree && isFocused()) {
            assert getState() != null;
            getState().setFocused(null);
        }
        isInTree = inTree;
    }

    public Surface getSurface() {
        return surface;
    }

    public Element setSurface(Surface surface) {
        enqueueDirtySection(false, false);
        this.surface = surface;
        enqueueDirtySection(false, false);
        return this;
    }

    public Rectangle getInnerSpace() {
        return this;
    }

    public boolean shouldClip() {
        return clip;
    }

    public void setClipping(boolean clip) {
        this.clip = clip;
        ifState(state -> {
            state.clipDirty.enqueue(this);
            state.markPriorityDirty();
        });
    }

    public <T extends DefaultCancellableEvent> boolean doSweepingEvent(Function<Element, Event<Consumer<T>>> eventGetter, T event, Function<Iterable<Element>, Element> forwarder) {
        eventGetter.apply(this).getSink().accept(event);
        return event.isPropagating();
    }

    public boolean collectPath(Element target, Consumer<Element> collector) {
        if (this == target) {
            collector.accept(this);
            return true;
        }
        return false;
    }

    public @Nullable GuiState getState() {
        return state;
    }

    void setState(GuiState state) {
        if (parent != null) {
            throw new RuntimeException("Cannot create a state on a child");
        }
        setStateInternal(state);
    }

    public boolean ifState(Consumer<GuiState> stateOperation) {
        if (this.state == null) {
            return false;
        } else {
            stateOperation.accept(this.state);
            return true;
        }
    }

    public boolean whenState(Consumer<GuiState> stateOperation) {
        if (this.state == null) {
            if (queuedStateOperations == null) {
                queuedStateOperations = new ArrayList<>();
            }
            queuedStateOperations.add(stateOperation);
            return false;
        } else {
            stateOperation.accept(this.state);
            return true;
        }
    }

    void setStateInternal(GuiState state) {
        this.state = state;
        if (state != null && queuedStateOperations != null) {
            queuedStateOperations.forEach(c -> c.accept(state));
            queuedStateOperations = null;
        }
    }

    void visit(BiConsumer<ParentElement<?>, Element> consumer) {
    }

    public int getOptimalWidth() {
        if (cachedOptimalWidth == -1) {
            cachedOptimalWidth = computeOptimalWidth();
        }
        return cachedOptimalWidth;
    }


    public int getOptimalHeight(int width) {
        return cachedOptimalHeight.computeIfAbsent(width, this::computeOptimalHeight);
    }


    abstract public int computeOptimalWidth();

    abstract public int computeOptimalHeight(int width);

    public enum HoverIntent {
        NONE(false, false), LOOK(false, true), INTERACT(true, false), INTERACT_LOOK(true, true);

        public final boolean interact;
        public final boolean look;

        HoverIntent(boolean interact, boolean look) {
            this.interact = interact;
            this.look = look;
        }
    }
}
