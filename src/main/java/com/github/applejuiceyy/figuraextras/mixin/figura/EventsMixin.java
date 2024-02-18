package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ducks.EventsAPIAccess;
import org.figuramc.figura.lua.api.event.EventsAPI;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = EventsAPI.class, remap = false)
public class EventsMixin implements EventsAPIAccess {

    @Shadow
    @Final
    private Map<String, LuaEvent> events;
    @Unique
    public LuaEvent CHAT_AUTOCOMPLETE;

    @Inject(method = "<init>", at = @At("RETURN"))
    void a(CallbackInfo ci) {
        CHAT_AUTOCOMPLETE = new LuaEvent();
        events.put("CHAT_AUTOCOMPLETE", CHAT_AUTOCOMPLETE);
    }

    @Override
    public LuaEvent figuraExtras$getAutocompleteEvent() {
        return CHAT_AUTOCOMPLETE;
    }
}
