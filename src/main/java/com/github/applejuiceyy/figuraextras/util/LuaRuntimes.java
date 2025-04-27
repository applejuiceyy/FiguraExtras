package com.github.applejuiceyy.figuraextras.util;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.lua.MinecraftLuaBridge;
import com.github.applejuiceyy.figuraextras.lua.types.nbt.*;
import com.github.applejuiceyy.figuraextras.lua.types.resource.Resources;
import com.github.applejuiceyy.luabridge.Bridge;
import com.github.applejuiceyy.luabridge.DispatchGenerator;
import com.github.applejuiceyy.luabridge.LuaRuntime;
import com.github.applejuiceyy.luabridge.asm.ASMDispatchGenerator;
import com.github.applejuiceyy.luabridge.asm.plugins.BuiltinContextArgumentsPlugin;
import com.github.applejuiceyy.luabridge.limiting.DefaultInstructionLimiter;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.lua.api.math.MatricesAPI;
import org.figuramc.figura.lua.api.math.VectorsAPI;
import org.figuramc.figura.math.matrix.FiguraMat2;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class LuaRuntimes {
    public static DispatchGenerator defaultGenerator;

    static {
        ASMDispatchGenerator.ASMDispatchGeneratorBuilder asmDispatchGeneratorBuilder = ASMDispatchGenerator.create();
        asmDispatchGeneratorBuilder.addArgumentPlugin(new BuiltinContextArgumentsPlugin());
        if (FiguraExtras.DEBUG) {
            asmDispatchGeneratorBuilder.setCompileExportPath(FiguraExtras.getFiguraExtrasDirectory().resolve("compile_debug"));
        }
        defaultGenerator = asmDispatchGeneratorBuilder.build();
    }

    public static MinecraftLuaBridge buildDefaultBridge() {
        MinecraftLuaBridge built = Bridge.create(MinecraftLuaBridge::new)
                .generator(defaultGenerator)
                .addClass(TagWrap.class)
                .addClass(NumericTagWrap.class)
                .addClass(CompoundTagWrap.class)
                .addClass(CollectionTagWrap.class)
                .addClass(StringTagWrap.class)
                .addClass(TagUtils.class)
                .addClass(Resources.class)
                .addClass(IteratorWrap.class)
                .build();
        LuaTypeManager manager = new LuaTypeManager();
        manager.generateMetatableFor(FiguraVec2.class);
        manager.generateMetatableFor(FiguraVec3.class);
        manager.generateMetatableFor(FiguraVec4.class);
        manager.generateMetatableFor(FiguraMat2.class);
        manager.generateMetatableFor(FiguraMat3.class);
        manager.generateMetatableFor(FiguraMat4.class);
        manager.generateMetatableFor(VectorsAPI.class);
        manager.generateMetatableFor(MatricesAPI.class);
        built.setFiguraTypeManager(manager);
        return built;
    }

    public static void fillUtilities(LuaRuntime<?> runtime) {
        runtime.set("nbt", new TagUtils());
        runtime.set("vectors", VectorsAPI.INSTANCE);
        runtime.set("matrices", MatricesAPI.INSTANCE);
    }

    public static @NotNull LuaRuntime<MinecraftLuaBridge> buildDefaultRuntime(String printName, Path resolve) {
        LuaRuntime<MinecraftLuaBridge> luaRuntime = new LuaRuntime<>(
                buildDefaultBridge(),
                DefaultInstructionLimiter::new
        ) {
            @Override
            protected void printImplementation(Varargs text) {
                FiguraLuaPrinter.sendLuaMessage(
                        bridge.getPrintableValue(text), printName

                );
            }
        };

        luaRuntime.addSearcher(path -> {
            path = sanitizePath(path);
            return importPath(luaRuntime, resolve, path);
        });
        return luaRuntime;
    }

    public static String sanitizePath(String path) {
        path = path.trim();
        FileSystem fileSystem = FileSystems.getDefault();
        path = path
                .replace(".", fileSystem.getSeparator())
                .replace("\\", fileSystem.getSeparator())
                .replace("/", fileSystem.getSeparator());
        path = Pattern.compile(Pattern.quote(fileSystem.getSeparator() + "+")).matcher(path).replaceAll(fileSystem.getSeparator());
        return path;
    }

    public static LuaValue importPath(LuaRuntime<?> runtime, Path root, String path) throws LuaRuntime.SearchException {
        try (FileReader reader = new FileReader(root.resolve(path + ".lua").toFile())) {
            return runtime.getGlobals().load(reader, path);
        } catch (FileNotFoundException e) {
            throw new LuaRuntime.SearchException("File \"" + path + "\" not found");
        } catch (IOException e) {
            throw new LuaRuntime.SearchException("File \"" + path + "\" not able to be read: " + e.getMessage());
        }
    }
}
