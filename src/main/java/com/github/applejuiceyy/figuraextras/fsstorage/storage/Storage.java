package com.github.applejuiceyy.figuraextras.fsstorage.storage;

import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.DataId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class Storage implements Iterable<Bucket> {
    static final Logger logger = LoggerFactory.getLogger("FiguraExtras:BucketStorage");
    private final StorageState state;

    Storage(StorageState state) {
        this.state = state;
    }

    public static Storage create(Path path, Set<DataId<?>> dataIds, int bucketCount) {
        StorageState storageState = new StorageState();

        ChildCreator creator = Tail.creator(storageState, dataIds);
        for (int count = bucketCount; count > 0; count--) {
            ChildCreator finalCreator = creator;
            creator = Segment.creator(storageState, finalCreator);
        }
        return new Head(storageState, path, creator);
    }

    @Nullable
    public Bucket getBucket(String... buckets) {
        return _getBucket(buckets, 0);
    }

    ;

    @Nullable
    protected abstract Bucket _getBucket(String[] buckets, int pos);

    public BucketBuilder createBucket(String... buckets) {
        return new BucketBuilder(buckets);
    }

    @NotNull
    public Iterator<Bucket> iterator() {
        return iterate(new String[]{}).iterator();
    }

    public Iterable<Bucket> iterate(String... buckets) {
        return () -> _iterate(buckets, 0);
    }

    protected abstract Iterator<Bucket> _iterate(String[] buckets, int pos);

    protected abstract Bucket _createBucket(String[] buckets, int pos, Map<DataId<?>, Object> values);


    interface ChildCreator {
        Storage createFromExisting(Path path, String[] buckets, Runnable deleter);

        Storage createFromNew(Path path, String[] buckets, Runnable deleter, Map<DataId<?>, Object> values);
    }

    public class BucketBuilder {
        Map<DataId<?>, Object> map;
        String[] buckets;

        BucketBuilder(String[] buckets) {
            this.buckets = buckets;
            map = new HashMap<>();
        }

        public <O> BucketBuilder data(DataId<O> dataId, O object) {
            map.put(dataId, object);
            return this;
        }

        public Bucket create() {
            return _createBucket(buckets, 0, map);
        }
    }
}
