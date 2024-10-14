package com.github.applejuiceyy.figuraextras.fsstorage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommonOps {
    public static final DataId<Instant> TIME = DataId.of(new DataId.RW<>() {
        @Override
        public Instant read(DataInputStream stream) throws IOException {
            return Instant.ofEpochMilli(stream.readLong());
        }

        @Override
        public void write(Instant data, DataOutputStream stream) throws IOException {
            stream.writeLong(data.toEpochMilli());
        }
    }, "time");

    private CommonOps() {
    }

    public static void pruneBucketsByTime(Iterable<? extends Bucket> storage, Duration maxDuration) {
        pruneBucketsByTime(storage, maxDuration, TIME);
    }

    public static void pruneBucketsByTime(Iterable<? extends Bucket> storage, Duration maxDuration, DataId<Instant> timeLike) {
        List<Bucket> toDelete = new ArrayList<>();
        for (Bucket bucket : storage) {
            Instant l = bucket.get(timeLike);
            if (l.plus(maxDuration).isBefore(Instant.now())) {
                toDelete.add(bucket);
            }
        }
        for (Bucket bucket : toDelete) {
            bucket.delete();
        }
    }
}
