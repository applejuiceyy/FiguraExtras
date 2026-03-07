package com.github.applejuiceyy.figuraextras.mixin.figura;


import com.github.applejuiceyy.figuraextras.util.WireFrameVertexConsumer;
import com.github.applejuiceyy.figuraextras.views.Hover;
import com.llamalad7.mixinextras.sugar.Local;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.figuramc.figura.model.rendertasks.RenderTask;
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

        renderTask.render(
            customizationStack,
            WireFrameVertexConsumer.of(((AvatarRenderer) (Object) this).bufferSource),
            15,
            0
        );
    }

}
