package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.ResultsReader;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.DataToProcessWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.DataToProcessWriterFactory;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.process.normalizer.noop.NoOpRenormaliser;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

public class AutodetectCommunicator implements Closeable {

    private final AutodetectProcess autodetectProcess;
    private final Logger jobLogger;
    private final DataToProcessWriter autoDetectWriter;
    private final ResultsReader resultsReader;
    private final Thread outputParserThread;


    public AutodetectCommunicator(JobDetails jobDetails, AutodetectProcess process, Logger jobLogger,
                                  JobResultsPersister resultsPersister, StatusReporter statusReporter) {
        this.autodetectProcess = process;
        this.jobLogger = jobLogger;

        // TODO get latest process manager code from the old engine-api project
        this.resultsReader = new ResultsReader(new NoOpRenormaliser(), resultsPersister, process.out(), this.jobLogger,
                jobDetails.getAnalysisConfig().getUsePerPartitionNormalization());

        // NORELEASE - use ES ThreadPool
        this.outputParserThread = new Thread(resultsReader, jobDetails.getId() + "-Bucket-Parser");
        this.outputParserThread.start();

        this.autoDetectWriter = DataToProcessWriterFactory.create(true, process, jobDetails.getDataDescription(),
                jobDetails.getAnalysisConfig(), jobDetails.getSchedulerConfig(), new TransformConfigs(jobDetails.getTransforms()),
                statusReporter, jobLogger);
    }

    public DataCounts writeToJob(InputStream inputStream)
            throws MalformedJsonException, MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {

        DataCounts results = autoDetectWriter.write(inputStream);
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
        autodetectProcess.flushJob(params);
     }

    public void addAlertObserver(AlertObserver ao)
    {
        resultsReader.addAlertObserver(ao);
    }

    public boolean removeAlertObserver(AlertObserver ao)
    {
        return resultsReader.removeAlertObserver(ao);
    }

    public ZonedDateTime getProcessStartTime() { return autodetectProcess.getProcessStartTime(); }

}
