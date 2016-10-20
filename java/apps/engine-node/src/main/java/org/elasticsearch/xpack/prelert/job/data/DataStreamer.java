
package org.elasticsearch.xpack.prelert.job.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.JobInUseException;
import org.elasticsearch.xpack.prelert.job.exceptions.LicenseViolationException;
import org.elasticsearch.xpack.prelert.job.exceptions.TooManyJobsException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.NativeProcessRunException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;

public class DataStreamer {
    private static final Logger LOGGER = Loggers.getLogger(DataStreamer.class);

    private final DataProcessor dataProccesor;

    public DataStreamer(DataProcessor dataProcessor) {
        dataProccesor = Objects.requireNonNull(dataProcessor);
    }

    /**
     * Stream the data to the native process.
     *
     * @param contentEncoding
     * @param jobId
     * @param input
     * @param params
     * @return Count of records, fields, bytes, etc written
     * @throws IOException
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws MissingFieldException
     * @throws JobInUseException                      if the data cannot be written to because
     *                                                the job is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws LicenseViolationException              If the license is violated
     * @throws MalformedJsonException                 If JSON data is malformed and we cannot recover
     */
    public DataCounts streamData(String contentEncoding, String jobId, InputStream input, DataLoadParams params)
            throws IOException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException, JobException {
        LOGGER.trace("Handle Post data to job {} ", jobId);

        input = tryDecompressingInputStream(contentEncoding, jobId, input);
        DataCounts stats = handleStream(jobId, input, params);

        LOGGER.debug("Data uploaded to job {}", jobId);

        return stats;
    }

    private InputStream tryDecompressingInputStream(String contentEncoding, String jobId, InputStream input) throws IOException, JobException {
        if ("gzip".equals(contentEncoding)) {
            LOGGER.debug("Decompressing post data in job {}", jobId);
            try {
                return new GZIPInputStream(input);
            } catch (ZipException ze) {
                LOGGER.error("Failed to decompress data file", ze);
                throw new JobException(Messages.getMessage(Messages.REST_GZIP_ERROR), ErrorCodes.UNCOMPRESSED_DATA);
            }
        }
        return input;
    }

    /**
     * Pass the data stream to the native process.
     *
     * @param jobId
     * @param input
     * @param params
     * @return Count of records, fields, bytes, etc written
     * @throws NativeProcessRunException              If there is an error starting the native
     *                                                process
     * @throws UnknownJobException                    If the jobId is not recognised
     * @throws MissingFieldException                  If a configured field is missing from
     *                                                the CSV header
     * @throws JsonParseException
     * @throws JobInUseException                      if the data cannot be written to because
     *                                                the job is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws LicenseViolationException              If the license is violated
     * @throws TooManyJobsException                   If too many jobs for the number of CPU cores
     * @throws MalformedJsonException                 If JSON data is malformed and we cannot recover
     */
    private DataCounts handleStream(String jobId, InputStream input, DataLoadParams params) throws
            NativeProcessRunException, UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, TooManyJobsException,
            MalformedJsonException {
        return dataProccesor.processData(jobId, input, params);
    }
}
