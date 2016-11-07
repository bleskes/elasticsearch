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
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.alert.AlertTrigger;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses the JSON output of the autodetect program.
 * <p>
 * Expects an array of buckets so the first element will always be the
 * start array symbol and the data must be terminated with the end array symbol.
 */
// NORELEASE remove this class when Jackson is gone
@SuppressForbidden(reason = "This class uses notify and wait but will be removed when jackson is gone")
public class AutodetectResultsParser {
    private final List<AlertObserver> observers = new ArrayList<>();
    private final Set<String> acknowledgedFlushes = new HashSet<>();
    private volatile boolean parsingStarted;
    private volatile boolean parsingInProgress;
    private boolean isPerPartitionNormalization;

    public AutodetectResultsParser() {
        this(false);
    }

    public AutodetectResultsParser(boolean isPerPartition) {
        isPerPartitionNormalization = isPerPartition;
    }

    public void addObserver(AlertObserver obs) {
        synchronized (observers) {
            observers.add(obs);
        }
    }

    public boolean removeObserver(AlertObserver obs) {
        synchronized (observers) {
            // relies on obj reference id for equality
            return observers.remove(obs);
        }
    }

    public int observerCount() {
        return observers.size();
    }


    /**
     * Parse the bucket results from inputstream and perist
     * via the JobDataPersister.
     * <p>
     * Trigger renormalisation of past results when new quantiles
     * are seen.
     */
    public void parseResults(InputStream inputStream, JobResultsPersister persister,
            Renormaliser renormaliser, Logger logger) throws ElasticsearchParseException {
        synchronized (acknowledgedFlushes) {
            parsingStarted = true;
            parsingInProgress = true;
            // NOCOMMIT fix this so its not needed
            acknowledgedFlushes.notifyAll();
        }

        try {
            parseResultsInternal(inputStream, persister, renormaliser, logger);
        } catch (IOException e) {
            throw new ElasticsearchParseException(e.getMessage(), e);
        } finally {
            // Don't leave any threads waiting for flushes in the lurch
            synchronized (acknowledgedFlushes) {
                // Leave parsingStarted set to true to avoid deadlock in the
                // case where the entire parse happens without the interested
                // thread getting scheduled
                parsingInProgress = false;
                // NOCOMMIT fix this so its not needed
                acknowledgedFlushes.notifyAll();
            }
        }
    }

