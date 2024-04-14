package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import com.github.applejuiceyy.figuraextras.util.Event;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Globals.class, remap = false)
public class GlobalsMixin implements GlobalsAccess {
    @Unique
    ActiveOpportunity<?> currentlyLookingForCapture;

    Event<SecondaryCallHook> hooks = Event.interfacing(SecondaryCallHook.class);

    @Unique
    LuaTable offTheShelfDebugLib;

    @Override
    public ActiveOpportunity<?> figuraExtrass$getCurrentlySearchingForCapture() {
        return currentlyLookingForCapture;
    }

    @Override
    public void figuraExtrass$setCurrentlySearchingForCapture(ActiveOpportunity<?> captureOpportunity) {
        currentlyLookingForCapture = captureOpportunity;
    }

    public @Nullable SecondaryCallHook figuraExtrass$getCurrentCapture() {
        return hooks.hasSubscribers() ? hooks.getSink() : null;
    }

    public Event<SecondaryCallHook>.Source figuraExtrass$getCaptureEventSource() {
        return hooks.getSource();
    }

    @Override
    public LuaTable figuraExtrass$getOffTheShelfDebugLib() {
        return offTheShelfDebugLib;
    }

    @Override
    public void figuraExtrass$setOffTheShelfDebugLib(LuaTable table) {
        offTheShelfDebugLib = table;
    }


}
