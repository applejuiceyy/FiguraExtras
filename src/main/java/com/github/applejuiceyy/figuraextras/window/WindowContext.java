package com.github.applejuiceyy.figuraextras.window;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;

import java.util.OptionalInt;

public interface WindowContext {
    void lockGuiScale(int scale);

    void unlockGuiScale();

    OptionalInt getLockedGuiScale();

    boolean canSetGuiScale();

    int getRecommendedGuiScale();

    boolean allowsCustomTitleBar();

    void setShowTitleBar(boolean show);

    WindowContentPopOutHost getContentPopOutHost();

    default boolean isCompletelyOverlaying() {
        return false;
    }

}
