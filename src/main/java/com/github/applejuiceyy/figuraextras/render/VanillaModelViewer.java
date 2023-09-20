package com.github.applejuiceyy.figuraextras.render;

import com.github.applejuiceyy.figuraextras.ducks.RemembersNameAccess;
import net.minecraft.client.model.geom.ModelPart;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelPart;
import org.figuramc.figura.model.ParentType;

public class VanillaModelViewer extends VanillaModelPart {
    ModelPart modelPart;

    public VanillaModelViewer(Avatar owner, ModelPart modelPart) {
        super(owner, ((RemembersNameAccess) (Object) modelPart).figuraExtrass$getName(), ParentType.Cape, entityModel -> modelPart);
        this.modelPart = modelPart;
    }
}
