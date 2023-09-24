package com.github.applejuiceyy.figuraextras.tech.trees.core;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;

import java.util.Optional;

public class Expander<V> {
    public final Observers.Observer<Optional<V>> observer;
    private final Registration registration;
    private final Event<Runnable>.Source updater;

    public Expander(Observers.Observer<Optional<V>> observer, Registration registration, Event<Runnable>.Source updater) {
        this.updater = updater;
        this.observer = observer;
        this.registration = registration;
    }

    public Event<Runnable>.Source getUpdater() {
        return updater;
    }

    public Runnable listEntries(Callback callback) {
        FiguraExtras.logger.info("Now ticking item listings");
        ExpanderHost<V> host = new ExpanderHost<>(observer, registration, callback, updater);
        return host.getCancel();
    }

    public Registration getRegistration() {
        return registration;
    }

    public interface Callback {
        void onAddEntry(Entry<?, ?, ?> entry);

        void onRemoveEntry(Entry<?, ?, ?> entry);
    }
}
