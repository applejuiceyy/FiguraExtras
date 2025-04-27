package com.github.applejuiceyy.figuraextras.smallchange.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.Util;
import org.figuramc.figura.wizards.AvatarWizard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(value = AvatarWizard.class, remap = false)
public class AvatarWizardMixin {

    @ModifyArg(method = "build", at = @At(value = "INVOKE", target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"), index = 0)
    String allowFolders(String regex) {
        switch (Util.getPlatform()) {
            case LINUX, SOLARIS, OSX -> {
                return "";
            }
            case WINDOWS -> {
                // removed slashes
                return "CON|PRN|AUX|NUL|COM\\d|LPT\\d|[:*?\"<>|\u0000]|\\.$";
            }
            case UNKNOWN -> {
                return regex;
            }
        }
        return regex;
    }

    @Inject(method = "build", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/wizards/AvatarWizard;buildMetadata(Ljava/lang/String;)[B"))
    void doFolders(CallbackInfo ci, @Local(name = "root") Path root, @Local(name = "folder") Path folder) {
        if (!folder.startsWith(root)) {
            throw new IllegalStateException("Path exits avatar folder");
        }
        Path current = folder;
        do {
            if (!Files.exists(current)) {
                continue;
            }

            if (Files.isDirectory(current)) {
                if (Files.exists(current.resolve("avatar.json"))) {
                    throw new IllegalStateException("Cannot create inside of an avatar");
                }
            } else {
                throw new IllegalStateException("Theres a file in the path");
            }
        }
        while ((current = current.getParent()).startsWith(root));

        try {
            Files.createDirectories(folder.getParent());
        } catch (IOException ignored) {

        }
    }
}
