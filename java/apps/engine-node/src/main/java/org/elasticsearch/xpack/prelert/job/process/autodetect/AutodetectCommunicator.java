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

public class AutodetectCommunicator implements Closeable {

    private final JobDetails jobDetails;
    private final AutodetectProcess autodetectProcess;
    private final Logger jobLogger;
    private final DataToProcessWriter autoDetectWriter;
    private final ResultsReader resultsReader;
    private final Thread outputParserThread;
    private final StatusReporter statusReporter;


    public AutodetectCommunicator(JobDetails jobDetails, AutodetectProcess process, Logger jobLogger,
                                  JobResultsPersister resultsPersister, StatusReporter statusReporter) {
        this.jobDetails = jobDetails;
        this.autodetectProcess = process;
        this.jobLogger = jobLogger;
        this.statusReporter = statusReporter;

        // TODO get latest process manager code from the old engine-api project
        this.resultsReader = new ResultsReader(new NoOpRenormaliser(), resultsPersister, process.out(), this.jobLogger,
                this.jobDetails.getAnalysisConfig().getUsePerPartitionNormalization());

        this.outputParserThread = new Thread(resultsReader, jobDetails.getId() + "-Bucket-Parser");
        this.outputParserThread.start();

        this.autoDetectWriter = DataToProcessWriterFactory.create(true, process, jobDetails.getDataDescription(),
                jobDetails.getAnalysisConfig(), jobDetails.getSchedulerConfig(), new TransformConfigs(jobDetails.getTransforms()),
                statusReporter, jobLogger);
    }


    public DataCounts writeToJob(InputStream inputStream)
            throws MalformedJsonException, MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {
        return autoDetectWriter.write(inputStream);
    }

    @Override
    public void close() throws IOException {
        try {
            joinResultsParserThread();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jobLogger.error("Error joining parser thread", e);
        }
    }

    private void joinResultsParserThread() throws InterruptedException {
        outputParserThread.join();
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

    public void addAlertObserver(AlertObserver alertObserver)
    {
        resultsReader.addAlertObserver(alertObserver);
    }

    public boolean removeAlertObserver(AlertObserver alertObserver)
    {
        return resultsReader.removeAlertObserver(alertObserver);
    }


}
