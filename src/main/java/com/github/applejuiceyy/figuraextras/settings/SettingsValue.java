package com.github.applejuiceyy.figuraextras.settings;

import com.github.applejuiceyy.figuraextras.util.Event;

import java.util.function.Consumer;

public interface SettingsValue<V> {
    static <V> SettingsValue<V> readOnly(V value) {
        return new SettingsValue<>() {
            @Override
            public void set(Object value) {
                throw new UnsupportedOperationException("Setting is read-only");
            }

            @Override
            public V get() {
                return value;
            }

            @Override
            public Event<Consumer<V>>.Source onChange() {
                return Event.<V>consumer().getSource();
            }
        };
    }

    void set(V value);

    V get();

    Event<Consumer<V>>.Source onChange();
}
