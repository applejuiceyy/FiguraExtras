package com.github.applejuiceyy.figuraextras.util;

import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.system.MemoryStack;

class SodiumAwareWireFrameVertexConsumer extends WireFrameVertexConsumer implements VertexBufferWriter {
    protected SodiumAwareWireFrameVertexConsumer(MultiBufferSource sources) {
        super(sources);
    }

    @Override
    public void push(MemoryStack memoryStack, long srcPosition, int vertexCount, VertexFormatDescription vertexFormatDescription) {
        if (!vertexFormatDescription.containsElement(CommonVertexAttribute.POSITION)) {
            return;
        }

        int relativePositionWithinVertex = vertexFormatDescription.getElementOffset(CommonVertexAttribute.POSITION);
        int sourceStride = vertexFormatDescription.stride();


        for (int vert = 0; vert < vertexCount; vert++) {
            float x = PositionAttribute.getX(((long) vert * sourceStride) + relativePositionWithinVertex + srcPosition);
            float y = PositionAttribute.getY(((long) vert * sourceStride) + relativePositionWithinVertex + srcPosition);
            float z = PositionAttribute.getZ(((long) vert * sourceStride) + relativePositionWithinVertex + srcPosition);
            vertex(x, y, z);
            endVertex();
        }
    }
}
