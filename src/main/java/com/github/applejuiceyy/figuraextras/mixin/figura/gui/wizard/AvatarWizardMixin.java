package com.github.applejuiceyy.figuraextras.mixin.figura.gui.wizard;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.constants.Paths;
import com.github.applejuiceyy.figuraextras.ducks.AvatarWizardAccess;
import com.github.applejuiceyy.figuraextras.services.AvatarPostProcessor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.wizards.AvatarWizard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;

@Mixin(AvatarWizard.class)
public abstract class AvatarWizardMixin implements AvatarWizardAccess {
    @Unique
    String[] postProcessor = new String[0];

    @Inject(method = "build", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getPlatform()Lnet/minecraft/Util$OS;"), cancellable = true)
    void postProcess(CallbackInfo ci, @Local(name = "folder") Path folder /* by name because index was making it select root instead */) {
        if (postProcessor.length == 0) return;
        long time = System.currentTimeMillis();

        ArrayList<ProcessBuilder> list = new ArrayList<>();

        for (String s : postProcessor) {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String sanitised = Paths.avatarPostProcessorsDirectory.resolve(s).toString();
            String command = Util.getPlatform() == Util.OS.WINDOWS ?
                    "cmd.exe /c \"\"" + sanitised + "\"\"" :
                    "/bin/sh -c \"" + sanitised.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            processBuilder.command(command.split(" "));
            processBuilder.directory(folder.toFile());
            processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            list.add(processBuilder);
        }

        AvatarPostProcessor.PostProcessingInstance postProcessingInstance;
        try {
            postProcessingInstance = AvatarPostProcessor.INSTANCE.addPostProcessing(folder, list.toArray(new ProcessBuilder[0]));
        } catch (IOException exc) {
            FiguraExtras.logger.error("Error while trying to spawn process: ", exc);
            return;
        }

        postProcessingInstance.onExit().handle((proc, throwable) -> {
            Minecraft.getInstance().execute(() -> {
                Throwable t = throwable;
                if (t != null) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    if (t instanceof AvatarPostProcessor.ProcessExitException th) {
                        FiguraToast.sendToast("Error", postProcessor[th.processIndex] + " failed with error code " + th.exitCode, FiguraToast.ToastType.ERROR);
                    } else {
                        FiguraExtras.logger.error("A catastrophic error happened while post-processing", t);
                        FiguraToast.sendToast("Catastrophic Error", "Check the console for more information");
                    }
                } else if (System.currentTimeMillis() > time + 2000) {
                    FiguraToast.sendToast("Finished post-processing");
                }

                Util.getPlatform().openUri(folder.toUri());
            });
            return null;
        });
        ci.cancel();
    }

    @Override
    public void figuraExtras$setPostProcessors(String[] postProcessor) {
        this.postProcessor = postProcessor;
    }

    @Override
    public String[] figuraExtras$getPostProcessors() {
        return postProcessor;
    }

}
