package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.trees.ObjectTreeView;
import com.github.applejuiceyy.figuraextras.views.trees.core.Expander;
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
