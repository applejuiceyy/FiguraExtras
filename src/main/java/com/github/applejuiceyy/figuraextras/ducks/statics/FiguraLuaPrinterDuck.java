package com.github.applejuiceyy.figuraextras.ducks.statics;

import org.figuramc.figura.avatar.Avatar;

public class FiguraLuaPrinterDuck {
    public static Avatar currentAvatar = null;
    public static Kind currentKind = null;

    public static boolean skipUserdataStuff = false;

    public enum Kind {
        PRINT, PINGS, ERRORS, OTHER
    }
}
