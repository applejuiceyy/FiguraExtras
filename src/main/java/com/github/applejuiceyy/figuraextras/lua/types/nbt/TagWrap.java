package com.github.applejuiceyy.figuraextras.lua.types.nbt;

import com.github.applejuiceyy.figuraextras.ducks.TextComponentTagVisitorAccess;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import com.github.applejuiceyy.luabridge.annotation.LuaPrint;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;


@LuaClass(wraps = Tag.class)
public class TagWrap {
    @LuaMethod
    public static String getType(Tag tag) {
        return tag.getType().getName();
    }

    @LuaMethod
    public static byte getId(Tag tag) {
        return tag.getId();
    }

    @LuaMethod
    public static Tag copy(Tag tag) {
        return tag.copy();
    }

    @LuaPrint
    public static Component print(Tag tag, boolean expand) {
        TextComponentTagVisitor visitor = new TextComponentTagVisitor("    ", 0);
        ((TextComponentTagVisitorAccess) visitor).figuraExtrass$setElementLimit(10);
        return visitor.visit(tag);
    }
}
