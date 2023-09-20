package com.github.applejuiceyy.figuraextras.views.trees.core;

import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.views.trees.interfaces.ObjectExpander;

import java.util.List;
import java.util.Optional;

public class ExpanderAdder implements ObjectExpander.Adder {
    private final List<ObjectExpander<?, ?, ?>> excluded;
    private final Registration registration;
    private final ExpanderHost<?> host;

    ExpanderAdder(
            List<ObjectExpander<?, ?, ?>> excluded,
            Registration registration,
            ExpanderHost<?> host
    ) {
        this.excluded = excluded;
        this.registration = registration;
        this.host = host;
    }

    @Override
    public <O> void add(Observers.Observer<Optional<O>> value) {

        new ExpanderInstance<>(value, excluded, registration, host);
    }
}
