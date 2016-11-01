/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

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
    private final AutodetectResultsParser parser;
    private final Renormaliser renormaliser;
    private final JobResultsPersister resultsPersister;

    public ResultsReader(Renormaliser renormaliser, JobResultsPersister persister,
            InputStream stream, Logger logger, boolean isPerPartitionNormalisation) {
        this.stream = stream;
        this.logger = logger;
        parser = new AutodetectResultsParser(isPerPartitionNormalisation);
        this.renormaliser = renormaliser;
        resultsPersister = persister;
    }

    @Override
    public void run() {
        try {
            parser.parseResults(stream, resultsPersister, renormaliser, logger);
        } catch (ElasticsearchParseException e) {
            logger.info("Error parsing autodetect output", e);
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

    /**
     * Blocks until a flush is acknowledged or the timeout expires, whichever happens first.
     *
     * @param flushId the id of the flush request to wait for
     * @param timeout the timeout
     * @return {@code true} if the flush has completed or the parsing finished; {@code false} if the timeout expired
     */
    public boolean waitForFlushAcknowledgement(String flushId, Duration timeout) {
        return parser.waitForFlushAcknowledgement(flushId, timeout);
    }

    public void waitUntilRenormaliserIsIdle() {
        renormaliser.waitUntilIdle();
    }
}

