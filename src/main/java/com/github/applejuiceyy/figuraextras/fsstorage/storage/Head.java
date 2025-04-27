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
            create();
        }
    }

    private void create() {
        delegate = creator.createFromExisting(path, new String[0], this::delete);
    }

    @Override
    protected Bucket _getBucket(String[] buckets, int pos) {
        return bound() ? delegate._getBucket(buckets, pos) : null;
    }

    @Override
    protected Iterator<Bucket> _iterate(String[] buckets, int pos) {
        return bound() ? delegate._iterate(buckets, pos) : Iterators.forArray();
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
        if (!bound()) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            delegate = creator.createFromNew(path, new String[0], this::delete, values);
        }

        return delegate._createBucket(buckets, pos, values);
    }

    private boolean bound() {
        if (Files.exists(path)) {
            ensureExisting();
            return true;
        } else {
            ensureNonExisting();
            return false;
        }
    }

    private void ensureExisting() {
        if (delegate == null) {
            logger.warn("Unexpected creation of head folder {}", this.path);
            create();
        }
    }

    private void ensureNonExisting() {
        if (delegate != null) {
            logger.warn("Unexpected deletion of head folder {}", this.path);
            delegate = null;
        }
    }
}
