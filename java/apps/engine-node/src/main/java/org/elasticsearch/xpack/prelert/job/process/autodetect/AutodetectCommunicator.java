package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.ResultsReader;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.DataToProcessWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.DataToProcessWriterFactory;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.process.normalizer.noop.NoOpRenormaliser;
import org.elasticsearch.xpack.prelert.job.status.CountingInputStream;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;

public class AutodetectCommunicator implements Closeable {

    private static final int DEFAULT_TRY_COUNT = 5;
    private static final int DEFAULT_TRY_TIMEOUT_SECS = 6;

    private final AutodetectProcess autodetectProcess;
    private final Logger jobLogger;
    private final DataToProcessWriter autoDetectWriter;
    private final ResultsReader resultsReader;
    private final Thread outputParserThread;
    private final StatusReporter statusReporter;


    public AutodetectCommunicator(JobDetails jobDetails, AutodetectProcess process, Logger jobLogger,
                                  JobResultsPersister resultsPersister, StatusReporter statusReporter) {
        this.autodetectProcess = process;
        this.jobLogger = jobLogger;
        this.statusReporter = statusReporter;

        // TODO Port the normalizer from the old project
        this.resultsReader = new ResultsReader(new NoOpRenormaliser(), resultsPersister, process.out(), this.jobLogger,
                jobDetails.getAnalysisConfig().getUsePerPartitionNormalization());

        // NORELEASE - use ES ThreadPool
        this.outputParserThread = new Thread(resultsReader, jobDetails.getId() + "-Bucket-Parser");
        this.outputParserThread.start();

        this.autoDetectWriter = createProcessWriter(jobDetails, process, statusReporter);
    }

    AutodetectCommunicator(JobDetails jobDetails, AutodetectProcess process, Logger jobLogger,
                            StatusReporter statusReporter, ResultsReader resultsReader) {
        this.autodetectProcess = process;
        this.jobLogger = jobLogger;
        this.statusReporter = statusReporter;
        this.resultsReader = resultsReader;
        // NORELEASE - use ES ThreadPool
        this.outputParserThread = new Thread(resultsReader, jobDetails.getId() + "-Bucket-Parser");
        this.outputParserThread.start();

        this.autoDetectWriter = createProcessWriter(jobDetails, process, statusReporter);
    }

    private DataToProcessWriter createProcessWriter(JobDetails jobDetails, AutodetectProcess process, StatusReporter statusReporter) {
        return DataToProcessWriterFactory.create(true, process, jobDetails.getDataDescription(), jobDetails.getAnalysisConfig(),
                jobDetails.getSchedulerConfig(), new TransformConfigs(jobDetails.getTransforms()) , statusReporter, jobLogger);
    }

    public DataCounts writeToJob(InputStream inputStream)
            throws MalformedJsonException, MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {

        CountingInputStream countingStream = new CountingInputStream(inputStream, statusReporter);
        DataCounts results = autoDetectWriter.write(countingStream);
        autoDetectWriter.flush();
        return results;
    }

    @Override
    public void close() throws IOException {
        autodetectProcess.close();
        waitForResultsParser();
    }

    private void waitForResultsParser() {
        try {
            outputParserThread.join();
        } catch (InterruptedException e) {
            jobLogger.error("Error joining parser thread", e);
        }
    }

    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
        autodetectProcess.writeResetBucketsControlMessage(params);
    }

    public void writeUpdateConfigMessage(String config) throws IOException {
        autodetectProcess.writeUpdateConfigMessage(config);
    }

    public void flushJob(InterimResultsParams params) throws IOException {
        flushJob(params, DEFAULT_TRY_COUNT, DEFAULT_TRY_TIMEOUT_SECS);
    }

    void flushJob(InterimResultsParams params, int tryCount, int tryTimeoutSecs) throws IOException {
        String flushId = autodetectProcess.flushJob(params);

        Duration intermittentTimeout = Duration.ofSeconds(tryTimeoutSecs);
        boolean isFlushComplete = false;
        while (isFlushComplete == false && --tryCount >= 0) {
            // Check there wasn't an error in the flush
            if (!autodetectProcess.isProcessAlive()) {

                String msg = Messages.getMessage(Messages.AUTODETECT_FLUSH_UNEXPTECTED_DEATH) + " " + autodetectProcess.readError();
                jobLogger.error(msg);
                throw ExceptionsHelper.nativeProcessException(msg, ErrorCodes.NATIVE_PROCESS_ERROR);
            }
            isFlushComplete = resultsReader.waitForFlushAcknowledgement(flushId, intermittentTimeout);
        }

        if (!isFlushComplete) {
            String msg = Messages.getMessage(Messages.AUTODETECT_FLUSH_TIMEOUT) + " " + autodetectProcess.readError();
            jobLogger.error(msg);
            throw ExceptionsHelper.nativeProcessException(msg, ErrorCodes.NATIVE_PROCESS_ERROR);
        }

        // We also have to wait for the normaliser to become idle so that we block
        // clients from querying results in the middle of normalisation.
        resultsReader.waitUntilRenormaliserIsIdle();
    }

    public void addAlertObserver(AlertObserver ao) {
        resultsReader.addAlertObserver(ao);
    }

    public boolean removeAlertObserver(AlertObserver ao) {
        return resultsReader.removeAlertObserver(ao);
    }

    public ZonedDateTime getProcessStartTime() {
        return autodetectProcess.getProcessStartTime();
    }

}
