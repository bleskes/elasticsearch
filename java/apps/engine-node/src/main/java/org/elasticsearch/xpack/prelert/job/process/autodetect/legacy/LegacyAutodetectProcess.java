package org.elasticsearch.xpack.prelert.job.process.autodetect.legacy;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.ControlMsgToProcessWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.LengthEncodedWriter;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Autodetect process based on the old Java API process code.
 */
// NORELEASE This is a temporary solution so the integration tests can be added.
public class LegacyAutodetectProcess implements AutodetectProcess {
    private static final Logger LOGGER = Loggers.getLogger(LegacyAutodetectProcess.class);

    private final LengthEncodedWriter recordWriter;
    private final ZonedDateTime startTime;
    private final int numberOfAnalysisFields;
    private final Process nativeProcess;
    private final BufferedReader errorReader;
    private final List<Path> filesToDelete;

    public LegacyAutodetectProcess(Process nativeProcess, int numberOfAnalysisFields, List<Path> filesToDelete) {
        this.recordWriter = new LengthEncodedWriter(nativeProcess.getOutputStream());
        startTime = ZonedDateTime.now();
        this.numberOfAnalysisFields = numberOfAnalysisFields;
        this.nativeProcess = nativeProcess;
        this.filesToDelete = filesToDelete;
        errorReader = new BufferedReader(new InputStreamReader(this.nativeProcess.getErrorStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
        recordWriter.writeRecord(record);
    }

    @Override
    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(recordWriter, numberOfAnalysisFields);
        writer.writeResetBucketsMessage(params);
    }

    @Override
    public void writeUpdateConfigMessage(String config) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(recordWriter, numberOfAnalysisFields);
        writer.writeUpdateConfigMessage(config);
    }

    @Override
    public String flushJob(InterimResultsParams params) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(recordWriter, numberOfAnalysisFields);
        writer.writeCalcInterimMessage(params);
        return writer.writeFlushMessage();
    }

    @Override
    public void flushStream() throws IOException {
        recordWriter.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            // closing its input causes the process to exit
            nativeProcess.getOutputStream().close();

            // wait for the process to exit
            int exitValue = nativeProcess.waitFor();
            String msg = String.format(Locale.ROOT, "Process returned with value %d.", exitValue);
            if (exitValue != 0) {
                // Read any error output from the process
                StringBuilder sb = new StringBuilder(msg).append("\n");
                readProcessErrorOutput(sb);

                LOGGER.error(sb.toString());
                throw ExceptionsHelper.serverError(sb.toString(), ErrorCodes.NATIVE_PROCESS_ERROR);
            } else {
                LOGGER.info(msg);
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Exception closing the running native process");
            Thread.currentThread().interrupt();
        } finally {
            deleteAssociatedFiles();
        }
    }

    /**
     * Read the error output from the process into the string builder.
     *
     * @param sb This will be modified and returned.
     * @return The parameter <code>sb</code>
     */
    private StringBuilder readProcessErrorOutput(StringBuilder sb) {
        try {
            if (errorReader.ready() == false) {
                return sb;
            }

            String line;
            while ((line = errorReader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            LOGGER.warn("Exception thrown reading the native processes error output", e);
        }

        return sb;
    }

    public void deleteAssociatedFiles() throws IOException {
        if (filesToDelete == null) {
            return;
        }

        for (Path fileToDelete : filesToDelete) {
            if (Files.deleteIfExists(fileToDelete) == true) {
                LOGGER.debug("Deleted file {}", fileToDelete::toString);
            } else {
                LOGGER.warn("Failed to delete file {}", fileToDelete::toString);
            }
        }
    }

    @Override
    public InputStream error() {
        return nativeProcess.getErrorStream();
    }

    @Override
    public InputStream out() {
        return nativeProcess.getInputStream();
    }

    @Override
    public ZonedDateTime getProcessStartTime() {
        return startTime;
    }

    @Override
    public boolean isProcessAlive() {
        // Sanity check make sure the process hasn't terminated already
        try {
            int exitValue = nativeProcess.exitValue();

            // If we get here the process has exited.
            String msg = String.format(Locale.ROOT, "Process exited with code %d.", exitValue);
            LOGGER.info(msg);
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    @Override
    public String readError() {
        return readProcessErrorOutput(new StringBuilder()).toString();
    }
}
