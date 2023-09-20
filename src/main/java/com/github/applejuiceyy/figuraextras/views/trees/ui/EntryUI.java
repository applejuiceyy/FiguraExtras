package com.github.applejuiceyy.figuraextras.views.trees.ui;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.views.trees.core.Entry;
import com.github.applejuiceyy.figuraextras.views.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.views.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.views.trees.core.Registration;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class EntryUI<V> {
    public final FlowLayout childrenLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

    private final LabelComponent noEntries = Components.label(Component.literal("No entries found"));
    private int currentEntries = 0;
    HashMap<Entry<?, ?, ?>, KeyValueEntryUI<?, ?>> entryMappings = new HashMap<>();

    Runnable listingCancel;

    public EntryUI(Expander<V> expander, ContentPopOut contentPopOut, ReferenceStore referenceStore, Registration registration) {
        childrenLayout.child(noEntries);

        listingCancel = expander.listEntries(new Expander.Callback() {
            @Override
            public void onAddEntry(Entry<?, ?, ?> entry) {
                KeyValueEntryUI<?, ?> e = new KeyValueEntryUI<>(entry, contentPopOut, referenceStore, registration, expander.getUpdater());
                childrenLayout.child(e.root);
                entryMappings.put(entry, e);
                if (currentEntries == 0) {
                    childrenLayout.removeChild(noEntries);
                }
                currentEntries++;
            }

            @Override
            public void onRemoveEntry(Entry<?, ?, ?> entry) {
                KeyValueEntryUI<?, ?> entryUI = entryMappings.remove(entry);
                childrenLayout.removeChild(entryUI.root);
                currentEntries--;
                if (currentEntries == 0) {
                    childrenLayout.child(noEntries);
                }
            }
        });
    }

    public void dispose() {
        for (Map.Entry<Entry<?, ?, ?>, KeyValueEntryUI<?, ?>> entry : entryMappings.entrySet()) {
            entry.getValue().dispose();
        }
        listingCancel.run();
    }
}
