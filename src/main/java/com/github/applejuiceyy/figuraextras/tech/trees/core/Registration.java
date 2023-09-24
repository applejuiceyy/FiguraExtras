package com.github.applejuiceyy.figuraextras.tech.trees.core;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Registration {
    ArrayList<ObjectExpander<?, ?, ?>> expanders = new ArrayList<>();

    ArrayList<ObjectInterpreter<?>> interpreters = new ArrayList<>();

    public List<ObjectExpander<?, ?, ?>> getExpanders() {
        return Collections.unmodifiableList(expanders);
    }

    public List<ObjectInterpreter<?>> getInterpreters() {
        return Collections.unmodifiableList(interpreters);
    }

    public void addExpander(ObjectExpander<?, ?, ?> expander) {
        expanders.add(expander);
    }

    public void addInterpreter(ObjectInterpreter<?> interpreter) {
        interpreters.add(interpreter);
    }

    public static Registration from(Consumer<Registration> consumer) {
        Registration registration = new Registration();
        consumer.accept(registration);
        return registration;
    }
}
