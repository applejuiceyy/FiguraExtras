package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.CaptureOpportunity;

import java.util.HashMap;

public interface LuaRuntimeAccess {
    HashMap<Object, CaptureOpportunity> figuraExtrass$getNoticedPotentialCaptures();
}
