package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.render.rendertasks.EntityTask;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendertasks.RenderTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(FiguraModelPart.class)
public class ModelPartMixin {
    @Shadow
    @Final
    private Avatar owner;

    @Shadow
    public Map<String, RenderTask> renderTasks;

    @LuaWhitelist
    public EntityTask newEntity(@LuaNotNil String name) {
        EntityTask task = new EntityTask(name, owner, (FiguraModelPart) (Object) this);
        renderTasks.put(name, task);
        return task;
    }
}
