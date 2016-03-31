/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.job.logs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.settings.PrelertSettings;

/**
 * Read/Tail the logs
 */
public class JobLogs
{
    private static final Logger LOGGER = Logger.getLogger(JobLogs.class);

    private static final String DEFAULT_LOG_FILE = "autodetect_api.log";
    private static final String LOG_FILE_EXTENSION = ".log";

    /**
     * Use the expected line length to estimate how far from the
     * end N lines starts
     */
    public static final int EXPECTED_LINE_LENGTH = 132;

    /**
     * If this system property is set the log files aren't deleted when
     * the job is.
     */
    public static final String DONT_DELETE_LOGS_PROP = "preserve.logs";
    private boolean m_DontDelete;

    /**
     * If -D{@value #DONT_DELETE_LOGS_PROP} is set to anything
     * (not null) the log files aren't deleted
     */
    public JobLogs()
    {
        m_DontDelete = PrelertSettings.isSet(DONT_DELETE_LOGS_PROP);
    }

    /**
     * Read the entire contents of the file and return
     * as a string. The file should be UTF-8 encoded.
     * If <code>filename</code> does not end with {@value #LOG_FILE_EXTENSION}
     * then {@value #LOG_FILE_EXTENSION} is appended to it, this means
     * only files ending in {@value #LOG_FILE_EXTENSION} can be read.
     *
     * @param jobId
     * @param filename
     * @return
     * @throws UnknownJobException
     */
    public String file(String jobId, String filename) throws UnknownJobException
    {
        if (filename.endsWith(LOG_FILE_EXTENSION) == false)
        {
            filename = filename + LOG_FILE_EXTENSION;
        }

        Path filePath = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR,
                jobId, filename);

