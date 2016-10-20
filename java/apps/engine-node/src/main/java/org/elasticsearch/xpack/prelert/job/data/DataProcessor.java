package org.elasticsearch.xpack.prelert.job.data;

import com.fasterxml.jackson.core.JsonParseException;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.exceptions.JobInUseException;
import org.elasticsearch.xpack.prelert.job.exceptions.LicenseViolationException;
import org.elasticsearch.xpack.prelert.job.exceptions.TooManyJobsException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.NativeProcessRunException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;

import java.io.InputStream;

public interface DataProcessor {

    /**
     * Passes data to the native process.
     * This is a blocking call that won't return until all the data has been
     * written to the process.
     *
     * @param jobId
     * @param input
     * @param params
     * @return
     * @throws NativeProcessRunException If there is an error starting the native
     * process
     * @throws UnknownJobException If the jobId is not recognised
     * @throws MissingFieldException If a configured field is missing from
     * the CSV header
     * @throws JsonParseException
     * @throws JobInUseException if the job cannot be written to because
     * it is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws LicenseViolationException If the license is violated
     * @throws TooManyJobsException If too many jobs for the number of CPU cores
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     * @return Count of records, fields, bytes, etc written
     */
    DataCounts processData(String jobId, InputStream input, DataLoadParams params)
            throws UnknownJobException, NativeProcessRunException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, TooManyJobsException,
            MalformedJsonException;

    /**
     * Flush the running job, ensuring that the native process has had the
     * opportunity to process all data previously sent to it with none left
     * sitting in buffers.
     *
     * @param jobId The job to flush
     * @param interimResultsParams Parameters about whether interim results calculation
     * should occur and for which period of time
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if a data upload is part way through
     */
    void flushJob(String jobId, InterimResultsParams interimResultsParams)
            throws UnknownJobException, NativeProcessRunException, JobInUseException;

    /**
     * Stop the running job and mark it as finished.<br>
     *
     * @param jobId The job to stop
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if the job cannot be closed because data is
     * being streamed to it
     */
    void closeJob(String jobId) throws UnknownJobException, NativeProcessRunException, JobInUseException;
}
