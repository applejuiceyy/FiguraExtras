package com.github.applejuiceyy.figuraextras.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.figuramc.figura.FiguraMod;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class WireFrameVertexConsumer implements VertexConsumer, MultiBufferSource {
    private static final MethodHandle creator;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType constructorType = MethodType.methodType(void.class, MultiBufferSource.class);

        find:
        {
            try {
                Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter");
            } catch (ClassNotFoundException e) {
                try {
                    creator = lookup.findConstructor(WireFrameVertexConsumer.class, constructorType);
                    break find;
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }

            try {
                creator = lookup.findConstructor(
                    lookup.findClass("com.github.applejuiceyy.figuraextras.util.SodiumAwareWireFrameVertexConsumer"),
                    constructorType
                );
            } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private final VertexConsumer buffer;
    private boolean acceptingPosition = false;

    private double[] backedUpPositions = null;
    private VertexFormat.Mode currentMode;
    private int activePos = 0;

    protected WireFrameVertexConsumer(MultiBufferSource sources) {
        this.buffer = sources.getBuffer(RenderType.LINES);
    }

    public static WireFrameVertexConsumer of(MultiBufferSource sources) {
        try {
            return (WireFrameVertexConsumer) creator.invoke(sources);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull VertexConsumer vertex(double x, double y, double z) {
        if (!acceptingPosition) {
            return this;
        }

        backedUpPositions[activePos * 3] = x;
        backedUpPositions[activePos * 3 + 1] = y;
        backedUpPositions[activePos * 3 + 2] = z;

        activePos += 1;

        if (activePos == currentMode.primitiveLength) {
            for (int i = 0; i < currentMode.primitiveLength - 1; i++) {
                emitVertex(backedUpPositions[i * 3], backedUpPositions[i * 3 + 1], backedUpPositions[i * 3 + 2]);
                int ii = i + 1;
                emitVertex(backedUpPositions[ii * 3], backedUpPositions[ii * 3 + 1], backedUpPositions[ii * 3 + 2]);

                if (i < currentMode.primitiveLength - currentMode.primitiveStride) {
                    int stridePos = i + currentMode.primitiveStride;
                    backedUpPositions[i * 3] = backedUpPositions[stridePos * 3];
                    backedUpPositions[i * 3 + 1] = backedUpPositions[stridePos * 3 + 1];
                    backedUpPositions[i * 3 + 2] = backedUpPositions[stridePos * 3 + 2];
                }
            }
            activePos -= currentMode.primitiveStride;
        }

        acceptingPosition = false;
        return this;
    }

    private void emitVertex(double x, double y, double z) {
        buffer.vertex(x, y, z);
        int o = FiguraMod.ticks % 20 < 10 ? 0 : 255;
        buffer.color(255, o, o, 255);
        buffer.normal(0, 0, 0);
        buffer.endVertex();
    }

    @Override
    public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer uv(float u, float v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    @Override
    public void endVertex() {
        acceptingPosition = true;
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {

    }

    @Override
    public void unsetDefaultColor() {

    }


    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        acceptingPosition = true;
        currentMode = renderType.mode();
        backedUpPositions = new double[currentMode.primitiveLength * 3];
        activePos = 0;
        return this;
    }

}
