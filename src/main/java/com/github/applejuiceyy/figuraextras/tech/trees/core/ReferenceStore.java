package com.github.applejuiceyy.figuraextras.tech.trees.core;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectDescriber;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class ReferenceStore {
    HashMap<Object, ArrayList<Observers.WritableObserver<Integer>>> seen = new HashMap<>();
    IntObjectHashMap<Object> referenceIdToObject = new IntObjectHashMap<>();
    HashMap<Object, Integer> objectToReferenceId = new HashMap<>();

    public ReferenceStore() {

    }

    public ReferenceCreator referenceCreator() {
        return new ReferenceCreator();
    }

    public class ReferenceCreator implements ObjectDescriber.CyclicReferenceConsumer {
        ArrayList<Reference<?>> references = new ArrayList<>();

        @Override
        public <O> Observers.Observer<Component> accept(Observers.Observer<Optional<O>> observer) {
            Reference<O> ref = new Reference<>(observer);
            references.add(ref);
            return ref.returns;
        }

        public void dispose() {
            references.forEach(Reference::dispose);
            references.clear();
        }
    }

    class Reference<O> {
        Observers.Observer<Optional<O>> original;
        Object oldValue = null;
        Observers.WritableObserver<Integer> result;
        Observers.Observer<Component> returns;
        Observers.UnSubscriber unsubber;

        Reference(Observers.Observer<Optional<O>> original) {
            this.original = original;
            result = Observers.of(0);

            unsubber = original.observe(optional -> {
                removeOldValue();

                if (optional.isPresent()) {
                    Object value = optional.get();

                    if (!ReferenceStore.this.seen.containsKey(value)) {
                        ReferenceStore.this.seen.put(value, new ArrayList<>());
                    }

                    ArrayList<Observers.WritableObserver<Integer>> list = ReferenceStore.this.seen.get(value);
                    list.add(result);

                    if (list.size() > 1) {
                        if (list.size() == 2) {
                            int newReference = 1;
                            while (referenceIdToObject.containsKey(newReference)) {
                                newReference++;
                            }

                            referenceIdToObject.put(newReference, value);
                            objectToReferenceId.put(value, newReference);

                            for (Observers.WritableObserver<Integer> integerWritableObserver : list) {
                                integerWritableObserver.set(newReference);
                            }
                        } else {
                            result.set(objectToReferenceId.get(value));
                        }
                    }

                    oldValue = value;
                }
            });

            returns = result.derive(value -> {
                if (value == 0) {
                    return Component.empty();
                }

                return Component.literal(" (*" + value + ")").withStyle(ChatFormatting.AQUA);
            });
        }

        private void removeOldValue() {
            if (oldValue != null) {
                ReferenceStore.this.seen.get(oldValue).remove(result);

                if (ReferenceStore.this.seen.get(oldValue).size() == 1) {
                    ReferenceStore.this.seen.get(oldValue).get(0).set(0);
                    referenceIdToObject.remove(objectToReferenceId.remove(oldValue));
                } else if (ReferenceStore.this.seen.get(oldValue).size() == 0) {
                    ReferenceStore.this.seen.remove(oldValue);
                }
                result.set(0);
                oldValue = null;
            }
        }

        void dispose() {
            unsubber.stop();
            removeOldValue();
        }
    }
}
