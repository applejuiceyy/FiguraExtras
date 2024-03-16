package com.github.applejuiceyy.figuraextras.tech.gui.stack;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class Stacks {
    public static WorkStackWO<RenderTarget, RenderTargetOptions> RENDER_TARGETS = new WorkStackWO<>() {

        final ArrayList<RenderTarget> availableRenderTargets = new ArrayList<>();

        @Override
        protected RenderTarget declaim(RenderTarget previous, @Nullable RenderTargetOptions options) {
            if (options == null) {
                options = new RenderTargetOptions(previous.width, previous.height);
            }

            for (RenderTarget availableRenderTarget : availableRenderTargets) {
                if (availableRenderTarget.width == options.width && availableRenderTarget.height == options.height) {
                    availableRenderTarget.clear(Minecraft.ON_OSX);
                    availableRenderTarget.bindWrite(true);
                    return availableRenderTarget;
                }
            }
            RenderTarget target;
            if (availableRenderTargets.size() > 5) {
                target = new TextureTarget(options.width, options.height, true, Minecraft.ON_OSX);
            } else {
                target = availableRenderTargets.remove(0);
                target.resize(options.width, options.height, Minecraft.ON_OSX);
            }

            return target;
        }

        @Override
        protected RenderTarget applyPush(RenderTarget previous, RenderTarget declaimed) {
            declaimed.bindWrite(true);
            return declaimed;
        }

        @Override
        protected void applyPop(RenderTarget newCurrent, RenderTarget previouslyCurrent) {
            newCurrent.bindWrite(true);
        }

        @Override
        public void reclaim(RenderTarget value) {
            availableRenderTargets.add(value);
        }

        @Override
        protected RenderTarget outsider() {
            return Minecraft.getInstance().getMainRenderTarget();
        }
    };

    private Stacks() {
    }

    public record RenderTargetOptions(int width, int height) {
    }
}
