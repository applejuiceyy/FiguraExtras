package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import java.util.Optional;

public interface NumberRangeAlike {
    float getMin();

    float getMax();

    float getValue();

    default Optional<Float> getStepSize() {
        return Optional.empty();
    }

    ;
}
