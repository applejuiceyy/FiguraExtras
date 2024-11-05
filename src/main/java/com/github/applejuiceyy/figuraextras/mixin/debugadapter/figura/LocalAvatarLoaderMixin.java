package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.UserDataAccess;
import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.lua.MinecraftLuaBridge;
import com.github.applejuiceyy.figuraextras.lua.types.resource.Resources;
import com.github.applejuiceyy.figuraextras.util.LuaRuntimes;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.luabridge.LuaRuntime;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.parsers.LuaScriptParser;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;

@Mixin(value = LocalAvatarLoader.class, remap = false)
public class LocalAvatarLoaderMixin {
    @Shadow
    private static String loadError;

    @SuppressWarnings("UnresolvedMixinReference") // false
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/AvatarManager;loadLocalAvatar(Ljava/nio/file/Path;)V"), cancellable = true)
    static private void reloading(CallbackInfo ci) {
        if (DebugProtocolServer.getInternalInterface() != null) {
            DebugProtocolServer.getInternalInterface().avatarReloading();
            ci.cancel();
        }
    }

    @WrapOperation(method = "lambda$loadAvatar$2", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/local/LocalAvatarLoader;loadScripts(Ljava/nio/file/Path;Lnet/minecraft/nbt/CompoundTag;)V"))
    private static void disableMinifier(Path name, CompoundTag script, Operation<Void> original) {
        if (willPreprocess(name) && Configs.FORMAT_SCRIPT.value != 0) {
            Integer value = Configs.FORMAT_SCRIPT.value;
            Configs.FORMAT_SCRIPT.value = 0;
            try {
                original.call(name, script);
            } finally {
                Configs.FORMAT_SCRIPT.value = value;
            }
        } else {
            original.call(name, script);
        }
    }

    @Unique
    static private boolean willPreprocess(Path path) {
        Path resolve = path.resolve(".preprocess");

        return resolve.toFile().isDirectory() && resolve.resolve("main.lua").toFile().isFile();
    }

    @Inject(method = "lambda$loadAvatar$2", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/UserData;loadAvatar(Lnet/minecraft/nbt/CompoundTag;)V"), cancellable = true, remap = true)
    static private void mutateRead(Path finalPath, UserData target, CallbackInfo ci, @Local(ordinal = 0) LocalRef<CompoundTag> tag) {
        // hurt (doing this because LoadState is a private enum)
        try {
            // no need to setAccessible, everything is this class or a child
            // (I hope)
            Field loadState = LocalAvatarLoader.class.getDeclaredField("loadState");
            Class<?> cls = Class.forName("org.figuramc.figura.avatar.local.LocalAvatarLoader$LoadState");
            Field unknown = cls.getField("UNKNOWN");
            loadState.set(null, unknown.get(null));
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Path resolve = finalPath.resolve(".preprocess");

        CompoundTag hostCompoundTag = tag.get();
        CompoundTag guestCompoundTag = null;

        CompoundTag hostFiguraExtras = new CompoundTag();
        CompoundTag guestFiguraExtras = new CompoundTag();

        if (willPreprocess(finalPath)) {
            LuaRuntime<MinecraftLuaBridge> luaRuntime = LuaRuntimes.buildDefaultRuntime(
                    Minecraft.getInstance().getUser().getName() + "-preprocessor",
                    resolve
            );
            luaRuntime.addSearcher(path -> {
                path = LuaRuntimes.sanitizePath(path);
                String prefix = "@avatar" + FileSystems.getDefault().getSeparator();
                if (!path.startsWith(prefix)) {
                    throw new LuaRuntime.SearchException("Not relevant");
                }
                path = path.substring(prefix.length());
                return LuaRuntimes.importPath(luaRuntime, finalPath, path);
            });

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

                loadError = "Preprocessing error: " + err.getMessage();
                ci.cancel();
                return;
            }

            tag.set(nbts[0]);
            hostCompoundTag = nbts[0];

            if (!nbts[0].equals(nbts[1])) {
                guestCompoundTag = nbts[1];
                performDelayedFormatter(guestCompoundTag);
            }
            performDelayedFormatter(hostCompoundTag);
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
                loadError = "An error happened while processing host-splitting: " + e.getMessage();
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
    static private void performDelayedFormatter(CompoundTag nbt) {
        if (Configs.FORMAT_SCRIPT.value == 0) {
            return;
        }

        if (!nbt.contains("scripts")) {
            return;
        }

        Tag tag = nbt.get("scripts");

        if (!(tag instanceof CompoundTag ct)) {
            return;
        }

        for (String key : ct.getAllKeys()) {
            Tag script = ct.get(key);

            if (!(script instanceof ByteArrayTag st)) {
                continue;
            }

            ct.put(key, LuaScriptParser.parseScript(key, new String(st.getAsByteArray(), StandardCharsets.UTF_8)));
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
