package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;

public interface GlobalsAccess {
    ActiveOpportunity<?> figuraExtrass$getCurrentlySearchingForCapture();

    void figuraExtrass$setCurrentlySearchingForCapture(ActiveOpportunity<?> captureOpportunity);

    public SecondaryCallHook figuraExtrass$getCurrentCapture();

    public void figuraExtrass$setCurrentCapture(SecondaryCallHook hook);
}