        try
        {
            return file(filePath);
        }
        catch (IOException e)
        {
            String msg = Messages.getMessage(Messages.LOGFILE_MISSING, filePath);
            LOGGER.warn(msg);
            throw new UnknownJobException(jobId, msg, ErrorCodes.MISSING_LOG_FILE);
        }
    }


    /**
     * Read the entire contents of the file and return
     * as a string. The file should be UTF-8 encoded.
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public String file(Path filePath) throws IOException
    {
        byte[] encoded = Files.readAllBytes(filePath);
        return new String(encoded, StandardCharsets.UTF_8);
    }


    /**
     * Return the last N lines from the file or less if the file
     * is shorter than N lines.
     *
     * @param jobId Read the default log file for this job
     * @param nLines Lines to tail
     * @return
     * @throws UnknownJobException If jobId is not recognised
     * @see {@link #tail(File, String, int, int)}
     */
    public String tail(String jobId, int nLines)
    throws UnknownJobException
    {
        return tail(jobId, DEFAULT_LOG_FILE, nLines);
    }

    /**
     * Return the last N lines from the file or less if the file
     * is shorter than N lines.
     *
     * @param jobId Read the log file for this job
     * @param nLines Lines to tail
     * @return
     * @throws UnknownJobException If jobId is not recognised
     * @see {@link #tail(File, String, int, int)}
     */
    public String tail(String jobId, String filename, int nLines)
    throws UnknownJobException
    {
        if (filename.endsWith(LOG_FILE_EXTENSION) == false)
        {
            filename = filename + LOG_FILE_EXTENSION;
        }

        File file = new File(new File(ProcessCtrl.LOG_DIR, jobId), filename);

        return tail(file, jobId, nLines, EXPECTED_LINE_LENGTH);
    }


    /**
     * Return the last N lines from the file or less if the file
     * is shorter than N lines.
     * <br>
     * The algorithm is imprecise in that it first takes a guess
     * how far back from the end of the file N lines is based on the
     * <code>expectedLineSize</code> parameter then counts the lines
     * from there returning the last N. It will iteratively go back
     * further is less than N lines are read and there is more file to
     * read.
     *
     * @param file The log file to read
     * @param jobId The job Id is only required for error reporting
     * @param nLines Lines to tail
     * @param expectedLineSize If this value is very small in relation to
     * the actual line lengths then the wrong number of lines may be returned.
     * {@linkplain #EXPECTED_LINE_LENGTH} is a good estimate
     * @return
     * @throws UnknownJobException If jobId is not recognised
     */
    public String tail(File file, String jobId, int nLines, int expectedLineSize)
    throws UnknownJobException
    {
        StringBuilder builder = new StringBuilder();
        try
        {
            RandomAccessFile logFile = new RandomAccessFile(file, "r");
            try
            {
                // go to where we think the last N lines will start
                long seek = Math.max(logFile.length() - (nLines * expectedLineSize), 0);
                logFile.seek(seek);

                // the first line is probably a partial line so discard it
                // unless we are at the beginning of the file
                if (seek > 0)
                {
                    logFile.readLine();
                }
                long lastReadLoopStartPos = logFile.getFilePointer();
                String line = logFile.readLine();


                int lineCount = 0;
                Deque<String> circularBuffer = new ArrayDeque<>(nLines);

                while (line != null)
                {
                    if (lineCount >= nLines)
                    {
                        circularBuffer.poll();
                    }
                    circularBuffer.add(line);

                    line = logFile.readLine();
                    lineCount++;
                }

                // If we don't have enough lines go back for more
                while (lineCount < nLines)
                {
                    if (seek <= 0)
                    {
                        // we cannot go back past the beginning of the file
                        break;
                    }

                    int missingLines = nLines - lineCount;
                    Deque<String> supplementQueue = new ArrayDeque<>(missingLines);

                    // seek further back into the file
                    seek = Math.max(seek - (missingLines * expectedLineSize), 0);
                    logFile.seek(seek);

                    // the first line is probably a partial line so discard it
                    // unless we are at the beginning of the file
                    if (seek > 0)
                    {
                        logFile.readLine();
                    }
                    long thisLoopStartPos = logFile.getFilePointer();
                    line = logFile.readLine();

                    // don't read past where we read from last time
                    while (line != null)
                    {
                        // Are we up to the point we started reading from last time?
                        long pos = logFile.getFilePointer();
                        if (pos > lastReadLoopStartPos)
                        {
                            break;
                        }

                        if (lineCount >= nLines)
                        {
                            supplementQueue.poll();
                        }
                        supplementQueue.add(line);

                        line = logFile.readLine();
                        lineCount++;
                    }

                    String last = supplementQueue.pollLast();
                    while(last != null)
                    {
                        circularBuffer.offerFirst(last);
                        last = supplementQueue.pollLast();
                    }

                    lastReadLoopStartPos = thisLoopStartPos;
                }

                for (String ln : circularBuffer)
                {
                    builder.append(ln).append('\n');
                }

                return builder.toString();
            }
            catch (IOException ioe)
            {
                LOGGER.error("Error tailing log file", ioe);
            }
            finally
            {
                logFile.close();
            }
        }
        catch (FileNotFoundException e)
        {
            String msg = Messages.getMessage(Messages.LOGFILE_MISSING, file);
            LOGGER.warn(msg);
            throw new UnknownJobException(jobId, msg, ErrorCodes.MISSING_LOG_FILE);
        }
        catch (IOException e)
        {
            LOGGER.error("Error closing log file", e);
        }

        return builder.toString();
    }

    /**
     * Zips the contents of the job's log directory and returns
     * as a byte array.
     *
     * @param jobId
     * @return
     * @throws UnknownJobException If jobId is not recognised
     * @see {@linkplain #zippedLogFiles(File, String)}
     */
    public byte[] zippedLogFiles(String jobId)
    throws UnknownJobException
    {
        File logDir = new File(ProcessCtrl.LOG_DIR, jobId);
        return zippedLogFiles(logDir, jobId);
    }


    /**
     * Zips the contents of <code>logDirectory</code> and
     * return as a byte array.
     *
     * @param logDirectory The directory containing the log files
     * @param root The zip file contents will be in a directory with this name
     * @return
     * @throws UnknownJobException
     */
    public byte[] zippedLogFiles(File logDirectory, String root)
    throws UnknownJobException
    {
        File[] listOfFiles = logDirectory.listFiles();

        if (listOfFiles == null)
        {
            String msg = Messages.getMessage(Messages.LOGFILE_MISSING_DIRECTORY, logDirectory);
            LOGGER.error(msg);
            throw new UnknownJobException(root, msg, ErrorCodes.CANNOT_OPEN_DIRECTORY);
        }

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(byteos))
        {
            byte [] buffer = new byte[65536];

            // add a directory
            zos.putNextEntry(new ZipEntry(root + "/"));


            addFiles(root, listOfFiles, zos, buffer);

            zos.finish();

        }
        catch (IOException e1)
        {
            LOGGER.error("Error closing Zip outputstream", e1);
        }

        return byteos.toByteArray();
    }

    private void addFiles(String root, File[] listOfFiles, ZipOutputStream zos,
            byte[] buffer)
    {
        for (File file : listOfFiles)
        {
            if (file.isDirectory())
            {
                addFiles(root + "/" + file.getName(), file.listFiles(), zos, buffer);
            }
            else
            {
                try
                {
                    FileInputStream in = new FileInputStream(file);
                    ZipEntry entry = new ZipEntry(root + "/" + file.getName());
                    zos.putNextEntry(entry);

                    int len;
                    while ((len = in.read(buffer)) > 0)
                    {
                        zos.write(buffer, 0, len);
                    }

                    in.close();
                    zos.closeEntry();
                }
                catch (FileNotFoundException e)
                {
                    LOGGER.error("Missing log file '" + file
                            + "' will not be added to zipped logs file");
                }
                catch (IOException e)
                {
                    LOGGER.error("Error zipping log file", e);
                }
            }
        }
    }


    /**
     * Delete all the log files and log directory associated with a job.
     *
     * @param jobId
     * @return true if success.
     */
    public boolean deleteLogs(String jobId)
    {
        return deleteLogs(ProcessCtrl.LOG_DIR, jobId);
    }

    /**
     * Delete all the files in the directory <pre>logDir/jobId</pre>.
     *
     * @param logDir The base directory of the log files
     * @param jobId
     * @return
     */
    public boolean deleteLogs(String logDir, String jobId)
    {
        if (m_DontDelete)
        {
            return true;
        }

        LOGGER.info(String.format("Deleting log files %s/%s", logDir, jobId));
        Path logPath = FileSystems.getDefault().getPath(logDir, jobId);

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(logPath))
        {
            for (Path logFile : directoryStream)
            {
                try
                {
                    Files.delete(logFile);
                }
                catch (IOException e)
                {
                    String msg = "Cannot delete log file " + logDir + ". ";
                    msg += (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                    LOGGER.warn(msg);
                }
            }
        }
        catch (IOException e)
        {
            String msg = "Cannot open the log directory " + logDir + ". ";
            msg += (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            LOGGER.warn(msg);
        }

        // delete the directory
        try
        {
            Files.delete(logPath);
        }
        catch (IOException e)
        {
            String msg = "Cannot delete log directory " + logDir + ". ";
            msg += (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            LOGGER.warn(msg);
            return false;
        }

        return true;
    }
}
