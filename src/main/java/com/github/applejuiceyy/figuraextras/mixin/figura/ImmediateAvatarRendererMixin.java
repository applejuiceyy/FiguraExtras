package com.github.applejuiceyy.figuraextras.mixin.figura;


import com.github.applejuiceyy.figuraextras.views.Hover;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.figuramc.figura.model.rendertasks.RenderTask;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ImmediateAvatarRenderer.class, remap = false)
public class ImmediateAvatarRendererMixin {
    @Shadow
    @Final
    protected PartCustomization.PartCustomizationStack customizationStack;

    @Inject(method = "renderPart", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/model/rendertasks/RenderTask;render(Lorg/figuramc/figura/model/PartCustomization$PartCustomizationStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", shift = At.Shift.AFTER, remap = true))
    void renderGhost(FiguraModelPart part, int[] remainingComplexity, boolean prevPredicate, CallbackInfoReturnable<Boolean> cir, @Local(name = "task") RenderTask renderTask) {
        Object hover = Hover.currentHover.get();
        if (hover == null) {
            return;
        }

        if (hover instanceof RenderTask rt) {
            if (rt != renderTask) {
                return;
            }
        } else if (hover instanceof FiguraModelPart modelPart) {
            FiguraModelPart current = ((RenderTaskAccessor) renderTask).getParent();

            while (modelPart != current) {
                current = current.parent;

                if (current == null) {
                    return;
                }
            }
        } else {
            return;
        }

        VertexConsumer buffer = ((AvatarRenderer) (Object) this).bufferSource.getBuffer(RenderType.LINE_STRIP);

        VertexConsumer strippedDown = new VertexConsumer() {
            boolean acceptingPosition = true;

            @Override
            public @NotNull VertexConsumer vertex(double x, double y, double z) {
                if (!acceptingPosition) {
                    return this;
                }
                buffer.vertex(x, y, z);
                buffer.color(255, 255, 255, 255);
                buffer.normal(0, 0, 0);
                buffer.endVertex();
                acceptingPosition = false;
                return this;
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
        };

        MultiBufferSource onlyLines = renderType -> strippedDown;

        renderTask.render(customizationStack, onlyLines, 15, 0);
    }
}
