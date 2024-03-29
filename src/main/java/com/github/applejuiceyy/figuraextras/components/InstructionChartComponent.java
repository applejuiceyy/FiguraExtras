package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class InstructionChartComponent extends Element {
    long cutoff = 10 * 1000;
    ArrayList<DataPoint> dataPoints = new ArrayList<>();
    long accumulatedTime;
    long lastEntry;

    int maxInstructionsSeen = 0;
    float fallOff = 0;

    public InstructionChartComponent() {
        lastEntry = System.currentTimeMillis();
        accumulatedTime = 0;
    }

    public void consumeEntry(int data) {
        long millis = System.currentTimeMillis();
        long diff = millis - lastEntry;
        lastEntry = millis;
        accumulatedTime += diff;
        dataPoints.add(new DataPoint(diff, data));
        if (dataPoints.size() >= 2) {
            while ((accumulatedTime - dataPoints.get(0).afterMillis - dataPoints.get(1).afterMillis) > cutoff) {
                accumulatedTime -= dataPoints.remove(0).afterMillis;
            }
        }
        enqueueDirtySection(false, false);
    }

    public void dispose() {
    }


    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var matrices = context.pose();
        matrices.pushPose();
        matrices.translate(x.get(), y.get(), 0);

        context.fill(0, 0, width.get(), height.get(), 0xff000000);

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

    record DataPoint(long afterMillis, int instructions) {
    }
}
