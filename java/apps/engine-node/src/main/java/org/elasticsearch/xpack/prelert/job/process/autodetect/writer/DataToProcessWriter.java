
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import java.io.IOException;
import java.io.InputStream;

import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;

/**
 * A writer for transforming and piping data from an
 * inputstream to outputstream as the process expects.
 */
public interface DataToProcessWriter {
    /**
     * Reads the inputIndex, transform to length encoded values and pipe
     * to the OutputStream.
     * If any of the fields in <code>analysisFields</code> or the
     * <code>DataDescription</code>s timeField is missing from the CSV header
     * a <code>MissingFieldException</code> is thrown
     *
     * @return Counts of the records processed, bytes read etc
     * @throws IOException
     * @throws MissingFieldException                  If any fields are missing from the inputIndex
     * @throws HighProportionOfBadTimestampsException If a large proportion
     *                                                of the records read have missing fields
     * @throws OutOfOrderRecordsException
     * @throws MalformedJsonException                 If JSON data is malformed and we cannot recover.
     */
    DataCounts write(InputStream inputStream) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException;
}
