package com.github.applejuiceyy.figuraextras.ducks.statics;

import com.github.applejuiceyy.figuraextras.mixin.figura.AvatarManagerAccessor;
import org.figuramc.figura.backend2.NetworkStuff;

public class AuthHandlerDuck {
    static private boolean isDiverting = false;

    public static void setDivert(boolean divert) {
        if (divert == isDiverting) {
            return;
        }
        NetworkStuff.disconnect("Diverting");
        isDiverting = divert;
        AvatarManagerAccessor.getFetchedUsers().clear();
        NetworkStuff.auth();
    }

    public static boolean isDiverting() {
        return isDiverting;
    }
}
