package com.github.applejuiceyy.figuraextras.fsstorage;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DataProperty<O> {
    public final DataId<O> dataId;
    private final Path path;
    private byte @Nullable [] bytes = null;

    public DataProperty(Path path, DataId<O> dataId) {
        this.path = path;
        this.dataId = dataId;
        if (dataId.buffered) {
            bytes = new byte[0];
            updateFromDisk();
        }
    }

    void updateFromDisk() {
        if (bytes != null) {
            try {
                bytes = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <P> void initFile(DataId<P> dataId, Path path, P thing) throws IOException {
        try (OutputStream file = new FileOutputStream(path.toFile())) {
            dataId.readWriter.write(thing, new DataOutputStream(file));
        }
    }

    public O read() throws IOException, DataId.ParseException {
        try (InputStream file = bytes != null ? new ByteArrayInputStream(bytes) : new FileInputStream(path.toFile())) {
            O o = dataId.readWriter.read(new DataInputStream(file));
            if (file.read() != -1) {
                throw new DataId.ParseException("Leftover data");
            }
            return o;
        }
    }

    public void write(O thing) throws IOException {
        try (OutputStream file = bytes == null ? new FileOutputStream(path.toFile()) : new ByteArrayOutputStream()) {
            dataId.readWriter.write(thing, new DataOutputStream(file));

            if (bytes != null) {
                assert file instanceof ByteArrayOutputStream;
                bytes = ((ByteArrayOutputStream) file).toByteArray();
                commit();
            }
        }
    }

    void commit() {
        if (bytes != null) {
            try {
                Files.write(path, bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
