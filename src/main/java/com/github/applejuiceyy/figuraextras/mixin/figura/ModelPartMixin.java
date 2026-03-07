package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.util.WireFrameVertexConsumer;
import com.github.applejuiceyy.figuraextras.views.Hover;
import net.minecraft.client.renderer.RenderType;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.figuramc.figura.model.rendering.Vertex;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(value = FiguraModelPart.class, remap = false)
public abstract class ModelPartMixin {

    @Shadow
    public List<Integer> facesByTexture;

    @Shadow
    @Final
    public Map<Integer, List<Vertex>> vertices;

    @Shadow
    public List<FiguraTextureSet> textures;

    @Shadow
    public abstract List<FiguraTexture> getTextures();

    @Inject(
            method = "pushVerticesImmediate",
            at = @At("RETURN")
    )
    void renderGhost(ImmediateAvatarRenderer avatarRenderer, int[] remainingComplexity, CallbackInfoReturnable<Boolean> cir) {
        if (Hover.currentHover.get() == null || !(Hover.currentHover.get() instanceof FiguraModelPart)) {
            return;
        }

        FiguraModelPart current = (FiguraModelPart) (Object) this;

        while (Hover.currentHover.get() != current) {
            current = current.parent;

            if (current == null) {
                return;
            }
        }

        FiguraVec4 pos = FiguraVec4.of();
        WireFrameVertexConsumer vertexConsumer = WireFrameVertexConsumer.of(avatarRenderer.bufferSource);
        vertexConsumer.getBuffer(RenderType.endPortal());

        for (int i = 0; i < facesByTexture.size(); i++) {
            PartCustomization.PartCustomizationStack stack = ((ImmediateAvatarRendererAccessor) avatarRenderer).getCustomizationStack();

            List<Vertex> vertexList = vertices.get(i);
            if (facesByTexture.get(i) == 0) {
                continue;
            }

            for (Vertex vertex : vertexList) {
                pos.set(vertex.x, vertex.y, vertex.z, 1);
                pos.transform(stack.peek().positionMatrix);

                vertexConsumer
                    .vertex(pos.x, pos.y, pos.z)
                    .endVertex();
            }
        }
    }
}
