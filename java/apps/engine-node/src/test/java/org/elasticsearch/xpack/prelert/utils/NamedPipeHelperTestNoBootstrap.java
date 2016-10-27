/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (C) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained therein, is the
 * exclusive property of Elasticsearch BV and its licensors, if any, and is
 * protected under applicable domestic and foreign law, and international
 * treaties. Reproduction, republication or distribution without the express
 * written consent of Elasticsearch BV is strictly prohibited.
 */

package org.elasticsearch.xpack.prelert.utils;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import org.apache.lucene.util.Constants;
import org.apache.lucene.util.LuceneTestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;


/**
 * Covers positive test cases for create named pipes, which are not possible in Java with
 * the Elasticsearch security manager configuration or seccomp.  This is why the class extends
 * LuceneTestCase rather than ESTestCase.
 *
 * The way that pipes are managed in this class, e.g. using the mkfifo shell command, is
 * not suitable for production, but adequate for this test.
 */
public class NamedPipeHelperTestNoBootstrap extends LuceneTestCase {

    private static final String HELLO_WORLD = "Hello, world!";
    private static final String GOODBYE_WORLD = "Goodbye, world!";

    private static final int BUFFER_SIZE = 4096;

    private static final long PIPE_ACCESS_OUTBOUND = 2;
    private static final long PIPE_ACCESS_INBOUND = 1;
    private static final long PIPE_TYPE_BYTE = 0;
    private static final long PIPE_WAIT = 0;
    private static final long PIPE_REJECT_REMOTE_CLIENTS = 8;
    private static final long NMPWAIT_USE_DEFAULT_WAIT = 0;

    private static final Pointer INVALID_HANDLE_VALUE = Pointer.createConstant(Pointer.SIZE == 8 ? -1 : 0xFFFFFFFFL);

    /**
     * Try to ensure we'll generate different pipe names if multiple invocations of this test
     * run on the same machine at the same time.  The assumption is that the OS will cycle
     * through ephemeral ports so different processes that run around the same time will get
     * different numbers here.
     */
    private static final int TEST_ID;
    static {
        int port;
        try (ServerSocket sock = new ServerSocket(0)) {
            port = sock.getLocalPort();
        } catch (IOException e) {
            port = -1;
        }
        TEST_ID = port;

        // Have to use JNA for Windows named pipes
        if (Constants.WINDOWS) {
            Native.register("kernel32");
        }
    }

    public static class DWord extends IntegerType {

        public DWord() {
            super(4, 0, true);
        }

        public DWord(long val) {
            super(4, val, true);
        }
    }

    // https://msdn.microsoft.com/en-us/library/windows/desktop/aa365150(v=vs.85).aspx
    private static native Pointer CreateNamedPipeW(WString name, DWord openMode, DWord pipeMode, DWord maxInstances, DWord outBufferSize,
                                                   DWord inBufferSize, DWord defaultTimeOut, Pointer securityAttributes);

    // https://msdn.microsoft.com/en-us/library/windows/desktop/aa365146(v=vs.85).aspx
    private static native boolean ConnectNamedPipe(Pointer handle, Pointer overlapped);

    // https://msdn.microsoft.com/en-us/library/windows/desktop/ms724211(v=vs.85).aspx
    private static native boolean CloseHandle(Pointer handle);

    // https://msdn.microsoft.com/en-us/library/windows/desktop/aa365467(v=vs.85).aspx
    private static native boolean ReadFile(Pointer handle, Pointer buffer, DWord numberOfBytesToRead, IntByReference numberOfBytesRead,
                                           Pointer overlapped);

    // https://msdn.microsoft.com/en-us/library/windows/desktop/aa365747(v=vs.85).aspx
    private static native boolean WriteFile(Pointer handle, Pointer buffer, DWord numberOfBytesToWrite, IntByReference numberOfBytesWritten,
                                            Pointer overlapped);

    private static Pointer createPipe(String pipeName, boolean forWrite) throws IOException, InterruptedException {
        if (Constants.WINDOWS) {
            return createPipeWindows(pipeName, forWrite);
        }
        createPipeUnix(pipeName);
        // This won't be used in the *nix version
        return INVALID_HANDLE_VALUE;
    }

    private static void createPipeUnix(String pipeName) throws IOException, InterruptedException {
        if (Runtime.getRuntime().exec("mkfifo " + pipeName).waitFor() != 0) {
            throw new IOException("mkfifo failed for pipe " + pipeName);
        }
    }

    private static Pointer createPipeWindows(String pipeName, boolean forWrite) throws IOException {
        Pointer handle = CreateNamedPipeW(new WString(pipeName), new DWord(forWrite ? PIPE_ACCESS_OUTBOUND : PIPE_ACCESS_INBOUND),
                new DWord(PIPE_TYPE_BYTE | PIPE_WAIT | PIPE_REJECT_REMOTE_CLIENTS), new DWord(1),
                new DWord(BUFFER_SIZE), new DWord(BUFFER_SIZE), new DWord(NMPWAIT_USE_DEFAULT_WAIT), Pointer.NULL);
        if (INVALID_HANDLE_VALUE.equals(handle)) {
            throw new IOException("CreateNamedPipeW failed for pipe " + pipeName + " with error " + Native.getLastError());
        }
        return handle;
    }

    private static String readLineFromPipe(String pipeName, Pointer handle) throws IOException {
        if (Constants.WINDOWS) {
            return readLineFromPipeWindows(pipeName, handle);
        }
        return readLineFromPipeUnix(pipeName);
    }

