package com.github.applejuiceyy.luabridge;

public interface Collector<T> {
    void collect(T data);

    void end();
}
