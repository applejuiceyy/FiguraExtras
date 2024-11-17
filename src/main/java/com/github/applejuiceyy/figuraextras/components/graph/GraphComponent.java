package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.views.View;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GraphComponent extends Element {
    private final List<DataCollection> dataCollections = new ArrayList<>();
    private @Nullable Axis axisRenderer = Axis.createDefault();


    private final Map<DataCollection, Runnable> invalidators = new HashMap<>();
    private @Nullable Runnable axisRendererInvalidator;


    private @Nullable Float xMin, xMax, yMin, yMax;
    private @Nullable UnaryOperator<Bounds> boundsEffect;

    private boolean recalculateElements = true;


    private final List<DataCollection.BakedDataRendering> bakedPlots = new ArrayList<>();
    private @Nullable Axis.BakedAxisRenderer axisRendererBaked = null;

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
        float ax, ay, bx, by;
        ax = ay = Float.MAX_VALUE;
        bx = by = Float.MIN_VALUE;
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
        float xMin, yMin, xMax, yMax;

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
        invalidators.put(dataCollection, un);
        dataCollections.add(dataCollection);
        invalidate();
        return this;
    }

    public GraphComponent removeCollection(DataCollection collection) {
        invalidators.remove(collection).run();
        dataCollections.remove(collection);
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

        BakingContext bakingContext = new BakingContext(bakedDataBounds, bakedGraphRealEstate);

        for (int i = 0; i < dataCollections.size(); i++) {
            DataCollection plot = dataCollections.get(i);
            DataCollection.BakedDataRendering baked = plot.getBaked(bakingContext);
            if(bakedPlots.size() <= i) {
                bakedPlots.add(baked);
            }
            else {
                bakedPlots.set(i, baked);
            }
        }
        if(bakedPlots.size() < dataCollections.size()) {
            bakedPlots.subList(dataCollections.size(), bakedPlots.size()).clear();
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
            axisRendererBaked.render(context);
            matrices.translate(bakedGraphRealEstate.getX(), bakedGraphRealEstate.getY(), 0);
        }

        for (DataCollection.BakedDataRendering bakedPlot : bakedPlots) {
            bakedPlot.render(context);
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
        if(event.button == 2) {
            Elements.spawnContextMenu(getState(), event.x, event.y, (flow, deleter) -> {
                View.doOnContext("flamegraph", flow.adder(s -> {}), a -> {
                    ParentElement<?> googleEnPassant = View.forNewWindow(null, (v, c) -> View.error("Google en passant").apply(c));
                    googleEnPassant.activation.subscribe(ev -> deleter.run());
                    a.accept(googleEnPassant);
                });
            });
        }
        else if(event.button == 1) {
            for (DataCollection.BakedDataRendering bakedPlot : bakedPlots) {
                if (bakedPlot.handleMouseDown(event.x, event.y, event.button)) {
                    break;
                }
            }
        }
    }

    @Override
    protected void defaultToolTipBehaviour(DefaultCancellableEvent.ToolTipEvent event) {
        if(bakedGraphRealEstate == null) return;
        tooltipDisposer = event::invalidate;
        for (DataCollection.BakedDataRendering bakedPlot : bakedPlots) {
            if (bakedPlot.doTooltip(event, event.x - getX() - bakedGraphRealEstate.getX(), event.y - getY() - bakedGraphRealEstate.getY())) {
                break;
            }
        }
    }

    @Override
    protected void defaultMouseMoveBehaviour(DefaultCancellableEvent.MousePositionEvent event) {
        invalidateTooltip();
    }
}
