package com.github.applejuiceyy.figuraextras.mixin.render;


import com.github.applejuiceyy.figuraextras.ducks.RenderTargetAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.stack.Stacks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public class RenderTargetMixin implements RenderTargetAccess {

    @Shadow
    public int frameBufferId;

    @Unique
    int stencilId = -1;

    @Override
    public void figuraExtrass$setStencil(int texture) {
        if (texture == stencilId) return;
        stencilId = texture;
        if (frameBufferId >= 0) {
            Stacks.RENDER_TARGETS.push((RenderTarget) (Object) this);
            updateStencil();
            Stacks.RENDER_TARGETS.pop(false);
        }
    }

    @Unique
    void updateStencil() {
        GlStateManager._glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_STENCIL_ATTACHMENT,
                GL30.GL_TEXTURE_2D,
                stencilId,
                0
        );
    }

    @Inject(
            method = "createBuffers",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;checkStatus()V")
    )
    void e(int width, int height, boolean getError, CallbackInfo ci) {
        if (stencilId > 0) {
            updateStencil();
        }
    }
}