    private static String readLineFromPipeUnix(String pipeName) throws IOException {
        return Files.readAllLines(Paths.get(pipeName), StandardCharsets.UTF_8).get(0);
    }

    private static String readLineFromPipeWindows(String pipeName, Pointer handle) throws IOException {
        if (!ConnectNamedPipe(handle, Pointer.NULL)) {
            throw new IOException("ConnectNamedPipe failed for pipe " + pipeName + " with error " + Native.getLastError());
        }
        IntByReference numberOfBytesRead = new IntByReference();
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        if (!ReadFile(handle, Native.getDirectBufferPointer(buf), new DWord(BUFFER_SIZE), numberOfBytesRead, Pointer.NULL)) {
            throw new IOException("ReadFile failed for pipe " + pipeName + " with error " + Native.getLastError());
        }
        byte[] content = new byte[numberOfBytesRead.getValue()];
        buf.get(content);
        String line = new String(content, StandardCharsets.UTF_8);
        int newlinePos = line.indexOf('\n');
        if (newlinePos == -1) {
            return line;
        }
        return line.substring(0, newlinePos);
    }

    private static void writeLineToPipe(String pipeName, Pointer handle, String line) throws IOException {
        if (Constants.WINDOWS) {
            writeLineToPipeWindows(pipeName, handle, line);
        } else {
            writeLineToPipeUnix(pipeName, line);
        }
    }

    private static void writeLineToPipeUnix(String pipeName, String line) throws IOException {
        Files.write(Paths.get(pipeName), (line + '\n').getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
    }

    private static void writeLineToPipeWindows(String pipeName, Pointer handle, String line) throws IOException {
        if (!ConnectNamedPipe(handle, Pointer.NULL)) {
            throw new IOException("ConnectNamedPipe failed for pipe " + pipeName + " with error " + Native.getLastError());
        }
        IntByReference numberOfBytesWritten = new IntByReference();
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        buf.put((line + '\n').getBytes(StandardCharsets.UTF_8));
        if (!WriteFile(handle, Native.getDirectBufferPointer(buf), new DWord(buf.position()), numberOfBytesWritten, Pointer.NULL)) {
            throw new IOException("WriteFile failed for pipe " + pipeName + " with error " + Native.getLastError());
        }
    }

    private static void deletePipe(String pipeName, Pointer handle) throws IOException {
        if (Constants.WINDOWS) {
            deletePipeWindows(pipeName, handle);
        } else {
            deletePipeUnix(pipeName);
        }
    }

    private static void deletePipeUnix(String pipeName) throws IOException {
        Files.delete(Paths.get(pipeName));
    }

    private static void deletePipeWindows(String pipeName, Pointer handle) throws IOException {
        if (!CloseHandle(handle)) {
            throw new IOException("CloseHandle failed for pipe " + pipeName + " with error " + Native.getLastError());
        }
    }

    private static class PipeReaderServer extends Thread {

        private String pipeName;
        private String line;
        private Exception exception;

        public PipeReaderServer(String pipeName) {
            this.pipeName = pipeName;
        }

        public void run() {
            Pointer handle = INVALID_HANDLE_VALUE;
            try {
                handle = createPipe(pipeName, false);
                line = readLineFromPipe(pipeName, handle);
            }
            catch (IOException | InterruptedException e) {
                exception = e;
            }
            try {
                deletePipe(pipeName, handle);
            } catch (IOException e) {
                // Ignore it if the previous block caught an exception, as this probably means we failed to create the pipe
                if (exception == null) {
                    exception = e;
                }
            }
        }

        public String getLine() {
            return line;
        }

        public Exception getException() {
            return exception;
        }
    }

    private static class PipeWriterServer extends Thread {

        private String pipeName;
        private String line;
        private Exception exception;

        public PipeWriterServer(String pipeName, String line) {
            this.pipeName = pipeName;
            this.line = line;
        }

        public void run() {
            Pointer handle = INVALID_HANDLE_VALUE;
            try {
                handle = createPipe(pipeName, true);
                writeLineToPipe(pipeName, handle, line);
            } catch (IOException | InterruptedException e) {
                exception = e;
            }
            try {
                deletePipe(pipeName, handle);
            } catch (IOException e) {
                // Ignore it if the previous block caught an exception, as this probably means we failed to create the pipe
                if (exception == null) {
                    exception = e;
                }
            }
        }

        public Exception getException() {
            return exception;
        }
    }

    public void testOpenForInput() throws IOException, InterruptedException {
        String pipeName = NamedPipeHelper.getDefaultPipeDirectoryPrefix() + "inputPipe" + TEST_ID;

        PipeWriterServer server = new PipeWriterServer(pipeName, HELLO_WORLD);
        server.start();
        try {
            InputStream is = NamedPipeHelper.openNamedPipeInputStream(pipeName, Duration.ofSeconds(1));
            assertNotNull(is);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                assertEquals(HELLO_WORLD, line);
            }
        } finally {
            server.join();
        }

        assertNull(server.getException());
    }

    public void testOpenForOutput() throws IOException, InterruptedException {
        String pipeName = NamedPipeHelper.getDefaultPipeDirectoryPrefix() + "outputPipe" + TEST_ID;

        PipeReaderServer server = new PipeReaderServer(pipeName);
        server.start();
        try {
            OutputStream os = NamedPipeHelper.openNamedPipeOutputStream(pipeName, Duration.ofSeconds(1));
            assertNotNull(os);

            try (OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write(GOODBYE_WORLD);
                writer.write('\n');
            }
        } finally {
            server.join();
        }

        assertNull(server.getException());
        assertEquals(GOODBYE_WORLD, server.getLine());
    }
}
