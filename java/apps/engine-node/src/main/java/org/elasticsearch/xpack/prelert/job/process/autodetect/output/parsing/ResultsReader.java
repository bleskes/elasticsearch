
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;

import java.io.IOException;
import java.io.InputStream;

/**
 * A runnable class that reads the autodetect process output
 * and writes the results via the {@linkplain JobResultsPersister}
 * passed in the constructor.
 * <p>
 * Has methods to register and remove alert observers.
 * Also has a method to wait for a flush to be complete.
 */
public class ResultsReader implements Runnable {
    private final InputStream stream;
    private final Logger logger;
    private final AutoDetectResultsParser parser;
    private final Renormaliser renormaliser;
    private final JobResultsPersister resultsPersister;

    public ResultsReader(Renormaliser renormaliser, JobResultsPersister persister,
                         InputStream stream, Logger logger, boolean isPerPartitionNormalisation) {
        this.stream = stream;
        this.logger = logger;
        parser = new AutoDetectResultsParser(isPerPartitionNormalisation);
        this.renormaliser = renormaliser;
        resultsPersister = persister;
    }

    @Override
    public void run() {
        try {
            parser.parseResults(stream, resultsPersister, renormaliser, logger);
        } catch (ElasticsearchParseException e) {
            logger.info("Error parsing autodetect_api output", e);
        } finally {
            try {
                // read anything left in the stream before
                // closing the stream otherwise if the process
                // tries to write more after the close it gets
                // a SIGPIPE
                byte[] buff = new byte[512];
                while (stream.read(buff) >= 0) {
                    // Do nothing
                }
                stream.close();
            } catch (IOException e) {
                logger.warn("Error closing result parser input stream", e);
            }

            // The renormaliser may have started another thread,
            // so give it a chance to shut this down
            renormaliser.shutdown(logger);
        }

        logger.info("Parse results Complete");
    }

    public void addAlertObserver(AlertObserver ao) {
        parser.addObserver(ao);
    }

    public boolean removeAlertObserver(AlertObserver ao) {
        return parser.removeObserver(ao);
    }

    public void waitForFlushComplete(String flushId)
            throws InterruptedException {
        parser.waitForFlushAcknowledgement(flushId);

        // We also have to wait for the normaliser to become idle so that we block
        // clients from querying results in the middle of normalisation.
        renormaliser.waitUntilIdle();
    }
}

