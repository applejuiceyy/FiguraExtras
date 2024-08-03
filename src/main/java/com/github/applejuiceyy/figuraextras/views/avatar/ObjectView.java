package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.trees.ObjectTreeView;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.views.View;
import org.figuramc.figura.avatar.Avatar;
import org.luaj.vm2.LuaValue;

public class ObjectView extends ObjectTreeView<LuaValue> {
    public ObjectView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        super(context, additionPoint);
    }

    @Override
    protected Expander<LuaValue> getRootExpander() {
        return ((AvatarAccess) context.getValue()).figuraExtrass$getObjectViewTree();
    }
}
