package com.github.applejuiceyy.figuraextras.tech.trees.ui;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Entry;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Registration;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class EntryUI<V> {
    public final Flow childrenLayout = new Flow();

    private final Label noEntries = new Label(Component.literal("No entries found"));
    HashMap<Entry<?, ?, ?>, KeyValueEntryUI<?, ?>> entryMappings = new HashMap<>();
    Runnable listingCancel;
    private int currentEntries = 0;

    public EntryUI(Expander<V> expander, ContentPopOut contentPopOut, ReferenceStore referenceStore, Registration registration) {
        childrenLayout.add(noEntries);

        listingCancel = expander.listEntries(new Expander.Callback() {
            @Override
            public void onAddEntry(Entry<?, ?, ?> entry) {
                KeyValueEntryUI<?, ?> e = new KeyValueEntryUI<>(entry, contentPopOut, referenceStore, registration, expander.getUpdater());
                childrenLayout.add(e.root);
                entryMappings.put(entry, e);
                if (currentEntries == 0) {
                    childrenLayout.remove(noEntries);
                }
                currentEntries++;
            }

            @Override
            public void onRemoveEntry(Entry<?, ?, ?> entry) {
                KeyValueEntryUI<?, ?> entryUI = entryMappings.remove(entry);
                childrenLayout.remove(entryUI.root);
                currentEntries--;
                if (currentEntries == 0) {
                    childrenLayout.add(noEntries);
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
