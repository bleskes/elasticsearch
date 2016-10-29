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

import org.apache.lucene.util.Constants;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.env.Environment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;


/**
 * Opens named pipes that are created elsewhere.
 *
 * In production, these will have been created in C++ code, as the procedure for creating them is
 * platform dependent and uses native OS calls that are not easily available in Java.
 *
 * Once the named pipes have been created elsewhere Java can open them like normal files, however,
 * there are complications:
 * - On *nix, when opening a pipe for output Java will create a normal file of the requested name
 *   if the named pipe doesn't already exist.  To avoid this, named pipes are only opened for
 *   for output once the expected file name exists in the file system.
 * - On Windows, the server end of a pipe needs to reset it between client connects.  Methods like
 *   File.isFile() and File.exists() on Windows internally call the Win32 API function CreateFile()
 *   followed by GetFileInformationByHandle(), and if the CreateFile() succeeds it counts as opening
 *   the named pipe, requiring it to be reset on the server side before subsequent access.  To avoid
 *   this, the check for whether a given path represents a named pipe is done using simple string
 *   comparison on Windows.
 */
public class NamedPipeHelper {

    /**
     * Try this often to open named pipes that we're waiting on another process to create.
     */
    private static final long PAUSE_TIME_MS = 20;

    /**
     * On Windows named pipes are ALWAYS accessed via this path; it is impossible to put them
     * anywhere else.
     */
    private static final String WIN_PIPE_PREFIX = "\\\\.\\pipe\\";

    private NamedPipeHelper() {
        // Do nothing
    }

