package com.github.applejuiceyy.figuraextras.ducks;

import java.util.function.IntConsumer;

public interface InstructionsAccess {
    Runnable addHook(IntConsumer result);
}
