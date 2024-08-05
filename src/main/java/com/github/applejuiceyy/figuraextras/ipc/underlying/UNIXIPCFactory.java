package com.github.applejuiceyy.figuraextras.ipc.underlying;

import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class UNIXIPCFactory extends IPCFactory {
    private static String toUnixDomain(String path) {
        return "/tmp/unix_domain/" + path.replace("\\", FileSystems.getDefault().getSeparator()) + ".sock";
    }

    @Contract("_->param1")
    private static String createFolders(String name) throws IOException {
        Path path = Path.of(name);
        File file = new File(path.getParent().toUri());
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Cannot create necessary folders");
        }
        return name;
    }

    private static Tuple<InputStream, OutputStream> wrap(SocketChannel channel) {
        AtomicInteger openStreams = new AtomicInteger(2);
        return new Tuple<>(
                new ReadableByteChannelInputStream(channel, openStreams, channel::close),
                new WritableByteChannelOutputStream(channel, openStreams, channel::close)
        );
    }

    @Override
    public boolean exists(String name) {
        Path path = Path.of(toUnixDomain(name));
        return new File(path.toUri()).exists();
    }

    @Override
    public IPC createServer(String path) throws IOException {
        ArrayList<AutoCloseable> autos = new ArrayList<>();
        try {
            ServerSocketChannel socket = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            autos.add(socket);
            String name = createFolders(toUnixDomain(path));
            autos.add(() -> {
                if (!new File(toUnixDomain(path)).delete()) {
                    throw new IOException("Could not delete files while cleaning up");
                }
            });
            socket.bind(UnixDomainSocketAddress.of(name));
            return new UNIXIPC(socket, name);
        } catch (IOException e) {
            Util.closeMultiple(autos.toArray(new AutoCloseable[0]));
            throw e;
        }
    }

    @Override
    public Tuple<InputStream, OutputStream> connectAsClient(String path) throws IOException {
        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(UnixDomainSocketAddress.of(createFolders(toUnixDomain(path))));
        return wrap(channel);
    }

    interface RambunctiousRunnable {
        void close() throws IOException;
    }

    static class UNIXIPC extends IPC {

        private final ServerSocketChannel socket;
        private final String path;

        public UNIXIPC(ServerSocketChannel socket, String path) {
            this.socket = socket;
            this.path = path;
        }

        @Override
        public Tuple<InputStream, OutputStream> connect(boolean awaitIfFull) throws IOException {
            SocketChannel connection = socket.accept();
            return wrap(connection);
        }

        @Override
        public void close() throws IOException {
            socket.close();
            if (new File(path).delete()) {
                throw new IOException("Could not delete file");
            }
            ;
        }

        @Override
        public void _continuousConnect(BiConsumer<InputStream, OutputStream> consumer) throws IOException {
            try {
                super._continuousConnect(consumer);
            } catch (AsynchronousCloseException ignored) {
            }
        }
    }

    static class WritableByteChannelOutputStream extends OutputStream {

        private final WritableByteChannel channel;
        private final RambunctiousRunnable closer;
        private final AtomicInteger openStreams;

        private boolean closed = false;

        public WritableByteChannelOutputStream(WritableByteChannel channel, AtomicInteger openStreams, RambunctiousRunnable closer) {
            this.channel = channel;
            this.closer = closer;
            this.openStreams = openStreams;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public void write(int b) throws IOException {
            ensureOpen();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1);
            buffer.mark();
            buffer.put((byte) b);
            buffer.reset();
            channel.write(buffer);
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            ensureOpen();
            ByteBuffer buffer = ByteBuffer.allocateDirect(len);
            buffer.mark();
            buffer.put(b, off, len);
            buffer.reset();
            channel.write(buffer);
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;

            if (openStreams.decrementAndGet() == 0) {
                closer.close();
            }
            ;
        }
    }

    static class ReadableByteChannelInputStream extends InputStream {

        private final ReadableByteChannel channel;
        private final RambunctiousRunnable closer;
        private final AtomicInteger openStreams;
        private boolean closed = false;

        public ReadableByteChannelInputStream(ReadableByteChannel channel, AtomicInteger openStreams, RambunctiousRunnable closer) {
            this.channel = channel;
            this.closer = closer;
            this.openStreams = openStreams;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }


        private int read(ByteBuffer buffer) throws IOException {
            return channel.read(buffer);
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1);
            while (true) {
                int read = read(buffer);
                if (read == -1) {
                    return -1;
                }
                if (read == 1) {
                    return buffer.get(0);
                }
            }
        }

        @Override
        public int read(byte @NotNull [] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) throws IOException {
            ensureOpen();
            ByteBuffer buffer = ByteBuffer.allocateDirect(len);
            int read = read(buffer);
            if (read == -1) {
                return -1;
            }
            buffer.get(0, b, off, Math.min(len, read));
            return read;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;

            if (openStreams.decrementAndGet() == 0) {
                closer.close();
            }
            ;
        }
    }
}