    /**
     * Wait for a particular flush ID to be received by the parser.  In
     * order to wait, this method must be called after parsing has started.
     * It will give up waiting if parsing finishes before the flush ID is
     * seen or the timeout expires.
     *
     * @param flushId The ID to wait for.
     * @param timeout the timeout
     * @return true if the supplied flush ID was seen or the parsing finished; false if the timeout expired.
     */
    public boolean waitForFlushAcknowledgement(String flushId, Duration timeout) {
        synchronized (acknowledgedFlushes) {
            // NOCOMMIT fix this so its not needed
            try {
                acknowledgedFlushes.wait(timeout.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean isFlushAcknowledged = acknowledgedFlushes.remove(flushId);
            return isFlushAcknowledged || !parsingInProgress;
        }
    }

    /**
     * Can be used by unit tests to ensure the pre-condition of the
     * {@link #waitForFlushAcknowledgement(String, Duration) waitForFlushAcknowledgement} method is met.
     */
    void waitForParseStart() {
        synchronized (acknowledgedFlushes) {
            while (!parsingStarted) {
                // NOCOMMIT fix this so its not needed
                try {
                    acknowledgedFlushes.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void parseResultsInternal(InputStream inputStream, JobResultsPersister persister,
            Renormaliser renormaliser, Logger logger) throws IOException {
        logger.debug("Parse Results");
        boolean deleteInterimRequired = true;
        JsonParser parser = new JsonFactory().createParser(inputStream);
        parser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        JsonToken token = parser.nextToken();
        // if start of an array ignore it, we expect an array of buckets
        if (token == JsonToken.START_ARRAY) {
            token = parser.nextToken();
            logger.debug("JSON starts with an array");
        }

        if (token == JsonToken.END_ARRAY) {
            logger.info("Empty results array, 0 buckets parsed");
            return;
        } else if (token != JsonToken.START_OBJECT) {
            logger.error("Expecting Json Start Object token after the Start Array token");
            throw new ElasticsearchParseException(
                    "Invalid JSON should start with an array of objects or an object = " + token);
        }

        // Parse the buckets from the stream
        int bucketCount = 0;
        while (token != JsonToken.END_ARRAY) {
            if (token == null) // end of input
            {
                logger.error("Unexpected end of Json input");
                break;
            }
            if (token == JsonToken.START_OBJECT) {
                token = parser.nextToken();
                if (token == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    switch (fieldName) {
                    case "timestamp":
                        if (deleteInterimRequired == true) {
                            // Delete any existing interim results at the start of a job upload:
                            // these are generated by a Flush command, and will be replaced or
                            // superseded by new results
                            logger.trace("Deleting interim results");

                            // NOCOMMIT: This feels like an odd side-effect to have in a parser,
                            // especially since it has to wire up to actionlisteners.  Feels like it should
                            // be refactored out somewhere, after parsing?
                            persister.deleteInterimResults();
                            deleteInterimRequired = false;
                        }
                        Bucket bucket = new BucketParser(parser).parseJsonAfterStartObject();
                        if (isPerPartitionNormalization) {
                            bucket.calcMaxNormalizedProbabilityPerPartition();
                        }
                        persister.persistBucket(bucket);
                        notifyObservers(bucket);

                        logger.trace("Bucket number " + ++bucketCount + " parsed from output");
                        break;
                    case "quantileState":
                        Quantiles quantiles = new QuantilesParser(parser).parseJsonAfterStartObject();
                        persister.persistQuantiles(quantiles);

                        logger.debug("Quantiles parsed from output - will " +
                                "trigger renormalisation of scores");
                        if (isPerPartitionNormalization) {
                            renormaliser.renormaliseWithPartition(quantiles, logger);
                        } else {
                            renormaliser.renormalise(quantiles, logger);
                        }
                        break;
                    case "snapshotId":
                        ModelSnapshot modelSnapshot = new ModelSnapshotParser(parser).parseJsonAfterStartObject();
                        persister.persistModelSnapshot(modelSnapshot);
                        break;
                    case "modelBytes":
                        ModelSizeStats modelSizeStats = new ModelSizeStatsParser(parser).parseJsonAfterStartObject();
                        logger.trace(String.format(Locale.ROOT, "Parsed ModelSizeStats: %d / %d / %d / %d / %d / %s",
                                modelSizeStats.getModelBytes(),
                                modelSizeStats.getTotalByFieldCount(),
                                modelSizeStats.getTotalOverFieldCount(),
                                modelSizeStats.getTotalPartitionFieldCount(),
                                modelSizeStats.getBucketAllocationFailuresCount(),
                                modelSizeStats.getMemoryStatus()));

                        persister.persistModelSizeStats(modelSizeStats);
                        break;
                    case "debugFeature":
                        ModelDebugOutput modelDebugOutput = new ModelDebugOutputParser(parser).parseJsonAfterStartObject();
                        persister.persistModelDebugOutput(modelDebugOutput);
                        break;
                    case "flush":
                        FlushAcknowledgement ack = new FlushAcknowledgementParser(parser).parseJsonAfterStartObject();
                        logger.debug("Flush acknowledgement parsed from output for ID " +
                                ack.getId());
                        // Commit previous writes here, effectively continuing
                        // the flush from the C++ autodetect process right
                        // through to the data store
                        persister.commitWrites();
                        synchronized (acknowledgedFlushes) {
                            acknowledgedFlushes.add(ack.getId());
                            // NOCOMMIT fix this so its not needed
                            acknowledgedFlushes.notifyAll();
                        }
                        // Interim results may have been produced by the flush, which need to be
                        // deleted when the next finalized results come through
                        deleteInterimRequired = true;
                        break;
                    case "categoryDefinition":
                        CategoryDefinition category = new CategoryDefinitionParser(parser).parseJsonAfterStartObject();
                        persister.persistCategoryDefinition(category);
                        break;
                    default:
                        logger.error("Unexpected object parsed from output - first field " + fieldName);
                        throw new ElasticsearchParseException(
                                "Invalid JSON  - unexpected object parsed from output - first field " + fieldName);
                    }
                }
            } else {
                logger.error("Expecting Json Field name token after the Start Object token");
                throw new ElasticsearchParseException(
                        "Invalid JSON  - object should start with a field name, not " + parser.getText());
            }
            token = parser.nextToken();
        }

        logger.info(bucketCount + " buckets parsed from autodetect output - about to refresh indexes");

        // commit data to the datastore
        persister.commitWrites();
    }

    private final class ObserverTriggerPair {
        AlertObserver observer;
        AlertTrigger trigger;

        private ObserverTriggerPair(AlertObserver observer, AlertTrigger trigger) {
            this.observer = observer;
            this.trigger = trigger;
        }
    }

    private void notifyObservers(Bucket bucket) {
        List<ObserverTriggerPair> observersToFire = new ArrayList<>();

        // one-time alerts so remove them from the list before firing
        synchronized (observers) {
            Iterator<AlertObserver> iter = observers.iterator();
            while (iter.hasNext()) {
                AlertObserver ao = iter.next();

                List<AlertTrigger> triggeredAlerts = ao.triggeredAlerts(bucket);
                if (triggeredAlerts.isEmpty() == false) {
                    // only fire on the first alert trigger
                    observersToFire.add(new ObserverTriggerPair(ao, triggeredAlerts.get(0)));
                    iter.remove();
                }
            }
        }

        for (ObserverTriggerPair pair : observersToFire) {
            pair.observer.fire(bucket, pair.trigger);
        }
    }

}

