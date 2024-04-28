package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.PossibleCapture;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.vscode.dsp.SourceListener;
import net.minecraft.util.Tuple;
import org.luaj.vm2.Prototype;

import java.util.HashMap;
import java.util.WeakHashMap;

public interface LuaRuntimeAccess {
    HashMap<Object, PossibleCapture> figuraExtrass$getNoticedPotentialCaptures();

    Event<SourceListener>.Source figuraExtrass$dynamicLoadsEvent();

    HashMap<Integer, Tuple<String, String>> figuraExtrass$getRegisteredDynamicSources();

    int figuraExtrass$newDynamicLoad(Prototype prototype, String source);

    String figuraExtrass$getSource(int i);

    WeakHashMap<Prototype, Integer> figuraExtrass$getPrototypesMarkedAsLoadStringed();
}
