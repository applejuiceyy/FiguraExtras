package com.github.applejuiceyy.figuraextras.views.trees.core;

import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.views.trees.interfaces.ObjectExpander;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpanderInstance<V> {
    private final Registration registration;

    public ExpanderInstance(Observers.Observer<Optional<V>> observer, List<ObjectExpander<?, ?, ?>> excluded, Registration registration, ExpanderHost<?> host) {
        this.registration = registration;

        for (ObjectExpander<?, ?, ?> expander : registration.getExpanders()) {
            if (!excluded.contains(expander)) {
                new ObjectExpanderRuntime<>(
                        expander,
                        observer,
                        excluded,
                        registration,
                        host
                );
            }
        }
        /*
        observer.observe(value -> {
            List<ObjectExpander<V, ?, ?>> expanders;
            if(value.isPresent()) {
                //noinspection unchecked
                Class<V> v = (Class<V>) value.get().getClass();
                expanders = findExpanders(v);
            }
            else {
                expanders = Collections.emptyList();
            }

            for (Iterator<Map.Entry<ObjectExpander<V, ?, ?>, ObjectExpanderRuntime<V, ?, ?>>> iterator = runningExpanders.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<ObjectExpander<V, ?, ?>, ObjectExpanderRuntime<V, ?, ?>> entry = iterator.next();
                if (!expanders.contains(entry.getKey())) {
                    ObjectExpanderRuntime<V, ?, ?> runtime = entry.getValue();

                    // it was being angry and I couldn't bother to deal with it
                    // also a copy in here is fine to deal with CME because it's not a hot path
                    //noinspection rawtypes
                    for (Observers.WritableObserver o: new ArrayList<>(runtime.livingObservers)) {
                        //noinspection unchecked
                        o.set(Optional.empty());
                    }


                    iterator.remove();
                }
            }

            for (ObjectExpander<V, ?, ?> expander : expanders) {
                if(runningExpanders.containsKey(expander)) {
                    ObjectExpanderRuntime<V, ?, ?> runtime = runningExpanders.get(expander);
                    runtime.incoming.set(value.get());
                }
                else {
                    runningExpanders.put(expander, new ObjectExpanderRuntime<>(
                            expander,
                            Observers.of(value.get()),
                            excluded,
                            registration,
                            host
                    ));
                }
            }
        });*/
    }

    <T> ArrayList<ObjectExpander<T, ?, ?>> findExpanders(Class<T> object) {
        ArrayList<ObjectExpander<T, ?, ?>> expanders = new ArrayList<>();
        for (ObjectExpander<?, ?, ?> expander : registration.expanders) {
            if (expander.getObjectClass().isAssignableFrom(object)) {
                //noinspection unchecked
                expanders.add((ObjectExpander<T, ?, ?>) expander);
            }
        }
        return expanders;
    }

    static class ObjectExpanderRuntime<T, K, V> {
        ObjectExpanderRuntime(
                ObjectExpander<?, K, V> expander,
                Observers.Observer<Optional<T>> observer,
                List<ObjectExpander<?, ?, ?>> excluded,
                Registration registration,
                ExpanderHost<?> host
        ) {
            Observers.Observer<Optional<T>> derives = observer.derive(live ->
                    live.filter(value -> expander.getObjectClass().isAssignableFrom(value.getClass()))
            );
            //noinspection unchecked
            ObjectExpander<T, K, V> p = (ObjectExpander<T, K, V>) expander;
            List<ObjectExpander<?, ?, ?>> e = new ArrayList<>(excluded);
            e.add(p);
            p.fetchAllEntries(
                    derives,
                    new ExpanderAdder(e, registration, host),
                    value -> {
                        Entry<T, K, V> entry = new Entry<>(
                                derives,
                                p,
                                value
                        );

                        Observers.ConditionalObservation<Optional<Tuple<K, V>>> o = value.conditionalObservation(k -> {
                            if (k.isEmpty()) {
                                host.callback.onRemoveEntry(entry);
                                return true;
                            }
                            return false;
                        });

                        o.start();
                        host.stopUpdatingEntries.getSource().subscribe(o::stop);

                        host.callback.onAddEntry(entry);
                    },
                    host.ticker,
                    host.stopUpdatingEntries.getSource()
            );
        }
    }
}
