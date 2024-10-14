package com.github.applejuiceyy.figuraextras.fsstorage;

public interface Bucket {
    <O> void set(DataId<O> dataId, O thing);

    <O> O get(DataId<O> dataId);

    void delete();

    String[] getBuckets();
}
