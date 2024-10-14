package com.github.applejuiceyy.figuraextras.fsstorage.storage;

import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.DataId;
import com.github.applejuiceyy.figuraextras.fsstorage.DataProperty;
import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Tail extends Storage {
    private final Path path;
    private final Runnable deleter;
    private final String[] buckets;
    private final Set<DataId<?>> dataIds;
    private boolean deleted = false;
    private SoftReference<ImplBucket> bucket = null;

    public Tail(StorageState state, String[] buckets, Set<DataId<?>> dataIds, Path path, Runnable deleter) {
        super(state);
        this.dataIds = dataIds;
        this.path = path;
        this.deleter = deleter;
        this.buckets = buckets;
    }

    static ChildCreator creator(StorageState state, Set<DataId<?>> dataIds) {
        return new ChildCreator() {
            @Override
            public Storage createFromExisting(Path path, String[] buckets, Runnable deleter) {
                return new Tail(state, buckets, dataIds, path, deleter);
            }

            @Override
            public Storage createFromNew(Path path, String[] buckets, Runnable deleter, Map<DataId<?>, Object> values) {
                for (DataId<?> dataId : dataIds) {
                    if (!values.containsKey(dataId)) {
                        throw new RuntimeException("unfilled data id " + dataId.name);
                    }

                    try {
                        // we know the types match
                        //noinspection unchecked
                        DataProperty.initFile((DataId<? super Object>) dataId, getDataIdPath(path, dataId), values.get(dataId));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                return createFromExisting(path, buckets, deleter);
            }
        };
    }

    private static @NotNull Path getDataIdPath(Path path, DataId<?> dataId) {
        return path.resolve(dataId.name);
    }

    @Override
    protected Bucket _getBucket(String[] buckets, int pos) {
        return getBucket();
    }

    @Override
    protected Iterator<Bucket> _iterate(String[] buckets, int pos) {
        ImplBucket implBucket = getBucket();
        if (implBucket == null) {
            return Iterators.forArray();
        }
        return Iterators.forArray(implBucket);
    }

    @Override
    protected Bucket _createBucket(String[] buckets, int pos, Map<DataId<?>, Object> map) {
        return getBucket();
    }

    private ImplBucket getBucket() {
        ImplBucket implBucket;
        if (bucket != null && (implBucket = bucket.get()) != null) {
            return implBucket;
        }
        if (!probeData()) {
            Storage.logger.error("Malformed storage data for {}", path);
            return null;
        }
        Map<DataId<?>, DataProperty<?>> properties = new HashMap<>();
        for (DataId<?> dataId : dataIds) {
            DataProperty<?> value = new DataProperty<>(path.resolve(dataId.name), dataId);
            properties.put(dataId, value);
        }
        implBucket = new ImplBucket(properties);
        bucket = new SoftReference<>(implBucket);
        return implBucket;
    }

    private boolean probeData() {
        for (DataId<?> dataId : dataIds) {
            Path dataIdPath = getDataIdPath(path, dataId);
            if (!Files.exists(dataIdPath)) {
                return false;
            }
            if (!Files.isRegularFile(dataIdPath)) {
                return false;
            }
            DataProperty<?> property = new DataProperty<>(getDataIdPath(path, dataId), dataId);
            try {
                property.read();
            } catch (IOException | DataId.ParseException e) {
                return false;
            }
        }
        return true;
    }

    private class ImplBucket implements Bucket {
        private final Map<DataId<?>, DataProperty<?>> data;

        ImplBucket(Map<DataId<?>, DataProperty<?>> data) {
            this.data = data;
        }

        @Override
        public <O> void set(DataId<O> dataId, O thing) {
            ensureValid(dataId);
            //noinspection unchecked
            DataProperty<O> property = (DataProperty<O>) data.get(dataId);
            try {
                property.write(thing);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void ensureValid(DataId<?> dataId) {
            ensureValid();
            if (!data.containsKey(dataId)) {
                throw new IllegalArgumentException("DataId not registered");
            }
        }

        private void ensureValid() {
            if (deleted) {
                throw new IllegalStateException("Modifying a deleted bucket");
            }
        }

        @Override
        public <O> O get(DataId<O> dataId) {
            ensureValid(dataId);
            //noinspection unchecked
            DataProperty<O> property = (DataProperty<O>) data.get(dataId);
            try {
                return property.read();
            } catch (DataId.ParseException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void delete() {
            deleted = true;
            for (DataProperty<?> value : data.values()) {
                try {
                    Files.delete(getDataIdPath(path, value.dataId));
                } catch (IOException ignored) {
                }
            }
            bucket = null;
            deleter.run();
        }

        @Override
        public String[] getBuckets() {
            return buckets;
        }
    }
}