    /**
     * The default path where named pipes will be created.  On *nix they can be created elsewhere
     * (subject to security manager constraints), but on Windows this is the ONLY place they can
     * be created.
     * @return The directory prefix as a string.
     */
    public static String getDefaultPipeDirectoryPrefix() {
        // The return type is String because we don't want any (too) clever path processing removing
        // the seemingly pointless . in the path used on Windows.
        if (Constants.WINDOWS) {
            return WIN_PIPE_PREFIX;
        }
        // Use the Java temporary directory.  The Elasticsearch bootstrap sets up the security
        // manager to allow this to be read from and written to.  Also, the code that spawns our
        // daemon passes on this location to the C++ code using the $TMPDIR environment variable.
        // All these factors need to align for everything to work in production.  If any changes
        // are made here then CNamedPipeFactory::defaultPath() in the C++ code will probably
        // also need to be changed.
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir.endsWith(PathUtils.getDefaultFileSystem().getSeparator())) {
            return tmpDir;
        }
        return tmpDir + PathUtils.getDefaultFileSystem().getSeparator();
    }

    /**
     * Open a named pipe created elsewhere for input.
     * 
     * @param path
     *            Path of named pipe to open.
     * @param timeout
     *            How long to wait for the named pipe to exist.
     * @return A stream opened to read from the named pipe.
     * @throws IOException
     *             if the named pipe cannot be opened.
     */
    public static InputStream openNamedPipeInputStream(Environment env, Path path, Duration timeout) throws IOException {
        return openNamedPipeInputStream(path, timeout);
    }

    /**
     * Open a named pipe created elsewhere for input.
     * @param file The named pipe to open.
     * @param timeout How long to wait for the named pipe to exist.
     * @return A stream opened to read from the named pipe.
     * @throws IOException if the named pipe cannot be opened.
     */
    public static InputStream openNamedPipeInputStream(Path file, Duration timeout) throws IOException {
        long timeoutMillisRemaining = timeout.toMillis();

        // Can't use File.isFile() on Windows, but luckily there's an even simpler check (that's not possible on *nix)
        if (Constants.WINDOWS && !file.toString().startsWith(WIN_PIPE_PREFIX)) {
            throw new IOException(file + " is not a named pipe");
        }

        // Try to open the file periodically until the timeout expires, then, if
        // it's still not available throw the exception from FileInputStream
        while (true) {
            // On Windows File.isFile() will render a genuine named pipe unusable
            if (!Constants.WINDOWS && Files.isRegularFile(file)) {
                throw new IOException(file + " is not a named pipe");
            }
            try {
                return Files.newInputStream(file);
            } catch (IOException ioe) {
                if (timeoutMillisRemaining <= 0) {
                    throw ioe;
                }
                long thisSleep = Math.min(timeoutMillisRemaining, PAUSE_TIME_MS);
                timeoutMillisRemaining -= thisSleep;
                try {
                    Thread.sleep(thisSleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ioe;
                }
            }
        }
    }

    /**
     * Open a named pipe created elsewhere for output.
     *
     * @param path
     *            Path of named pipe to open.
     * @param timeout
     *            How long to wait for the named pipe to exist.
     * @return A stream opened to read from the named pipe.
     * @throws IOException
     *             if the named pipe cannot be opened.
     */
    public static OutputStream openNamedPipeOutputStream(Environment env, Path path, Duration timeout) throws IOException {
        return openNamedPipeOutputStream(path, timeout);
    }

    /**
     * Open a named pipe created elsewhere for output.
     * @param file The named pipe to open.
     * @param timeout How long to wait for the named pipe to exist.
     * @return A stream opened to read from the named pipe.
     * @throws IOException if the named pipe cannot be opened.
     */
    public static OutputStream openNamedPipeOutputStream(Path file, Duration timeout) throws IOException {
        if (Constants.WINDOWS) {
            return openNamedPipeOutputStreamWindows(file, timeout);
        }
        return openNamedPipeOutputStreamUnix(file, timeout);
    }

    /**
     * The logic here is very similar to that of opening an input stream, because on Windows
     * Java cannot create a regular file when asked to open a named pipe that doesn't exist.
     * @param file The named pipe to open.
     * @param timeout How long to wait for the named pipe to exist.
     * @return A stream opened to read from the named pipe.
     * @throws IOException if the named pipe cannot be opened.
     */
    private static OutputStream openNamedPipeOutputStreamWindows(Path file, Duration timeout) throws IOException {
        long timeoutMillisRemaining = timeout.toMillis();

        // Can't use File.isFile() on Windows, but luckily there's an even simpler check (that's not possible on *nix)
        if (!file.toString().startsWith(WIN_PIPE_PREFIX)) {
            throw new IOException(file + " is not a named pipe");
        }

        // Try to open the file periodically until the timeout expires, then, if
        // it's still not available throw the exception from FileOutputStream
        while (true) {
            try {
                return Files.newOutputStream(file);
            } catch (IOException ioe) {
                if (timeoutMillisRemaining <= 0) {
                    throw ioe;
                }
                long thisSleep = Math.min(timeoutMillisRemaining, PAUSE_TIME_MS);
                timeoutMillisRemaining -= thisSleep;
                try {
                    Thread.sleep(thisSleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ioe;
                }
            }
        }
    }

    /**
     * This has to use different logic to the input pipe case to avoid the danger of creating
     * a regular file when the named pipe does not exist when the method is first called.
     * @param file The named pipe to open.
     * @param timeout How long to wait for the named pipe to exist.
     * @return A stream opened to read from the named pipe.
     * @throws IOException if the named pipe cannot be opened.
     */
    private static OutputStream openNamedPipeOutputStreamUnix(Path file, Duration timeout) throws IOException {
        long timeoutMillisRemaining = timeout.toMillis();

        // Periodically check whether the file exists until the timeout expires, then, if
        // it's still not available throw a FileNotFoundException
        while (timeoutMillisRemaining > 0 && !Files.exists(file)) {
            long thisSleep = Math.min(timeoutMillisRemaining, PAUSE_TIME_MS);
            timeoutMillisRemaining -= thisSleep;
            try {
                Thread.sleep(thisSleep);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (Files.isRegularFile(file)) {
            throw new IOException(file + " is not a named pipe");
        }

        if (!Files.exists(file)) {
            throw new FileNotFoundException("Cannot open " + file + " (No such file or directory)");
        }

        // There's a race condition here in that somebody could delete the named pipe at this point
        // causing the line below to create a regular file.  Not sure what can be done about this
        // without using low level OS calls...

        return Files.newOutputStream(file);
    }
}
