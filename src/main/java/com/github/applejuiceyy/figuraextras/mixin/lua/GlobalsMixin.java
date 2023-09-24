package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import org.luaj.vm2.Globals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Globals.class, remap = false)
public class GlobalsMixin implements GlobalsAccess {
    @Unique
    ActiveOpportunity<?> currentlyLookingForCapture;
    @Unique
    SecondaryCallHook currentlyCapturing;

    @Override
    public ActiveOpportunity<?> figuraExtrass$getCurrentlySearchingForCapture() {
        return currentlyLookingForCapture;
    }

    @Override
    public void figuraExtrass$setCurrentlySearchingForCapture(ActiveOpportunity<?> captureOpportunity) {
        currentlyLookingForCapture = captureOpportunity;
    }

    public SecondaryCallHook figuraExtrass$getCurrentCapture() {
        return currentlyCapturing;
    }

    public void figuraExtrass$setCurrentCapture(SecondaryCallHook hook) {
        currentlyCapturing = hook;
    }
}
