package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.trees.ObjectTreeView;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import org.luaj.vm2.LuaValue;

public class ObjectView extends ObjectTreeView<LuaValue> {
    public ObjectView(InfoViews.Context context) {
        super(context);
    }


    @Override
    protected Expander<LuaValue> getRootExpander() {
        return ((AvatarAccess) context.getAvatar()).figuraExtrass$getObjectViewTree();
    }
}
