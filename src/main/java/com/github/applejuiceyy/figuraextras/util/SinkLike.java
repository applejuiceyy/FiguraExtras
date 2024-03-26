package com.github.applejuiceyy.figuraextras.util;

import java.util.function.Consumer;

public interface SinkLike<T> {
    void run(Consumer<T> running);
}
