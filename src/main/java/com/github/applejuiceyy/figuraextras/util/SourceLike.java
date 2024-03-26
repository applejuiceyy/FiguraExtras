package com.github.applejuiceyy.figuraextras.util;

public interface SourceLike<T> {
    Runnable subscribe(T subscriber);

    void unsubscribe(T subscriber);
}
