package com.github.applejuiceyy.figuraextras.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class InstructionChartComponent extends BaseComponent {
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
    }

    public void dispose() {
    }


    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        var matrices = context.pose();
        matrices.pushPose();
        matrices.translate(x, y, 0);

        context.fill(0, 0, width, height, 0xff000000);

        if (!dataPoints.isEmpty()) {
            Matrix4f matrix4f = context.pose().last().pose();
            RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            fallOff += 0.1;
            maxInstructionsSeen -= (int) fallOff;

            long currentDataPoints = System.currentTimeMillis() - lastEntry;
            for (int i = dataPoints.size() - 1; i >= 1; i--) {
                DataPoint dataPoint = dataPoints.get(i);
                DataPoint next = dataPoints.get(i - 1);

                float shortEnd = width - (currentDataPoints / (float) cutoff) * width;
                float bigEnd = width - ((currentDataPoints + dataPoint.afterMillis) / (float) cutoff) * width;
                float shortHeight = height - (dataPoint.instructions / (float) maxInstructionsSeen) * height;
                float bigHeight = height - (next.instructions / (float) maxInstructionsSeen) * height;

                currentDataPoints += dataPoint.afterMillis;

                if (maxInstructionsSeen < (dataPoint.instructions * 1.4)) {
                    fallOff = 0;
                    maxInstructionsSeen = (int) (dataPoint.instructions * 1.4);
                }

                bufferBuilder.vertex(matrix4f, bigEnd, bigHeight, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(matrix4f, bigEnd, height, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd, height, 0).color(0xff00ff00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd, shortHeight, 0).color(0xff00ff00).endVertex();

                bufferBuilder.vertex(matrix4f, shortEnd - 0.5f, shortHeight, 0).color(0xff00aa00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd - 0.5f, height, 0).color(0xff00aa00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd + 0.5f, height, 0).color(0xff00aa00).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd + 0.5f, shortHeight, 0).color(0xff00aa00).endVertex();

                bufferBuilder.vertex(matrix4f, shortEnd, shortHeight - 1, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd - 1, shortHeight, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd, shortHeight + 1, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(matrix4f, shortEnd + 1, shortHeight, 0).color(0xffff0000).endVertex();
            }

            int p = (maxInstructionsSeen / 1000) * 100 + 100;

            for (int i = 0; i < maxInstructionsSeen; i += p) {
                float h = height - (i / (float) maxInstructionsSeen) * height;

                bufferBuilder.vertex(matrix4f, 0, h, 0).color(0xff888888).endVertex();
                bufferBuilder.vertex(matrix4f, 0, h + 0.5f, 0).color(0xff888888).endVertex();
                bufferBuilder.vertex(matrix4f, width, h + 0.5f, 0).color(0xff888888).endVertex();
                bufferBuilder.vertex(matrix4f, width, h, 0).color(0xff888888).endVertex();
            }

            BufferUploader.drawWithShader(bufferBuilder.end());

            for (int i = 0; i < maxInstructionsSeen; i += p) {
                float h = height - (i / (float) maxInstructionsSeen) * height;

                context.drawText(Component.literal(String.valueOf(i)), 3, h + 2, 1, 0xff777777);
            }
        }

        context.fill(0, 0, 1, height, 0xffffffff);
        context.fill(0, height - 1, width, height, 0xffffffff);

        matrices.popPose();
    }

    record DataPoint(long afterMillis, int instructions) {
    }
}
