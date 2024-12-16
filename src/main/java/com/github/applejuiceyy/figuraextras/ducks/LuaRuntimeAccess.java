package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.ipc.dsp.SourceListener;
import com.github.applejuiceyy.figuraextras.tech.captures.PossibleCapture;
import com.github.applejuiceyy.figuraextras.util.Event;
import net.minecraft.util.Tuple;
import org.luaj.vm2.Prototype;

import java.util.HashMap;
import java.util.WeakHashMap;

public interface LuaRuntimeAccess {
    HashMap<Object, PossibleCapture> figuraExtras$getNoticedPotentialCaptures();

    Event<SourceListener>.Source figuraExtras$dynamicLoadsEvent();

    HashMap<Integer, Tuple<String, String>> figuraExtras$getRegisteredDynamicSources();

    int figuraExtras$newDynamicLoad(Prototype prototype, String source);

    String figuraExtras$getSource(int i);

    WeakHashMap<Prototype, Integer> figuraExtras$getPrototypesMarkedAsLoadStringed();
}
