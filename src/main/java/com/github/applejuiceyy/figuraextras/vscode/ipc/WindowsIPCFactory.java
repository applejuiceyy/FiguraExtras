package com.github.applejuiceyy.figuraextras.vscode.ipc;

import com.github.applejuiceyy.figuraextras.vscode.ReceptionistServer;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

public class WindowsIPCFactory extends IPCFactory {

    public WindowsIPCFactory() {

    }

    private static String toNamedPipePath(String path) {
        return "\\\\.\\pipe\\" + path;
    }

    private static void raiseCode(int code) throws CodeIOException {
        throw new CodeIOException(code);
    }

    static WinBase.OVERLAPPED createOverlappedWithEvent() {
        WinNT.HANDLE event = Kernel32.INSTANCE.CreateEvent(
                null,
                true,
                false,
                null
        );
        WinBase.OVERLAPPED overlapped = new WinBase.OVERLAPPED();
        overlapped.hEvent = event;
        return overlapped;
    }

    static OptionalInt handleOverlapped(WinNT.HANDLE handle, WinBase.OVERLAPPED overlapped) {
        return handleOverlapped(handle, overlapped, new IntByReference());
    }

    static OptionalInt handleOverlapped(WinNT.HANDLE handle, WinBase.OVERLAPPED overlapped, IntByReference ref) {
        int code = Kernel32.INSTANCE.GetLastError();
        if (code == Kernel32.ERROR_IO_PENDING) {
            if (MoreKernel32.INSTANCE.GetOverlappedResult(handle, overlapped, ref, true)) {
                Kernel32.INSTANCE.ResetEvent(overlapped.hEvent);
                return OptionalInt.empty();
            } else {
                code = Kernel32.INSTANCE.GetLastError();
                Kernel32.INSTANCE.ResetEvent(overlapped.hEvent);
                return OptionalInt.of(code);
            }
        }
        return OptionalInt.of(code);
    }

    static OptionalInt handleOverlapped(boolean success, WinNT.HANDLE handle, WinBase.OVERLAPPED overlapped) {
        return handleOverlapped(success, handle, overlapped, new IntByReference());
    }

    static OptionalInt handleOverlapped(boolean success, WinNT.HANDLE handle, WinBase.OVERLAPPED overlapped, IntByReference ref) {
        if (success) {
            return OptionalInt.empty();
        }
        return handleOverlapped(handle, overlapped, ref);
    }

    static void destroyOverlapped(WinBase.OVERLAPPED overlapped) {
        Kernel32.INSTANCE.CloseHandle(overlapped.hEvent);
        overlapped.clear();
    }

    @Override
    public boolean exists(String path) {
        return Kernel32.INSTANCE.WaitNamedPipe(toNamedPipePath(path), 10);
    }

    @Override
    public IPC createServer(String path) {
        return new WindowsIPC(path);
    }

    @Override
    public Tuple<InputStream, OutputStream> connectAsClient(String path) throws CodeIOException {
        String name = toNamedPipePath(path);
        WinNT.HANDLE handle;

        if (!Kernel32.INSTANCE.WaitNamedPipe(name, 10000)) {
            int code = Kernel32.INSTANCE.GetLastError();
            if (code == Kernel32.ERROR_FILE_NOT_FOUND) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (!Kernel32.INSTANCE.WaitNamedPipe(name, 10000)) {
                    raiseCode(Kernel32.INSTANCE.GetLastError());
                }
            } else {
                raiseCode(code);
            }
        }

        while (true) {
            handle = Kernel32.INSTANCE.CreateFile(
                    name,
                    Kernel32.GENERIC_READ | Kernel32.GENERIC_WRITE,
                    0,
                    null,
                    Kernel32.OPEN_EXISTING,
                    Kernel32.FILE_FLAG_OVERLAPPED,
                    null
            );
            if (handle == WinBase.INVALID_HANDLE_VALUE) {
                int code = Kernel32.INSTANCE.GetLastError();
                raiseCode(code);
            }
            break;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        AtomicInteger refs = new AtomicInteger(2);
        NamedPipeInputStream namedPipeInputStream = new NamedPipeInputStream(handle, refs);
        NamedPipeOutputStream namedPipeOutputStream = new NamedPipeOutputStream(handle, refs);
        return new Tuple<>(namedPipeInputStream, namedPipeOutputStream);
    }


    public interface MoreKernel32 extends Kernel32 {
        MoreKernel32 INSTANCE = Native.load("kernel32", MoreKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean GetOverlappedResult(HANDLE file, OVERLAPPED overlapped, IntByReference bytesTransferred, boolean wait);

        // these exist in the original Kernel32 but lpBuffer has been changed

        boolean WriteFile(HANDLE hFile, Memory lpBuffer, int nNumberOfBytesToWrite,
                          IntByReference lpNumberOfBytesWritten,
                          OVERLAPPED lpOverlapped);

        boolean ReadFile(HANDLE hFile, Memory lpBuffer, int nNumberOfBytesToRead,
                         IntByReference lpNumberOfBytesRead, OVERLAPPED lpOverlapped);
    }

    private static class WindowsIPC extends IPC {

        private final String path;

        public WindowsIPC(String path) {
            this.path = path;
        }

