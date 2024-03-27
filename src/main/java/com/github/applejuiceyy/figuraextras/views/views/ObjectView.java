package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.trees.ObjectTreeView;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import org.luaj.vm2.LuaValue;

public class ObjectView extends ObjectTreeView<LuaValue> {
    public ObjectView(InfoViews.Context context, ParentElement.AdditionPoint additionPoint) {
        super(context, additionPoint);
    }

    @Override
    protected Expander<LuaValue> getRootExpander() {
        return ((AvatarAccess) context.getAvatar()).figuraExtrass$getObjectViewTree();
    }
}
