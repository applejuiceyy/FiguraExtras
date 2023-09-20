package com.github.applejuiceyy.figuraextras.mixin.render;

import com.github.applejuiceyy.figuraextras.ducks.RemembersNameAccess;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PartDefinition.class)
public class PartDefinitionMixin implements RemembersNameAccess {
    @Unique
    String rememberedName;

    @Inject(
            method = "addOrReplaceChild",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    void a(String name, CubeListBuilder builder, PartPose rotationData, CallbackInfoReturnable<PartDefinition> cir, PartDefinition partDefinition) {
        ((RemembersNameAccess) partDefinition).figuraExtrass$setName(name);
    }

    @Inject(
            method = "bake",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;setInitialPose(Lnet/minecraft/client/model/geom/PartPose;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    void b(int textureWidth, int textureHeight, CallbackInfoReturnable<ModelPart> cir, Object2ObjectArrayMap<String, ModelPart> object2ObjectArrayMap, List<ModelPart.Cube> list, ModelPart modelPart) {
        if (rememberedName != null) {
            ((RemembersNameAccess) (Object) modelPart).figuraExtrass$setName(rememberedName);
        }
    }

    @Override
    public void figuraExtrass$setName(String name) {
        rememberedName = name;
    }

    @Override
    public String figuraExtrass$getName() {
        return rememberedName;
    }
}
