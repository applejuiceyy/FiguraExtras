package com.github.applejuiceyy.figuraextras.fsstorage;

import net.minecraft.util.Tuple;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DataId<O> {
    public static RW<byte[]> PASS_THROUGH = new RW<>() {
        @Override
        public byte[] read(DataInputStream stream) throws IOException {
            return stream.readAllBytes();
        }

        @Override
        public void write(byte[] data, DataOutputStream stream) throws IOException {
            stream.write(data);
        }
    };
    public final String name;
    final RW<O> readWriter;
    final boolean buffered;


    DataId(RW<O> readWriter, String name, boolean buffered) {
        this.readWriter = readWriter;
        this.name = name;
        this.buffered = buffered;
    }

    public static <O> DataId<O> of(RW<O> readWriter, String name) {
        return new DataId<>(readWriter, name, true);
    }

    public static <O> DataId<O> of(RW<O> readWriter, String name, boolean buffered) {
        return new DataId<>(readWriter, name, buffered);
    }

    public static <P> RW<List<P>> repeating(RW<P> readWriter) {
        return new RW<>() {
            @Override
            public List<P> read(DataInputStream stream) throws ParseException, IOException {
                if (!stream.markSupported()) {
                    stream = new DataInputStream(new BufferedInputStream(stream));
                }

                List<P> r = new ArrayList<>();

                while (true) {
                    stream.mark(1);
                    if (stream.read() == -1) {
                        return r;
                    }
                    stream.reset();

                    try {
                        r.add(readWriter.read(stream));
                    } catch (EOFException exc) {
                        throw (ParseException) new ParseException().initCause(exc);
                    }
                }
            }

            @Override
            public void write(List<P> data, DataOutputStream stream) throws IOException {
                for (P datum : data) {
                    readWriter.write(datum, stream);
                }
            }
        };
    }

    public static <A, B> RW<Tuple<A, B>> concat(RW<A> arw, RW<B> brw) {
        return new RW<>() {
            @Override
            public Tuple<A, B> read(DataInputStream stream) throws ParseException, IOException {
                return new Tuple<>(arw.read(stream), brw.read(stream));
            }

            @Override
            public void write(Tuple<A, B> data, DataOutputStream stream) throws IOException {
                arw.write(data.getA(), stream);
                brw.write(data.getB(), stream);
            }
        };
    }

    public static void lengthMarkedWrite(OutputStream incoming, CapriciousConsumer<DataOutputStream> gated) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gated.accept(new DataOutputStream(outputStream));
        byte[] bytes = outputStream.toByteArray();
        new DataOutputStream(incoming).writeInt(bytes.length);
        incoming.write(bytes);
    }

    public static InputStream lengthMarkedRead(InputStream incoming) throws IOException {
        return new BoundedInputStream(incoming, new DataInputStream(incoming).readInt());
    }

    public DataId<O> withName(String name) {
        return new DataId<>(readWriter, name, buffered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(readWriter, name, buffered);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataId<?> dataId)) return false;
        return buffered == dataId.buffered && Objects.equals(readWriter, dataId.readWriter) && Objects.equals(name, dataId.name);
    }

    public interface RW<G> {
        G read(DataInputStream stream) throws ParseException, IOException;

        void write(G data, DataOutputStream stream) throws IOException;
    }

    public interface CapriciousConsumer<T> {
        void accept(T thing) throws IOException;
    }

    public static class ParseException extends Exception {
        public ParseException() {
            super();
        }

        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public ParseException(Throwable cause) {
            super(cause);
        }
    }
}