        @Override
        public Tuple<InputStream, OutputStream> connect(boolean awaitIfFull) throws CodeIOException {
            WinNT.HANDLE handle;
            while (true) {

                handle = Kernel32.INSTANCE.CreateNamedPipe(
                        toNamedPipePath(path),
                        WinBase.PIPE_ACCESS_DUPLEX | Kernel32.FILE_FLAG_OVERLAPPED,        // dwOpenMode
                        WinBase.PIPE_TYPE_BYTE | WinBase.PIPE_READMODE_BYTE | WinBase.PIPE_WAIT | WinBase.PIPE_REJECT_REMOTE_CLIENTS,    // dwPipeMode
                        Kernel32.PIPE_UNLIMITED_INSTANCES,    // nMaxInstances,
                        Byte.MAX_VALUE,    // nOutBufferSize,
                        Byte.MAX_VALUE,    // nInBufferSize,
                        1000,    // nDefaultTimeOut,
                        null
                );
                if (handle == WinBase.INVALID_HANDLE_VALUE) {
                    int code = Kernel32.INSTANCE.GetLastError();
                    if (awaitIfFull && code == Kernel32.ERROR_PIPE_BUSY) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }
                    raiseCode(code);
                }
                break;
            }
            WinBase.OVERLAPPED overlapped = createOverlappedWithEvent();
            OptionalInt result = handleOverlapped(Kernel32.INSTANCE.ConnectNamedPipe(handle, overlapped), handle, overlapped);
            if (result.isPresent()) {
                int code = result.getAsInt();
                if (code != Kernel32.ERROR_PIPE_CONNECTED) {
                    Kernel32.INSTANCE.CloseHandle(handle);
                    raiseCode(code);
                }
            }
            destroyOverlapped(overlapped);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            AtomicInteger refs = new AtomicInteger(2);
            NamedPipeInputStream namedPipeInputStream = new NamedPipeInputStream(handle, refs);
            NamedPipeOutputStream namedPipeOutputStream = new NamedPipeOutputStream(handle, refs);
            return new Tuple<>(namedPipeInputStream, namedPipeOutputStream);
        }
    }

    static class NamedPipeOutputStream extends OutputStream {

        private final WinNT.HANDLE handle;
        private boolean closed = false;

        private AtomicInteger refs;

        public NamedPipeOutputStream(WinNT.HANDLE handle) {
            this(handle, new AtomicInteger(1));
        }

        public NamedPipeOutputStream(WinNT.HANDLE handle, AtomicInteger refs) {
            this.handle = handle;
            this.refs = refs;

        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b});
        }

        @Override
        public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException {
            Memory memory = new Memory(len);
            memory.write(0, b, off, len);

            WinBase.OVERLAPPED overlapped = createOverlappedWithEvent();
            OptionalInt result = handleOverlapped(
                    MoreKernel32.INSTANCE.WriteFile(handle, memory, len, null, overlapped),
                    handle, overlapped
            );
            memory.close();
            destroyOverlapped(overlapped);
            if (result.isPresent()) {
                int code = result.getAsInt();
                if (code == Kernel32.ERROR_BROKEN_PIPE) {
                    close();
                    ensureOpen();
                }
                raiseCode(code);
            }
        }

        @Override
        public void close() throws IOException {
            ensureOpen();
            closed = true;
            if (refs.getAndDecrement() == 1) {
                if (!Kernel32.INSTANCE.CloseHandle(handle)) {
                    raiseCode(Kernel32.INSTANCE.GetLastError());
                }
            }
        }
    }

    static class NamedPipeInputStream extends InputStream {
        private final WinNT.HANDLE handle;
        private boolean closed = false;
        private boolean invalidated = false;
        private AtomicInteger refs;

        public NamedPipeInputStream(WinNT.HANDLE handle) {
            this(handle, new AtomicInteger(1));
        }

        public NamedPipeInputStream(WinNT.HANDLE handle, AtomicInteger refs) {
            this.handle = handle;
            this.refs = refs;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            if (invalidated) return -1;
            byte[] bytes = new byte[1];
            if (read(bytes) == -1) {
                return -1;
            }
            ;
            return bytes[0];
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) throws IOException {
            ensureOpen();
            if (len == 0) return 0;
            if (invalidated) return -1;

            Memory memory = new Memory(len);

            IntByReference intPointer = new IntByReference(0);
            WinBase.OVERLAPPED overlapped = createOverlappedWithEvent();
            OptionalInt result = handleOverlapped(MoreKernel32.INSTANCE.ReadFile(handle, memory, len, intPointer, overlapped), handle, overlapped, intPointer);
            destroyOverlapped(overlapped);
            if (result.isPresent()) {
                int code = result.getAsInt();
                if (code == Kernel32.ERROR_BROKEN_PIPE) {
                    invalidated = true;
                    return -1;
                }
                raiseCode(code);
            }

            memory.read(off, b, 0, len);
            memory.close();

            return intPointer.getValue();
        }

        @Override
        public void close() throws IOException {
            ensureOpen();
            closed = true;
            if (refs.getAndDecrement() == 1) {
                Kernel32.INSTANCE.CloseHandle(handle);
            }
        }
    }

    static class CodeIOException extends IOException {

        public int code;

        public CodeIOException(int code) {
            super("Code " + code + ": " + Kernel32Util.formatMessage(code));
            this.code = code;
        }

        public CodeIOException(int code, Throwable cause) {
            super("Code " + code + ": " + Kernel32Util.formatMessage(code), cause);
            this.code = code;
        }
    }
}
