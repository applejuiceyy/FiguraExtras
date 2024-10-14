package com.github.applejuiceyy.figuraextras.fsstorage.storage;

import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.DataId;
import com.google.common.collect.Iterators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class Head extends Storage {
    private final Path path;
    private final ChildCreator creator;
    Storage delegate = null;

    Head(StorageState state, Path path, ChildCreator creator) {
        super(state);
        this.path = path;
        this.creator = creator;
        if (Files.exists(path)) {
            delegate = creator.createFromExisting(path, new String[0], this::delete);
        }
    }

    private void delete() {
        try {
            Files.delete(path);
            delegate = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Bucket _createBucket(String[] buckets, int pos, Map<DataId<?>, Object> values) {
        if (delegate == null) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            delegate = creator.createFromNew(path, new String[0], this::delete, values);
        }
        return delegate._createBucket(buckets, pos, values);
    }

    @Override
    protected Bucket _getBucket(String[] buckets, int pos) {
        if (delegate == null) {
            return null;
        }
        return delegate._getBucket(buckets, pos);
    }

    @Override
    protected Iterator<Bucket> _iterate(String[] buckets, int pos) {
        if (delegate == null) {
            return Iterators.forArray();
        }
        return delegate._iterate(buckets, pos);
    }
}
