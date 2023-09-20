package com.github.applejuiceyy.figuraextras.mixin.render;


import com.github.applejuiceyy.figuraextras.ducks.RemembersNameAccess;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelPart.class)
public class ModelPartMixin implements RemembersNameAccess {
    @Unique
    String rememberedName;

    @Override
    public void figuraExtrass$setName(String name) {
        rememberedName = name;
    }

    @Override
    public String figuraExtrass$getName() {
        return rememberedName;
    }
}
