package com.github.applejuiceyy.figuraextras.ducks;

import org.figuramc.figura.avatar.Avatar;

public class FiguraLuaPrinterDuck {
    public static Avatar currentAvatar = null;
    public static Kind currentKind = null;

    public enum Kind {
        PRINT, PINGS, ERRORS, OTHER
    }
}
