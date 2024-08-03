package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.views.Hover;
import net.minecraft.client.renderer.LightTexture;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.figuramc.figura.model.rendering.Vertex;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
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

    @Inject(
            method = "pushVerticesImmediate",
            at = @At("RETURN")
    )
    void a(ImmediateAvatarRenderer avatarRenderer, int[] remainingComplexity, CallbackInfoReturnable<Boolean> cir) {
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

        for (int i = 0; i < facesByTexture.size(); i++) {
            PartCustomization.PartCustomizationStack stack = ((ImmediateAvatarRendererAccessor) avatarRenderer).getCustomizationStack();
            PartCustomization c = new PartCustomization();
            c.alpha = 1f;
            c.light = LightTexture.pack(15, 15);
            c.setPrimaryRenderType(RenderTypes.LINES);
            c.setSecondaryRenderType(RenderTypes.NONE);
            stack.push(c);
            avatarRenderer.pushFaces(facesByTexture.get(i), new int[1], textures.get(i), vertices.get(i));
            stack.pop();
        }
    }
}
