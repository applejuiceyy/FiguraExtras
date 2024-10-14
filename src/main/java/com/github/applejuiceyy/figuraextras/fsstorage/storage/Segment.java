package com.github.applejuiceyy.figuraextras.fsstorage.storage;

import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.DataId;
import com.github.applejuiceyy.figuraextras.fsstorage.FolderManager;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import net.minecraft.util.Tuple;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class Segment extends Storage {


    private final String[] buckets;
    private final Runnable deleter;
    private final FolderManager folderManager;
    private final Map<String, Storage> mapping = new HashMap<>();
    private final ChildCreator creator;
    Segment(StorageState state, String[] buckets, Path path, Runnable deleter, ChildCreator creator) {
        super(state);
        folderManager = FolderManager.open(path);
        this.deleter = deleter;
        this.creator = creator;
        this.buckets = buckets;

        for (Tuple<String, Path> directories : folderManager) {
            createChildFromExisting(directories.getA(), directories.getB());
        }
    }

    private void createChildFromExisting(String name, Path path) {
        createChild(name, (runnable) -> {
            String[] r = new String[buckets.length + 1];
            System.arraycopy(buckets, 0, r, 0, buckets.length);
            r[r.length - 1] = name;
            return creator.createFromExisting(path, r, runnable);
        });
    }

    private void createChild(String name, Function<Runnable, Storage> applier) {
        mapping.put(name, applier.apply(() -> {
            try {
                folderManager.deleteFolder(name);
                mapping.remove(name);
                if (!folderManager.iterator().hasNext()) {
                    deleter.run();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    static ChildCreator creator(StorageState state, ChildCreator childCreator) {
        return new ChildCreator() {
            @Override
            public Storage createFromExisting(Path path, String[] buckets, Runnable deleter) {
                return new Segment(state, buckets, path, deleter, childCreator);
            }

            @Override
            public Storage createFromNew(Path path, String[] buckets, Runnable deleter, Map<DataId<?>, Object> values) {
                return new Segment(state, buckets, path, deleter, childCreator);
            }
        };
    }

    @Override
    protected Bucket _createBucket(String[] buckets, int pos, Map<DataId<?>, Object> values) {
        if (pos >= buckets.length) {
            throw new IllegalArgumentException("Invalid bucket length");
        }

        String name = buckets[pos];
        if (!mapping.containsKey(name)) {
            Path path;
            try {
                path = folderManager.createFolder(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            createChildFromNew(name, path, values);
        }

        try {
            return mapping.get(name)._createBucket(buckets, pos + 1, values);
        } catch (Throwable exc) {
            try {
                folderManager.deleteFolder(name);
            } catch (IOException ignored) {
            }
            mapping.remove(name);
            throw exc;
        }
    }

    private void createChildFromNew(String name, Path path, Map<DataId<?>, Object> values) {
        createChild(name, runnable -> {
            String[] r = new String[buckets.length + 1];
            System.arraycopy(buckets, 0, r, 0, buckets.length);
            r[r.length - 1] = name;
            return creator.createFromNew(path, r, runnable, values);
        });
    }

    @Override
    protected Bucket _getBucket(String[] buckets, int pos) {
        if (pos >= buckets.length) {
            throw new IllegalArgumentException("Invalid bucket length");
        }
        if (mapping.containsKey(buckets[pos])) {
            return mapping.get(buckets[pos])._getBucket(buckets, pos + 1);
        }

        return null;
    }

    @Override
    protected Iterator<Bucket> _iterate(String[] buckets, int pos) {
        if (pos >= buckets.length) {
            return mapping.values().stream()
                    .map(storage -> storage._iterate(buckets, pos + 1))
                    .flatMap(Streams::stream)
                    .iterator();
        }
        String name = buckets[pos];
        if (mapping.containsKey(name)) {
            return mapping.get(name)._iterate(buckets, pos + 1);
        }
        return Iterators.forArray();
    }
}
