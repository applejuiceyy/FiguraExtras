package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.ReadableRectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.github.applejuiceyy.figuraextras.util.Util.maybeTry;

abstract public class ParentElement<S extends ParentElement.Settings> extends Element {
    public final Observers.WritableObserver<Integer> xView = Observers.of(0);
    public final Observers.WritableObserver<Integer> yView = Observers.of(0);
    public final Observers.WritableObserver<Integer> yViewSize = Observers.of(0);
    public final Observers.WritableObserver<Integer> xViewSize = Observers.of(0);
    private final HashMap<Element, S> settings = new HashMap<>();
    boolean constrainX = true, constrainY = true;
    int previousX, previousY;
    int previousXView, previousYView;
    private boolean needReposition = false;
    private boolean needReflowLayout = false;
    private boolean disableChildrenDirtyRequests = false;
    private List<Element> needReflowDetached = new ArrayList<>();

    {
        xView.observe(x -> {
            ifState(state -> state.childReprocessor.enqueue(this));
            enqueueDirtySection(false, false, false);
            ;
            needReposition = true;
            previousX -= previousXView - x;
            previousXView = x;
        });
        yView.observe(y -> {
            ifState(state -> state.childReprocessor.enqueue(this));
            enqueueDirtySection(false, false, false);
            needReposition = true;
            previousY -= previousYView - y;
            previousYView = y;
        });
        width.observe(() -> {
            ifState(state -> state.childReprocessor.enqueue(this));
            xView.set(Math.max(0, Math.min(xView.get(), xViewSize.get() - getWidth())));
            needReflowLayout = true;
        });
        xViewSize.observe(() -> {
            xView.set(Math.max(0, Math.min(xView.get(), xViewSize.get() - getWidth())));
        });
        height.observe(() -> {
            ifState(state -> state.childReprocessor.enqueue(this));
            yView.set(Math.max(0, Math.min(yView.get(), yViewSize.get() - getHeight())));
            needReflowLayout = true;
        });
        yViewSize.observe(() -> {
            yView.set(Math.max(0, Math.min(yView.get(), yViewSize.get() - getHeight())));
        });
        x.merge(y).observe(() -> {
            ifState(state -> state.childReprocessor.enqueue(this));
            needReposition = true;
        });
    }

    public boolean doConstrainX() {
        return constrainX;
    }

    public ParentElement<S> setConstrainX(boolean constrainX) {
        this.constrainX = constrainX;
        boundingChanged();
        ifState(state -> state.childReprocessor.enqueue(this));
        needReflowLayout = true;
        return this;
    }

    public boolean doConstrainY() {
        return constrainY;
    }

    public ParentElement<S> setConstrainY(boolean constrainY) {
        this.constrainY = constrainY;
        boundingChanged();
        ifState(state -> state.childReprocessor.enqueue(this));
        needReflowLayout = true;
        return this;
    }

    public int getXView() {
        return xView.get();
    }

    public ParentElement<S> setXView(int xview) {
        this.xView.set(xview);
        return this;
    }

    public int getYView() {
        return yView.get();
    }

    public ParentElement<S> setYView(int yview) {
        this.yView.set(yview);
        return this;
    }

    public void positionElements() {
        assert getState() != null;
        if (needReflowLayout) {
            positionElements(flowElements(true));
            needReposition = false;
            needReflowLayout = false;
        }

        if (needReposition) {
            boolean disable = shouldClip() || disableChildrenDirtyRequests;
            try (var ignored = maybeTry(getState().updateDirtySections::rejectNewEntries, disable)) {
                for (Element element : getElements()) {
                    if (element instanceof ParentElement<?> pe) {
                        pe.disableChildrenDirtyRequests = disable;
                    }
                    int relativeX = element.getX() - previousX,
                            relativeY = element.getY() - previousY;
                    element.setX(relativeX + getX());
                    element.setY(relativeY + getY());
                }
                needReposition = false;
            }
            disableChildrenDirtyRequests = false;
        }

        for (Element element : needReflowDetached) {
            S settings = getSettings(element);
            if (settings.doLayout()) continue;
            int width = settings.getWidth();
            int height = settings.getHeight();
            if (settings.isOptimalWidth()) {
                width = element.getOptimalWidth();
            }
            if (settings.isOptimalHeight()) {
                height = element.getOptimalHeight(width);
            }
            element.setX(settings.getX());
            element.setY(settings.getY());
            element.setWidth(width + settings.getOffsetWidth());
            element.setHeight(height + settings.getOffsetHeight());
        }
        needReflowDetached.clear();
        previousX = getX();
        previousY = getY();
    }

