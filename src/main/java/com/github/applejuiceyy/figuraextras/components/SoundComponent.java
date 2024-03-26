package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

public class SoundComponent extends Element {
    private final ByteBuffer buffer;
    private final AudioFormat format;
    public int sampleOffset = 0;
    public int sampleEnding = 0;

    public SoundComponent(ByteBuffer buffer, AudioFormat format) {
        this.buffer = buffer;
        this.format = format;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int current = x.get() + width.get();

        Matrix4f matrix4f = context.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int currentSample = sampleOffset; currentSample >= 11; currentSample -= 10) {
            ShortShortTuple sample = ShortShortTuple.ZERO;
            ShortShortTuple previous = ShortShortTuple.ZERO;
            if (currentSample <= sampleEnding) {
                sample = fetchValues(currentSample);
            }
            if (currentSample <= sampleEnding) {
                previous = fetchValues(currentSample - 10);
            }
            float nSample = Mth.map(sample.first, Short.MIN_VALUE, Short.MAX_VALUE, y.get(), y.get() + height.get());
            float npSample = Mth.map(previous.first, Short.MIN_VALUE, Short.MAX_VALUE, y.get(), y.get() + height.get());
            float diff = Math.abs(nSample - npSample) / 10;
            bufferBuilder.vertex(matrix4f, current - 1, npSample - 1, 0).color(0xff00ff00).endVertex();
            bufferBuilder.vertex(matrix4f, current - 1, npSample + 1, 0).color(0xff00ff00).endVertex();
            bufferBuilder.vertex(matrix4f, current, nSample + 1, 0).color(0xff00ff00).endVertex();
            bufferBuilder.vertex(matrix4f, current, nSample - 1, 0).color(0xff00ff00).endVertex();
            current -= 1;
            if (current <= x.get()) {
                break;
            }
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    @Override
    public int computeOptimalWidth() {
        return 0;
    }

    @Override
    public int computeOptimalHeight(int width) {
        return 50;
    }

    private int getOffset(int sampleIndex) {
        // interlaced mode has the samples have the left and right channel next to eachother
        // effectively taking double the space
        sampleIndex *= format.getChannels();
        // if it's 16 bits, the *actual* number will occupy 2 8-bit addresses in the sample
        //noinspection SuspiciousIntegerDivAssignment
        sampleIndex *= format.getSampleSizeInBits() / 8;
        return sampleIndex;
    }

    public int sampleCount() {
        return buffer.remaining() * 8 / format.getSampleSizeInBits() / format.getChannels();
    }

    private ShortShortTuple fetchValues(int sampleNumber) {
        if (sampleNumber < 0) {
            return ShortShortTuple.ZERO;
        }
        if (sampleNumber >= sampleCount()) {
            return ShortShortTuple.ZERO;
        }

        int offset = getOffset(sampleNumber);
        if (format.getChannels() == 1) {
            if (format.getSampleSizeInBits() == 8) {
                byte b = buffer.get(offset);
                return new ShortShortTuple(b, b);
            } else {
                byte high = buffer.get(format.isBigEndian() ? offset : offset + 1);
                byte low = buffer.get(format.isBigEndian() ? offset + 1 : offset);
                short pack = (short) ((high << 8) + low);
                return new ShortShortTuple(pack, pack);
            }
        }

        if (format.getSampleSizeInBits() == 8) {
            byte left = buffer.get(offset);
            byte right = buffer.get(offset + 1);
            return new ShortShortTuple(left, right);
        } else {
            byte highleft = buffer.get(format.isBigEndian() ? offset : offset + 1);
            byte lowleft = buffer.get(format.isBigEndian() ? offset + 1 : offset);
            byte highright = buffer.get(format.isBigEndian() ? offset + 2 : offset + 3);
            byte lowright = buffer.get(format.isBigEndian() ? offset + 3 : offset + 2);
            short packleft = (short) ((highleft << 8) + lowleft);
            short packright = (short) ((highright << 8) + lowright);
            return new ShortShortTuple(packleft, packright);
        }
    }


    record ShortShortTuple(short first, short second) {
        public static final ShortShortTuple ZERO = new ShortShortTuple((short) 0, (short) 0);
    }
}
