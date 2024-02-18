package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ducks.AvatarListAccess;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AvatarList.class, remap = false)
public class AvatarListMixin implements AvatarListAccess {
    @Unique
    SearchBar searchBar;

    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0),
            index = 0
    )
    Object e(Object e) {
        searchBar = (SearchBar) e;
        return e;
    }

    @Override
    public SearchBar figuraExtrass$getSearchBar() {
        return searchBar;
    }
}
