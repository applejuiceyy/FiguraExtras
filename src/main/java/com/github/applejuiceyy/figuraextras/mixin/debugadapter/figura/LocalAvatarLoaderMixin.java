package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.lua.MinecraftLuaBridge;
import com.github.applejuiceyy.figuraextras.lua.types.resource.Resources;
import com.github.applejuiceyy.figuraextras.util.LuaRuntimes;
import com.github.applejuiceyy.luabridge.LuaRuntime;
import com.github.applejuiceyy.luabridge.limiting.DefaultInstructionLimiter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.regex.Pattern;

@Mixin(value = LocalAvatarLoader.class, remap = false)
public class LocalAvatarLoaderMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/AvatarManager;loadLocalAvatar(Ljava/nio/file/Path;)V"), cancellable = true)
    static private void reloading(CallbackInfo ci) {
        if (DebugProtocolServer.getInternalInterface() != null) {
            DebugProtocolServer.getInternalInterface().avatarReloading();
            ci.cancel();
        }
    }

    @Inject(method = "lambda$loadAvatar$2", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/UserData;loadAvatar(Lnet/minecraft/nbt/CompoundTag;)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    static private void mutateRead(Path finalPath, UserData target, CallbackInfo ci, CompoundTag tag) {
        Path resolve = finalPath.resolve(".preprocess");
        if (resolve.toFile().isDirectory() && resolve.resolve("main.lua").toFile().isFile()) {
            LuaRuntime<MinecraftLuaBridge> luaRuntime = new LuaRuntime<>(
                    LuaRuntimes.buildDefaultBridge(),
                    DefaultInstructionLimiter::new
            ) {
                @Override
                protected void printImplementation(Varargs text) {
                    FiguraLuaPrinter.sendLuaMessage(
                            bridge.getPrintableValue(text),
                            Minecraft.getInstance().getUser().getName() + "-preprocessor"
                    );
                }

                @Override
                protected String sanitizeRequirePath(String path) {
                    path = path.trim();
                    FileSystem fileSystem = FileSystems.getDefault();
                    path = path
                            .replace(".", fileSystem.getSeparator())
                            .replace("\\", fileSystem.getSeparator())
                            .replace("/", fileSystem.getSeparator());
                    path = Pattern.compile(Pattern.quote(fileSystem.getSeparator() + "+")).matcher(path).replaceAll(fileSystem.getSeparator());
                    return path + ".lua";
                }

                @Override
                protected LuaValue requireImplementation(String path) {
                    LuaValue load;
                    try (FileReader reader = new FileReader(resolve.resolve(path).toFile())) {
                        load = getGlobals().load(reader, path);
                    } catch (FileNotFoundException e) {
                        throw new LuaError("File \"" + path + "\" not found");
                    } catch (IOException e) {
                        throw new LuaError("File \"" + path + "\" not able to be read: " + e.getMessage());
                    }
                    return load.call();
                }
            };

            LuaRuntimes.fillUtilities(luaRuntime);
            luaRuntime.set("resource", new Resources(finalPath));

            try {
                LuaValue main = luaRuntime.requireFile("main");
                luaRuntime.bridge.toJava(
                        luaRuntime.run(
                                () -> main.get("transform").call(luaRuntime.bridge.toLua(tag, false)),
                                FiguraExtras.prepInstructionCount.value
                        ).arg1(),
                        false
                );
            } catch (LuaError err) {
                FiguraExtras.sendBrandedMessage("Preprocessing Error", style -> style.withColor(ChatFormatting.RED), "An error has happened in preprocessing");
                Minecraft.getInstance().execute(() ->
                        FiguraMod.sendChatMessage(Component.literal(err.getMessage()).withStyle(ChatFormatting.RED))
                );
                ci.cancel();
            }

        }
    }
}
