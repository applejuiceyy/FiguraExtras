package com.github.applejuiceyy.figuraextras.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Differential<I, K, O> {
    private final Iterable<I> iterator;
    private final HashMap<K, O> mapping = new HashMap<>();
    private final Function<I, K> key;
    private final Function<I, O> creator;
    private final Consumer<O> disposer;

    public Differential(Iterable<I> iterator, Function<I, K> key, Function<I, O> creator, Consumer<O> disposer) {
        this.key = key;
        this.iterator = iterator;
        this.creator = creator;
        this.disposer = disposer;
    }

    public void update(BiConsumer<O, I> consumer) {
        Iterator<I> t = iterator.iterator();
        ArrayList<K> seen = new ArrayList<>();
        while (t.hasNext()) {
            I r = t.next();
            K key = this.key.apply(r);
            seen.add(key);

            if (mapping.containsKey(key)) {
                consumer.accept(mapping.get(key), r);
            } else {
                mapping.put(key, creator.apply(r));
            }
        }

        for (Iterator<Map.Entry<K, O>> iter = mapping.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<K, O> ioEntry = iter.next();
            if (!seen.contains(ioEntry.getKey())) {
                iter.remove();
                disposer.accept(ioEntry.getValue());
            }
        }
    }

    public void update(Consumer<O> consumer) {
        update((o, u) -> consumer.accept(o));
    }

    public void withoutTest(Consumer<O> consumer) {
        for (O value : mapping.values()) {
            consumer.accept(value);
        }
    }

    public void dispose() {
        for (O value : mapping.values()) {
            disposer.accept(value);
        }
        mapping.clear();
    }
}