    protected void childrenChanged() {
        if (settings.isEmpty()) return;
        optimalSizeChanged();
        ifState(state -> {
            state.childReprocessor.enqueue(this);
            needReflowLayout = true;
        });
    }

    public void childOptimalSizeChanged(Element element) {
        assert hasElement(element);
        if (willCauseReflow(element)) {
            childrenChanged();
        }
    }

    protected void boundingChanged() {
        if (settings.isEmpty()) return;
        ifState(state -> {
            state.childReprocessor.enqueue(this);
            needReflowLayout = true;
        });
    }

    @Override
    protected void markRenderingThisAndChildrenDirty(Consumer<ReadableRectangle> sectionConsumer, Processor<Element> processor) {
        super.markRenderingThisAndChildrenDirty(sectionConsumer, processor);
        ifState(state -> {
            for (Element e : getElements()) {

                e.enqueueDirtySectionImmediately(state, false, true);
            }
        });
    }

    public S getSettings(Element element) {
        if (settings.containsKey(element)) {
            return settings.get(element);
        } else throw new NotAChildException("Not a child");
    }

    public Iterable<Tuple<Element, S>> flowElements(boolean flowing) {
        return () -> getElements()
                .stream()
                .filter(element -> getSettings(element).doLayout() == flowing)
                .map(element -> new Tuple<>(element, getSettings(element)))
                .iterator();
    }

    @Override
    void setStateInternal(GuiState state) {
        super.setStateInternal(state);
        getElements().forEach(e -> e.setStateInternal(state));
    }

    @Override
    void setDepth(int depth) {
        super.setDepth(depth);
        getElements().forEach(e -> e.setDepth(getDepth() + 1));
    }

    @Override
    void setInTree(boolean inTree) {
        super.setInTree(inTree);
        getElements().forEach(e -> e.setInTree(isInTree()));
    }

    @Override
    public boolean collectPath(Element target, Consumer<Element> collector) {
        if (super.collectPath(target, collector)) {
            return true;
        }
        for (Element element : getElements()) {
            if (element.collectPath(target, collector)) {
                collector.accept(this);
                return true;
            }
        }
        return false;
    }

    public AdditionPoint adder(Consumer<S> settings) {
        return new AdditionPoint() {
            Element el = null;

            @Override
            public void accept(Element element) {
                remove();
                settings.accept(add(element));
                el = element;
            }

            @Override
            public void remove() {
                if (el != null) {
                    ParentElement.this.remove(el);
                    el = null;
                }
            }

            @Override
            public void ensureRemoved() {
                if (el != null) {
                    throw new AdditionPointNotCleared("Addition point is not cleared");
                }
            }
        };
    }

    public S add(Element element) {
        if (element.getParent() != null) {
            element.getParent().remove(element);
        }
        element.setStateInternal(getState());
        element.setDepth(getDepth() + 1);
        element.setParent(this);
        element.setInTree(true);
        //noinspection unchecked
        S[] s = (S[]) new Settings[]{null};
        s[0] = constructSettings(() -> this.willCauseReflow(s[0]), () -> needReflowDetached.add(element));
        markPriorityDirty();
        needReflowLayout = true;
        settings.put(element, s[0]);
        ifState(state -> state.clipDirty.enqueue(element));
        element.enqueueDirtySection(false, true);
        childrenChanged();
        return s[0];
    }

    public ParentElement<S> addAnd(Element element) {
        add(element);
        return this;
    }

    public S add(String text) {
        return add(Component.literal(text));
    }

    public S add(Component component) {
        return add(new Label(component));
    }

    public ParentElement<S> addAnd(Component component) {
        add(component);
        return this;
    }

    public ParentElement<S> addAnd(String text) {
        add(text);
        return this;
    }

    protected void markPriorityDirty() {
        whenState(GuiState::markPriorityDirty);
    }

    public void remove(Element element) {
        if (element.getParent() != this) {
            return;
        }
        if (element.hasRendered) {
            if (element instanceof ParentElement<?> ep) {
                element.enqueueDirtySection(false, !ep.shouldClip());
            } else {
                element.enqueueDirtySection(false, true);
            }
        } else {
            element.dequeueDirtySection();
        }

        element.setInTree(false);
        element.setParent(null);
        element.setDepth(0);
        markPriorityDirty();
        childrenChanged();
        settings.remove(element);
    }

    public Set<Element> getElements() {
        return settings.keySet();
    }

    public boolean hasElement(Element element) {
        return settings.containsKey(element);
    }

