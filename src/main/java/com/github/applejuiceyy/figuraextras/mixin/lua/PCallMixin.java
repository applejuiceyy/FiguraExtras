package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
import org.luaj.vm2.Globals;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.BaseLib;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"org.luaj.vm2.lib.BaseLib$pcall", "org.luaj.vm2.lib.BaseLib$xpcall"}, remap = false)
public class PCallMixin {
    @Shadow(remap = false)
    @Final
    BaseLib this$0;

    @Inject(method = "invoke", at = @At("HEAD"))
    void entry(Varargs args, CallbackInfoReturnable<Varargs> cir) {
        Globals globals = ((BaseLibAccessor) this$0).getGlobals();
        GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
        Hook capture = globalsAccess.figuraExtrass$getCaptureState().getSink();
        if (capture != null) {
            capture.intoPCall();
        }
    }

    @Inject(method = "invoke", at = @At("RETURN"))
    void exit(Varargs args, CallbackInfoReturnable<Varargs> cir) {
        Globals globals = ((BaseLibAccessor) this$0).getGlobals();
        GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
        Hook capture = globalsAccess.figuraExtrass$getCaptureState().getSink();
        if (capture != null) {
            capture.outOfPCall();
        }
    }
}
