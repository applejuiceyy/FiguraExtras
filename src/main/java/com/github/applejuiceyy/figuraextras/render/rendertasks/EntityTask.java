package com.github.applejuiceyy.figuraextras.render.rendertasks;

import com.github.applejuiceyy.figuraextras.ducks.RemembersNameAccess;
import com.github.applejuiceyy.figuraextras.render.VanillaModelViewer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendertasks.RenderTask;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

@LuaWhitelist
public class EntityTask extends RenderTask {
    public static EntityTask currentTask;
    private static final HashMap<Class<?>, ArrayList<Field>> cache = new HashMap<>();
    private ArrayList<VanillaModelViewer> currentlyRenderedModels;

    Entity entity;
    LuaFunction preRender;
    boolean failedLoadingEntity;
    CompoundTag nbt;

    public EntityTask(String name, Avatar owner, FiguraModelPart parent) {
        super(name, owner, parent);
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        stack.scale(16, 16, 16);

        updateEntity();

        if (!failedLoadingEntity) {
            currentTask = this;

            try {
                Minecraft.getInstance().getEntityRenderDispatcher()
                        .render(entity, 0.0, 0.0, 0.0, 0.0F, 0, stack, buffer, light);
            } finally {
                currentTask = null;
            }
        }
    }


    @Override
    public int getComplexity() {
        return 0;
    }

    private void purgeEntity() {
        entity = null;
        failedLoadingEntity = false;
    }

    private void updateEntity() {
        if (entity == null && !failedLoadingEntity) {
            entity = EntityType.loadEntityRecursive(nbt, owner.luaRuntime.getUser().level(), Function.identity());
            if (entity == null) {
                failedLoadingEntity = true;
            }
        }
    }

    public void executePreRender(Model model) {
        if (preRender != null && owner.luaRuntime != null) {
            LuaTable table = new LuaTable();
            ArrayList<VanillaModelViewer> models = new ArrayList<>();
            Class<?> cls = model.getClass();

            if (cache.containsKey(cls)) {
                for (Field field : cache.get(cls)) {
                    field.setAccessible(true);
                    ModelPart child;
                    try {
                        child = (ModelPart) field.get(model);
                    } catch (IllegalAccessException ignored) {
                        continue;
                    }
                    VanillaModelViewer viewer = new VanillaModelViewer(owner, child);
                    models.add(viewer);
                    table.set(
                            ((RemembersNameAccess) (Object) child).figuraExtrass$getName(),
                            owner.luaRuntime.typeManager.javaToLua(viewer).arg1()
                    );
                }
            } else {
                ArrayList<Field> cachedFields = new ArrayList<>();

                for (Field field : cls.getFields()) {
                    if (ModelPart.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        ModelPart child;
                        try {
                            child = (ModelPart) field.get(model);
                        } catch (IllegalAccessException ignored) {
                            continue;
                        }
                        String rememberedName = ((RemembersNameAccess) (Object) child).figuraExtrass$getName();

                        if (rememberedName != null) {
                            cachedFields.add(field);
                            VanillaModelViewer viewer = new VanillaModelViewer(owner, child);
                            models.add(viewer);
                            table.set(
                                    rememberedName,
                                    owner.luaRuntime.typeManager.javaToLua(viewer).arg1()
                            );
                        }
                    }
                }

                cache.put(cls, cachedFields);
            }

            owner.run(preRender, owner.render, table);

            for (VanillaModelViewer vanillaModelViewer : models) {
                vanillaModelViewer.save(null);
                vanillaModelViewer.preTransform(null);
                vanillaModelViewer.posTransform(null);
            }

            currentlyRenderedModels = models;
        }
    }

    public void executePostRender(Model model) {
        if (currentlyRenderedModels != null) {
            for (VanillaModelViewer vanillaModelViewer : currentlyRenderedModels) {
                vanillaModelViewer.restore(null);
            }
            currentlyRenderedModels = null;
        }
    }


    @LuaWhitelist
    public void setNbt(String nbt) {
        try {
            CompoundTag b = (new TagParser(new StringReader(nbt))).readStruct();
            if (!b.contains("id", CompoundTag.TAG_STRING)) {
                throw new LuaError("Entity nbt must contain id");
            }
            this.nbt = b;
            purgeEntity();
        } catch (CommandSyntaxException e) {
            throw new LuaError(e);
        }
    }


    @LuaWhitelist
    public Object __index(String key) {
        if (key.equals("preRender")) {
            return preRender;
        }
        return null;
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, LuaFunction value) {
        if (key.equals("preRender")) {
            preRender = value;
        } else {
            throw new LuaError("Cannot assign value on key \"" + key + "\"");
        }
    }
}
