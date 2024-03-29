package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ducks.FiguraTextureAccess;
import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.mixin.render.TextureManagerAccessor;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = FiguraTexture.class, remap = false)
public abstract class FiguraTextureMixin implements FiguraTextureAccess {
    @Shadow
    @Final
    private Avatar owner;


    @Shadow
    private boolean isClosed;

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract int getHeight();

    @Shadow
    @Final
    private NativeImage texture;
    @Unique
    boolean pendingModifications = false;
    @Unique
    int locks = 0;


    @Unique
    boolean realTimePendingModifications = false;
    @Unique
    boolean realTimeTextureIsRegistered = false;
    @Unique
    private FiguraIdentifier realTimeTextureIdentifier;
    @Unique
    private SimpleTexture realTimeTexture;

    @Unique
    private void generateUpdatedTexture() {
        realTimeTextureIdentifier = new FiguraIdentifier("avatar_tex/" + owner.owner + "/realtime/" + UUID.randomUUID());
        realTimeTexture = new SimpleTexture(realTimeTextureIdentifier) {
            @Override
            public void load(ResourceManager manager) {
            }
        };
        realTimePendingModifications = true;
        realTimeTextureIsRegistered = false;
    }

    @Unique
    private void ungenerateUpdatedTexture() {
        if (realTimeTexture != null) {
            realTimeTexture.close();
            realTimeTexture.releaseId();
            ((TextureManagerAccessor) Minecraft.getInstance().getTextureManager()).getByPath().remove(realTimeTextureIdentifier);
            realTimeTexture = null;
            realTimeTextureIdentifier = null;
        }
    }

    @Inject(method = "uploadIfDirty", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThreadOrInit()Z"))
    void e(CallbackInfo ci) {
        pendingModifications = false;
    }

    @Inject(method = "backupImage", at = @At(value = "HEAD"))
    void ee(CallbackInfo ci) {
        pendingModifications = true;
        realTimePendingModifications = true;
    }

    @Override
    public boolean figuraExtrass$hasRealTimePendingModifications() {
        return realTimePendingModifications;
    }

    @Override
    public boolean figuraExtrass$hasPendingModifications() {
        return pendingModifications;
    }


    @Override
    public void figuraExtrass$lockUpdatedTexture() {
        locks++;
        if (locks == 1) {
            generateUpdatedTexture();
        }
    }

    @Override
    public ResourceLocation figuraExtrass$getUpdatedTexture() {
        return realTimeTextureIdentifier;
    }

    @Override
    public void figuraExtrass$refreshUpdatedTexture() {
        if (realTimeTexture != null) {
            if (!realTimeTextureIsRegistered) {
                Minecraft.getInstance().getTextureManager().register(realTimeTextureIdentifier, realTimeTexture);
                realTimeTextureIsRegistered = true;
            }

            if (realTimePendingModifications && !isClosed) {
                realTimePendingModifications = false;

                RenderCall runnable = () -> {
                    // Upload texture to GPU.
                    TextureUtil.prepareImage(realTimeTexture.getId(), getWidth(), getHeight());
                    texture.upload(0, 0, 0, false);
                };

                if (RenderSystem.isOnRenderThreadOrInit()) {
                    runnable.execute();
                } else {
                    RenderSystem.recordRenderCall(runnable);
                }
            }
        }

        realTimePendingModifications = false;
    }

    @Override
    public void figuraExtrass$unlockUpdatedTexture() {
        locks--;
        if (locks == 0) {
            ungenerateUpdatedTexture();
        }
    }
}
