package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GraphComponent extends Element {
    private final List<DataCollection> dataCollections = new ArrayList<>();
    private @Nullable Axis axisRenderer = Axis.createDefault();
    private final List<InGraphRendererBaker> renderers = new ArrayList<>();

    private final Map<DataCollection, Runnable> dataCollectionsInvalidators = new HashMap<>();
    private @Nullable Runnable axisRendererInvalidator;
    private final Map<InGraphRendererBaker, Runnable> renderersInvalidators = new HashMap<>();


    private @Nullable Float xMin, xMax, yMin, yMax;
    private @Nullable UnaryOperator<Bounds> boundsEffect;

    private boolean recalculateElements = true;


    private final List<InGraphRenderer> bakedPlots = new ArrayList<>();
    private @Nullable Axis.BakedAxisRenderer axisRendererBaked = null;
    private final List<InGraphRenderer> bakedRenderers = new ArrayList<>();

    private @Nullable Rectangle bakedGraphRealEstate = null;
    private @Nullable Bounds bakedDataBounds = null;
    private @Nullable Runnable tooltipDisposer = () -> {};


    public GraphComponent() {
        width.observe(() -> recalculateElements = true);
        height.observe(() -> recalculateElements = true);
    }

    @Override
    public HoverIntent mouseHoverIntent(double mouseX, double mouseY) {
        return HoverIntent.LOOK;
    }

    private void invalidateTooltip() {
        if(tooltipDisposer == null) return;
        tooltipDisposer.run();
        tooltipDisposer = null;
    }

    private void invalidate() {
        recalculateElements = true;
        invalidateTooltip();
        enqueueDirtySection(false, false);
    }

    public @Nullable Float getXMin() {
        return xMin;
    }

    public GraphComponent setXMin(@Nullable Float xMin) {
        this.xMin = xMin;
        invalidate();
        return this;
    }

    public @Nullable Float getXMax() {
        return xMax;
    }

    public GraphComponent setXMax(@Nullable Float xMax) {
        this.xMax = xMax;
        invalidate();
        return this;
    }

    public @Nullable Float getYMin() {
        return yMin;
    }

    public GraphComponent setYMin(@Nullable Float yMin) {
        this.yMin = yMin;
        invalidate();
        return this;
    }

    public @Nullable Float getYMax() {
        return yMax;
    }

    public GraphComponent setYMax(@Nullable Float yMax) {
        this.yMax = yMax;
        invalidate();
        return this;
    }

    public @Nullable UnaryOperator<Bounds> getBoundsEffect() {
        return boundsEffect;
    }

    public GraphComponent setBoundsEffect(@Nullable UnaryOperator<Bounds> boundsEffect) {
        this.boundsEffect = boundsEffect;
        invalidate();
        return this;
    }

    public @Nullable Axis getAxisRenderer() {
        return axisRenderer;
    }

    public GraphComponent setAxisRenderer(@Nullable Axis axisRenderer) {
        this.axisRenderer = axisRenderer;
        if(axisRendererInvalidator != null) {
            axisRendererInvalidator.run();
            axisRendererInvalidator = null;
        }
        if(axisRenderer != null) {
            axisRendererInvalidator = axisRenderer.getInvalidator().subscribe(this::invalidate);
        }
        return this;
    }

    @Nullable
    public Bounds getCalculatedDataBounds() {
        return calculateDataBounds(true);
    }

    @Nullable
    private Bounds calculateDataBounds(boolean allWanted) {
        boolean hasBounds = false;
        double ax, ay, bx, by;
        ax = ay = Float.POSITIVE_INFINITY;
        bx = by = Float.NEGATIVE_INFINITY;
        for (DataCollection plot : dataCollections) {
            Bounds dataBounds = allWanted ?
                    plot.getDataBounds(true, true, true, true) :
                    plot.getDataBounds(
                    xMin == null,
                    yMin == null,
                    xMax == null,
                    yMax == null
                    );

            if(dataBounds != null) {
                hasBounds = true;
                ax = Math.min(ax, dataBounds.xMin());
                ay = Math.min(ay, dataBounds.yMin());
                bx = Math.max(bx, dataBounds.xMax());
                by = Math.max(by, dataBounds.yMax());
            }
        }
        if(!hasBounds) return null;

        if(ax == bx) {
            ax -= 0.1f;
            bx += 0.1f;
        }
        if(ay == by) {
            ay -= 0.1f;
            by += 0.1f;
        }

        Bounds bounds = new Bounds(ax, ay, bx, by);
        return boundsEffect == null ? bounds : boundsEffect.apply(bounds);
    }

    @Nullable
    public Bounds getFinalDataBounds() {
        if(xMin != null && xMax != null && yMin != null && yMax != null) {
            return new Bounds(xMin, yMin, xMax, yMax);
        }
        Bounds calculatedDataBounds = calculateDataBounds(false);
        if(calculatedDataBounds == null) return null;
        if(xMin == null && yMin == null && xMax == null && yMax == null) return calculatedDataBounds;
        double xMin, yMin, xMax, yMax;

        xMin = calculatedDataBounds.xMin();
        yMin = calculatedDataBounds.yMin();
        xMax = calculatedDataBounds.xMax();
        yMax = calculatedDataBounds.yMax();

        if(this.xMin != null) xMin = this.xMin;
        if(this.yMin != null) yMin = this.yMin;
        if(this.xMax != null) xMax = this.xMax;
        if(this.yMax != null) yMax = this.yMax;

        return new Bounds(xMin, yMin, xMax, yMax);
    }

    public GraphComponent addCollection(DataCollection dataCollection) {
        Runnable un = dataCollection.getInvalidator().subscribe(this::invalidate);
        dataCollectionsInvalidators.put(dataCollection, un);
        dataCollections.add(dataCollection);
        invalidate();
        return this;
    }

    public GraphComponent removeCollection(DataCollection collection) {
        dataCollectionsInvalidators.remove(collection).run();
        dataCollections.remove(collection);
        invalidate();
        return this;
    }

    public GraphComponent addRenderer(InGraphRendererBaker renderer) {
        Runnable un = renderer.getInvalidator().subscribe(this::invalidate);
        renderersInvalidators.put(renderer, un);
        renderers.add(renderer);
        invalidate();
        return this;
    }

    public GraphComponent removeRenderer(InGraphRendererBaker renderer) {
        renderersInvalidators.remove(renderer).run();
        renderers.remove(renderer);
        invalidate();
        return this;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if(recalculateElements) {
            calculateElements();
        }
        //noinspection deprecation
        context.drawManaged(() -> {
            renderManaged(context);
        });


        /*
        if (!dataPoints.isEmpty()) {
            Matrix4f matrix4f = context.pose().last().pose();
            RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            fallOff += 0.1;
            maxInstructionsSeen -= (int) fallOff;

            if (maxInstructionsSeen < 100) {
                maxInstructionsSeen = 100;
            }

            long currentDataPoints = System.currentTimeMillis() - lastEntry;
            for (int i = dataPoints.size() - 1; i >= 1; i--) {
                DataPoint dataPoint = dataPoints.get(i);
                DataPoint next = dataPoints.get(i - 1);

                float shortEnd = width.get() - (currentDataPoints / (float) cutoff) * width.get();
                float bigEnd = width.get() - ((currentDataPoints + dataPoint.afterMillis) / (float) cutoff) * width.get();
                float shortHeight = height.get() - (dataPoint.instructions / (float) maxInstructionsSeen) * height.get();
                float bigHeight = height.get() - (next.instructions / (float) maxInstructionsSeen) * height.get();

                currentDataPoints += dataPoint.afterMillis;

                if (maxInstructionsSeen < (dataPoint.instructions * 1.4)) {
                    fallOff = 0;
                    maxInstructionsSeen = (int) (dataPoint.instructions * 1.4);
                }

                bufferBuilder.vertex(matrix4f, bigEnd, bigHeight, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(matrix4f, bigEnd, height.get(), 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd, height.get(), 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd, shortHeight, 0).color(0xff00ff00).endVertex();

                bufferBuilder.vertex(matrix4f, shortEnd - 0.5f, shortHeight, 0).color(0xff00aa00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd - 0.5f, height.get(), 0).color(0xff00aa00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd + 0.5f, height.get(), 0).color(0xff00aa00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd + 0.5f, shortHeight, 0).color(0xff00aa00).endVertex();

                bufferBuilder.vertex(matrix4f, shortEnd, shortHeight - 1, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd - 1, shortHeight, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd, shortHeight + 1, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd + 1, shortHeight, 0).color(0xffff0000).endVertex();
            }

            int p = (maxInstructionsSeen / 1000) * 100 + 100;

            for (int i = 0; i < maxInstructionsSeen; i += p) {
                float h = height.get() - (i / (float) maxInstructionsSeen) * height.get();

                bufferBuilder.vertex(matrix4f, 0, h, 0).color(0xff888888).endVertex();
                bufferBuilder.vertex(matrix4f, 0, h + 0.5f, 0).color(0xff888888).endVertex();
                bufferBuilder.vertex(matrix4f, width.get(), h + 0.5f, 0).color(0xff888888).endVertex();
                bufferBuilder.vertex(matrix4f, width.get(), h, 0).color(0xff888888).endVertex();
            }

            BufferUploader.drawWithShader(bufferBuilder.end());

            for (int i = 0; i < maxInstructionsSeen; i += p) {
                float h = height.get() - (i / (float) maxInstructionsSeen) * height.get();

                context.drawString(Minecraft.getInstance().font, String.valueOf(i), 3, (int) h + 2, 0xff777777);
            }
        }

        context.fill(0, 0, 1, height.get(), 0xffffffff);
        context.fill(0, height.get() - 1, width.get(), height.get(), 0xffffffff);

        matrices.popPose();*/
    }

    private void calculateElements() {
        bakedDataBounds = getFinalDataBounds();
        bakedGraphRealEstate = null;

        if(bakedDataBounds == null) {
            return;
        }

        if(axisRenderer == null) {
            bakedGraphRealEstate = Rectangle.ofSize(getWidth(), getHeight());
            axisRendererBaked = null;
        }
        else {
            axisRendererBaked = axisRenderer.getBaked(getWidth(), getHeight(), bakedDataBounds);
            bakedGraphRealEstate = axisRendererBaked.getInnerGraph();
        }

        BakingContext bakingContext = new BakingContext(bakedDataBounds, bakedGraphRealEstate.getWidth(), bakedGraphRealEstate.getHeight());

        transfer(dataCollections, c -> c.getBaked(bakingContext), bakedPlots);
        transfer(renderers, c -> c.getBaked(bakingContext), bakedRenderers);
    }

    private <T, V> void transfer(List<T> data, Function<T, V> operation, List<V> destination) {
        for (int i = 0; i < data.size(); i++) {
            T plot = data.get(i);
            V baked = operation.apply(plot);
            if(destination.size() <= i) {
                destination.add(baked);
            }
            else {
                destination.set(i, baked);
            }
        }
        if(destination.size() < data.size()) {
            destination.subList(data.size(), destination.size()).clear();
        }
    }

    private void renderManaged(GuiGraphics context) {
        var matrices = context.pose();
        matrices.pushPose();
        matrices.translate(x.get(), y.get(), 0);

        renderChart(context);

        matrices.popPose();
    }

    private void renderChart(GuiGraphics context) {
        /* if(imageMin == imageMax) {
            imageMin = imageMin - 5;
            imageMax = imageMax + 5;
        }
        else {
            float v = imageMax - imageMin;
            if(imageMin > 0) {
                imageMin = Math.max(0, imageMin - (float) (v * 0.1));
            }
            else {
                imageMin -= (float) (v * 0.1);
            }

            imageMax += (float) (v * 0.1);
        } */

        if(bakedGraphRealEstate == null) return;

        var matrices = context.pose();
        matrices.pushPose();

        if(axisRendererBaked != null) {
            axisRendererBaked.renderAxis(context);
            matrices.translate(bakedGraphRealEstate.getX(), bakedGraphRealEstate.getY(), 0);
        }

        for (InGraphRenderer bakedPlot : bakedPlots) {
            bakedPlot.render(context);
        }

        if(axisRendererBaked != null) {
            axisRendererBaked.render(context);
        }

        for (InGraphRenderer renderer : bakedRenderers) {
            renderer.render(context);
        }

        matrices.popPose();
    }

    @Override
    public int computeOptimalWidth() {
        return 0;
    }

    @Override
    public int computeOptimalHeight(int width) {
        return 0;
    }

    @Override
    protected void defaultMouseDownBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {
        if(bakedGraphRealEstate == null) return;
        if(event.button == GLFW.GLFW_MOUSE_BUTTON_1) {
            for (InGraphRenderer bakedPlot : bakedPlots) {
                if (bakedPlot.handleMouseDown(event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY(), event.button)) {
                    return;
                }
            }
            if(axisRendererBaked != null && axisRendererBaked.handleMouseDown(event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY(), event.button)) {
                return;
            }
            for (InGraphRenderer bakedPlot : bakedRenderers) {
                if (bakedPlot.handleMouseDown(event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY(), event.button)) {
                    return;
                }
            }
        }
    }

    @Override
    protected void defaultToolTipBehaviour(DefaultCancellableEvent.ToolTipEvent event) {
        if(bakedGraphRealEstate == null) return;
        tooltipDisposer = event::invalidate;
        for (InGraphRenderer bakedPlot : bakedPlots) {
            if (bakedPlot.doTooltip(event, event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY())) {
                return;
            }
        }
        if(axisRendererBaked != null && axisRendererBaked.doTooltip(event, event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY())) {
            return;
        }
        for (InGraphRenderer bakedPlot : bakedRenderers) {
            if (bakedPlot.doTooltip(event, event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY())) {
                return;
            }
        }
    }

    @Override
    protected void defaultMouseMoveBehaviour(DefaultCancellableEvent.MousePositionEvent event) {
        invalidateTooltip();
    }
}