    @Override
    public Rectangle getInnerSpace() {
        if (yViewSize.get() <= height.get() && xViewSize.get() <= width.get()) {
            return super.getInnerSpace();
        }
        return Rectangle.of(getX() - xView.get(), getY() - yView.get(), xViewSize.get(), yViewSize.get());
    }

    void visit(BiConsumer<ParentElement<?>, Element> consumer) {
        for (Element element : getElements()) {
            consumer.accept(this, element);
            element.visit(consumer);
        }
    }


    abstract protected S constructSettings(BooleanSupplier willCauseReflow, Runnable reflowDetached);

    /***
     * Obligations:
     * the optimal size and final size of an element may be overridden by isOptimalWidth and getWidth, and isOptimalHeight and getHeight
     * the final position may be translated by getX, getOffsetX, getY and getOffsetY
     * implementation must take doConstrainX, doConstrainY into consideration
     * implementation must update xViewSize, yViewSize
     * implementation must translate elements according to self's size and view offset
     * implementation should update xViewSize and yViewSize before setting elements
     */
    abstract public void positionElements(Iterable<Tuple<Element, S>> elements);

    public void renderLayoutDebug(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    protected boolean willCauseReflow(S settings) {
        return settings.doLayout();
    }

    ;

    protected boolean willCauseReflow(Element element) {
        return willCauseReflow(getSettings(element));
    }

    ;

    public interface AdditionPoint extends Consumer<Element> {
        void remove();

        void ensureRemoved();
    }

    static public class Settings {

        private final BooleanSupplier willCauseReflow;
        private final ParentElement<?> owner;
        private final Runnable reflowDetached;
        private boolean doLayout = true;
        private boolean invisible = false;

        private boolean customPriority = false;
        private int priority = 0;
        private boolean optimalWidth = true;
        private boolean optimalHeight = true;
        private int width = 0;
        private int height = 0;
        private int offsetWidth = 0;
        private int offsetHeight = 0;
        private int x = 0;
        private int y = 0;

        public Settings(BooleanSupplier willCauseReflow, Runnable reflowDetached, ParentElement<?> owner) {
            this.willCauseReflow = willCauseReflow;
            this.reflowDetached = reflowDetached;
            this.owner = owner;
        }

        protected void markPriorityDirty() {
            owner.markPriorityDirty();
        }

        protected void mayDirtLayout(Runnable runnable) {
            boolean prev = willCauseReflow.getAsBoolean();
            boolean layout = doLayout;
            runnable.run();
            if (prev || willCauseReflow.getAsBoolean() || !doLayout) {
                owner.childrenChanged();
                if (layout || doLayout) {
                    owner.needReflowLayout = true;
                }
                if (!doLayout) {
                    reflowDetached.run();
                }
            }
        }

        public boolean doLayout() {
            return doLayout;
        }

        public Settings setDoLayout(boolean doLayout) {
            mayDirtLayout(() -> this.doLayout = doLayout);
            return this;
        }

        public boolean isInvisible() {
            return invisible;
        }

        public void setInvisible(boolean invisible) {
            this.invisible = invisible;
            markPriorityDirty();
        }

        public OptionalInt getPriority() {
            return customPriority ? OptionalInt.of(priority) : OptionalInt.empty();
        }

        @Contract("!null->fail")
        public Settings setPriority(Object n) {
            if (n != null) throw new NullPointerException("Not null");
            customPriority = false;
            markPriorityDirty();
            return this;
        }

        public Settings setPriority(int priority) {
            this.priority = priority;
            customPriority = true;
            markPriorityDirty();
            return this;
        }

        public boolean isOptimalWidth() {
            return optimalWidth;
        }

        public Settings setOptimalWidth(boolean optimalWidth) {
            mayDirtLayout(() -> this.optimalWidth = optimalWidth);
            return this;
        }

        public boolean isOptimalHeight() {
            return optimalHeight;
        }

        public Settings setOptimalHeight(boolean optimalHeight) {
            mayDirtLayout(() -> this.optimalHeight = optimalHeight);
            return this;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            mayDirtLayout(() -> this.width = width);
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            mayDirtLayout(() -> this.height = height);
        }

        public int getOffsetWidth() {
            return offsetWidth;
        }

        public void setOffsetWidth(int offsetWidth) {
            this.offsetWidth = offsetWidth;
        }

        public int getOffsetHeight() {
            return offsetHeight;
        }

        public void setOffsetHeight(int offsetHeight) {
            this.offsetHeight = offsetHeight;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    static class NotAChildException extends RuntimeException {
        public NotAChildException(String string) {
            super(string);
        }
    }

    static class AdditionPointNotCleared extends RuntimeException {
        public AdditionPointNotCleared(String string) {
            super(string);
        }
    }
}
