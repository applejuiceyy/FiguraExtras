package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.UserDataAccess;
import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.lua.MinecraftLuaBridge;
import com.github.applejuiceyy.figuraextras.lua.types.resource.Resources;
import com.github.applejuiceyy.figuraextras.util.LuaRuntimes;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.luabridge.LuaRuntime;
import com.github.applejuiceyy.luabridge.limiting.DefaultInstructionLimiter;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;
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

    @Inject(method = "lambda$loadAvatar$2", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/UserData;loadAvatar(Lnet/minecraft/nbt/CompoundTag;)V"), cancellable = true, remap = true)
    static private void mutateRead(Path finalPath, UserData target, CallbackInfo ci, @Local(ordinal = 0) LocalRef<CompoundTag> tag) {
        Path resolve = finalPath.resolve(".preprocess");

        CompoundTag hostCompoundTag = tag.get();
        CompoundTag guestCompoundTag = null;

        CompoundTag hostFiguraExtras = new CompoundTag();
        CompoundTag guestFiguraExtras = new CompoundTag();

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

            CompoundTag[] nbts = new CompoundTag[2];

            try {
                LuaValue main = luaRuntime.requireFile("main");

                CompoundTag finalCompoundTag = hostCompoundTag;
                luaRuntime.run(
                        () -> {
                            LuaValue hostTransform = main.get("hostTransform");
                            LuaValue transform = main.get("transform");
                            LuaValue guestTransform = main.get("guestTransform");
                            if (hostTransform.isnil() && guestTransform.isnil()) {
                                if (transform.isnil()) {
                                    throw new LuaError("returned table does not have a key for transform, guestTransform or hostTransform");
                                }
                                nbts[0] = nbts[1] = callOneOf(luaRuntime.bridge.toLua(finalCompoundTag, false), transform)
                                        .map(v -> sanitizeReturn(luaRuntime, v, "general"))
                                        .orElseThrow();
                            } else {
                                CompoundTag copied = finalCompoundTag.copy();
                                nbts[0] = callOneOf(luaRuntime.bridge.toLua(finalCompoundTag, false), hostTransform, transform)
                                        .map(v -> sanitizeReturn(luaRuntime, v, "host"))
                                        .orElse(finalCompoundTag);
                                nbts[1] = callOneOf(luaRuntime.bridge.toLua(copied, false), guestTransform, transform)
                                        .map(v -> sanitizeReturn(luaRuntime, v, "guest"))
                                        .orElse(copied);
                            }
                            return LuaValue.NIL;
                        },
                        FiguraExtras.prepInstructionCount.value
                ).arg1();
            } catch (LuaError err) {
                FiguraExtras.sendBrandedMessage("Preprocessing Error", style -> style.withColor(ChatFormatting.RED), "An error has happened in preprocessing");
                Minecraft.getInstance().execute(() ->
                        FiguraMod.sendChatMessage(Component.literal(err.getMessage()).withStyle(ChatFormatting.RED))
                );
                FiguraExtras.logger.error("Error while preprocessing", err);
                ci.cancel();
                return;
            }

            tag.set(nbts[0]);
            hostCompoundTag = nbts[0];

            if (!nbts[0].equals(nbts[1])) {
                guestCompoundTag = nbts[1];
            }
        }


        if (FiguraExtras.signAvatars.value > 0) {
            byte[] signature = FiguraExtras.avatarSigner.sign((guestCompoundTag == null ? hostCompoundTag : guestCompoundTag).getAsString().getBytes(StandardCharsets.UTF_8));
            (guestCompoundTag == null ? hostFiguraExtras : guestFiguraExtras).put("signature", new ByteArrayTag(signature));
        }
        ;

        if (guestCompoundTag != null) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
                NbtIo.writeCompressed(hostCompoundTag, outputStream);
                byte[] bytes = outputStream.toByteArray();
                guestFiguraExtras.put("host-counterpart", new ByteArrayTag(Util.hashBytes(bytes)));
            } catch (IOException e) {
                FiguraExtras.sendBrandedMessage("Host Splitting Error", style -> style.withColor(ChatFormatting.RED), "An error has happened while managing host splitting: " + e.getMessage());
                ci.cancel();
                return;
            }
            ((UserDataAccess) target).figuraExtrass$setFutureAvatarGuestNbt(guestCompoundTag);
        }

        if (!hostFiguraExtras.isEmpty()) {
            hostCompoundTag.put("figura-extras", hostFiguraExtras);
        }
        if (!guestFiguraExtras.isEmpty() && guestCompoundTag != null) {
            guestCompoundTag.put("figura-extras", guestFiguraExtras);
        }
    }

    @Unique
    static private Optional<Varargs> callOneOf(Varargs args, LuaValue... callee) {
        for (LuaValue luaValue : callee) {
            if (!luaValue.isnil()) {
                return Optional.of(luaValue.invoke(args));
            }
        }
        return Optional.empty();
    }

    @Unique
    static private CompoundTag sanitizeReturn(LuaRuntime<?> runtime, Varargs args, String side) {
        Object java = runtime.bridge.toJava(args.arg1(), false);
        if (!(java instanceof CompoundTag t)) {
            throw new LuaError("Return value of " + side + " transform is not a compound tag");
        }
        t.remove("figura-extras");
        return t;
    }
}
