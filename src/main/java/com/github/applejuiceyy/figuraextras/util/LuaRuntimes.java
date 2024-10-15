package com.github.applejuiceyy.figuraextras.util;

import com.github.applejuiceyy.figuraextras.lua.MinecraftLuaBridge;
import com.github.applejuiceyy.figuraextras.lua.types.nbt.*;
import com.github.applejuiceyy.figuraextras.lua.types.resource.Resources;
import com.github.applejuiceyy.luabridge.Bridge;
import com.github.applejuiceyy.luabridge.DispatchGenerator;
import com.github.applejuiceyy.luabridge.LuaRuntime;
import com.github.applejuiceyy.luabridge.asm.ASMDispatchGenerator;
import com.github.applejuiceyy.luabridge.asm.plugins.BuiltinContextArgumentsPlugin;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.lua.api.math.MatricesAPI;
import org.figuramc.figura.lua.api.math.VectorsAPI;
import org.figuramc.figura.math.matrix.FiguraMat2;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;

public class LuaRuntimes {
    public static DispatchGenerator defaultGenerator =
            ASMDispatchGenerator.create()
                    .addArgumentPlugin(new BuiltinContextArgumentsPlugin())
                    .build();

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
}
