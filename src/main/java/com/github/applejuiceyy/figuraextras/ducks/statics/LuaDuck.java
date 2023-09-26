package com.github.applejuiceyy.figuraextras.ducks.statics;

public class LuaDuck {
    public static CallType currentCallType = CallType.NORMAL;

    public enum CallType {
        NORMAL, TAIL
    }

    public enum ReturnType {
        NORMAL, ERROR
    }
}
