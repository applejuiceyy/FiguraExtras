package com.github.applejuiceyy.figuraextras.fsstorage.storage;

import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.DataId;
import com.github.applejuiceyy.figuraextras.fsstorage.FolderManager;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import net.minecraft.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class Segment extends Storage {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:Storage/Segment");

    private final String[] buckets;
    private final Runnable deleter;
    private final FolderManager folderManager;
    private final Map<String, Storage> mapping = new HashMap<>();
    private final ChildCreator creator;
    private final Path path;

    Segment(StorageState state, String[] buckets, Path path, Runnable deleter, ChildCreator creator) {
        super(state);
        folderManager = FolderManager.open(path);
        this.deleter = deleter;
        this.creator = creator;
        this.buckets = buckets;
        this.path = path;

        for (Tuple<String, Path> directories : folderManager) {
            createChildFromExisting(directories.getA(), directories.getB());
        }
    }

    private void checkMapping() {
        for (Map.Entry<String, Storage> entry : mapping.entrySet()) {
            if (folderManager.getFolder(entry.getKey()) == null) {
                ensureNonExistent(entry.getKey());
            }
        }
        for (Tuple<String, Path> tuple : folderManager) {
            ensureExistent(tuple.getA(), tuple.getB());
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

        String name = buckets[pos];

        Path path = folderManager.getFolder(name);
        if (path == null) {
            ensureNonExistent(name);
            return null;
        } else {
            ensureExistent(name, path);
            return mapping.get(name)._getBucket(buckets, pos + 1);
        }
    }

    private void ensureNonExistent(String name) {
        if (mapping.containsKey(name)) {
            logger.warn("Unexpected deletion of folder {} in {}", name, this.path);
            mapping.remove(name);
        }
    }

    private void ensureExistent(String name, Path path) {
        if (!mapping.containsKey(name)) {
            logger.warn("Unexpected creation of folder {} in {}", name, this.path);
            createChildFromExisting(name, path);
        }
    }

    @Override
    protected Iterator<Bucket> _iterate(String[] buckets, int pos) {
        if (pos >= buckets.length) {
            checkMapping();
            return Streams.stream(folderManager.iterator())
                    .map(t -> {
                        String a = t.getA();
                        ensureExistent(a, t.getB());
                        return mapping.get(a);
                    })
                    .map(storage -> storage._iterate(buckets, pos + 1))
                    .flatMap(Streams::stream)
                    .iterator();
        }
        String name = buckets[pos];

        Path path = folderManager.getFolder(name);
        if (path == null) {
            ensureNonExistent(name);
            return Iterators.forArray();
        } else {
            ensureExistent(name, path);
            return mapping.get(name)._iterate(buckets, pos + 1);
        }
    }
}
