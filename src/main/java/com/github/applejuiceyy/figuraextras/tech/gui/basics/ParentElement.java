package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

abstract public class ParentElement<S extends ParentElement.Settings> extends Element {
    public final Observers.WritableObserver<Integer> xView = Observers.of(0);
    public final Observers.WritableObserver<Integer> yView = Observers.of(0);
    public final Observers.WritableObserver<Integer> yViewSize = Observers.of(0);
    public final Observers.WritableObserver<Integer> xViewSize = Observers.of(0);
    private final HashMap<Element, S> settings = new HashMap<>();
    protected boolean needReposition = false;
    protected boolean needReflowLayout = false;
    protected List<Element> needReflowDetached = new ArrayList<>();
    boolean constrainX = true, constrainY = true;
    int previousX, previousY;
    int previousXView, previousYView;

    {
        xView.observe(x -> {
            this.getState().addChildrenReprocessingTask(this);
            needReposition = true;
            previousX -= previousXView - x;
            previousXView = x;
        });
        yView.observe(y -> {
            this.getState().addChildrenReprocessingTask(this);
            needReposition = true;
            previousY -= previousYView - y;
            previousYView = y;
        });
        width.observe(() -> {
            this.getState().addChildrenReprocessingTask(this);
            needReflowLayout = true;
        });
        xViewSize.observe(() -> {
            xView.set(Math.max(0, Math.min(xView.get(), xViewSize.get() - getWidth())));
        });
        height.observe(() -> {
            this.getState().addChildrenReprocessingTask(this);
            needReflowLayout = true;
        });
        yViewSize.observe(() -> {
            yView.set(Math.max(0, Math.min(yView.get(), yViewSize.get() - getHeight())));
        });
        x.merge(y).observe(() -> {
            this.getState().addChildrenReprocessingTask(this);
            needReposition = true;
        });
    }

    public boolean doConstrainX() {
        return constrainX;
    }

    public ParentElement<S> setConstrainX(boolean constrainX) {
        this.constrainX = constrainX;
        this.getState().addChildrenReprocessingTask(this);
        needReflowLayout = true;
        return this;
    }

    public boolean doConstrainY() {
        return constrainY;
    }

    public ParentElement<S> setConstrainY(boolean constrainY) {
        this.constrainY = constrainY;
        this.getState().addChildrenReprocessingTask(this);
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
        if (needReflowLayout) {
            positionElements(flowElements(true));
            needReposition = false;
            needReflowLayout = false;
        }

        if (needReposition) {
            for (Element element : getElements()) {
                int relativeX = element.getX() - previousX,
                        relativeY = element.getY() - previousY;
                element.setX(relativeX + getX());
                element.setY(relativeY + getY());
            }
            needReposition = false;
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
        getState().removeChildrenReprocessingTask(this);
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
    void setState(GuiState state) {
        super.setState(state);
        getElements().forEach(e -> e.setState(state));
    }

    @Override
    void setDepth(int depth) {
        super.setDepth(depth);
        getElements().forEach(e -> e.setDepth(getDepth() + 1));
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

    public S add(Element element) {
        if (element.getParent() != null) {
            element.getParent().remove(element);
        }
        element.setState(getState());
        element.setDepth(getDepth() + 1);
        element.setParent(this);
        //noinspection unchecked
        S[] s = (S[]) new Settings[]{null};
        s[0] = constructSettings(() -> this.willCauseReflow(s[0]), () -> needReflowDetached.add(element));
        markPriorityDirty();
        markLayoutDirty();
        needReflowLayout = true;
        getState().markClipDirty();
        settings.put(element, s[0]);
        return s[0];
    }

    public ParentElement<S> addAnd(Element element) {
        add(element);
        return this;
    }

    public Settings add(String text) {
        return add(new Label(Component.literal(text)));
    }

    public ParentElement<S> addAnd(String text) {
        add(text);
        return this;
    }

    @Override
    protected void markLayoutDirty() {
        super.markLayoutDirty();
        this.getState().addChildrenReprocessingTask(this);
        needReflowLayout = true;
    }

    protected void markPriorityDirty() {
        this.getState().markPriorityDirty();
    }

    public void remove(Element element) {
        if (element.getParent() != this) {
            return;
        }
        element.setState(null);
        element.setParent(null);
        element.setDepth(0);
        markPriorityDirty();
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

    abstract public int getOptimalWidth();

    abstract public int getOptimalHeight(int width);

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

    static public class Settings {

        private final BooleanSupplier willCauseReflow;
        private final ParentElement<?> owner;
        private final Runnable reflowDetached;
        private boolean doLayout = true;
        private boolean invisible = false;
        private int priority = 0;
        private boolean isolatePriority = false;
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
                owner.markLayoutDirty();
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

        public void setDoLayout(boolean doLayout) {
            mayDirtLayout(() -> this.doLayout = doLayout);
        }

        public boolean isInvisible() {
            return invisible;
        }

        public void setInvisible(boolean invisible) {
            this.invisible = invisible;
            markPriorityDirty();
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
            markPriorityDirty();
        }

        public boolean isolatePriority() {
            return isolatePriority;
        }

        public void setIsolatePriority(boolean isolatePriority) {
            this.isolatePriority = isolatePriority;
            markPriorityDirty();
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

        public void setOptimalHeight(boolean optimalHeight) {
            mayDirtLayout(() -> this.optimalHeight = optimalHeight);
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
}
