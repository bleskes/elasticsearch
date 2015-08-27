/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.process.output.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.persistence.JobResultsPersister;
import com.prelert.job.process.output.FlushAcknowledgement;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.utils.json.AutoDetectParseException;

/**
 * Parses the JSON output of the autodetect program.
 *
 * Expects an array of buckets so the first element will always be the
 * start array symbol and the data must be terminated with the end array symbol.
 */
public class AutoDetectResultsParser
{
    private List<AlertObserver> m_Observers = new ArrayList<>();
    private Set<String> m_AcknowledgedFlushes = new HashSet<>();
    private volatile boolean m_ParsingStarted;
    private volatile boolean m_ParsingInProgress;

    public void addObserver(AlertObserver obs)
    {
        synchronized (m_Observers)
        {
            m_Observers.add(obs);
        }
    }

    public boolean removeObserver(AlertObserver obs)
    {
        synchronized (m_Observers)
        {
            // relies on obj reference id for equality
            int index = m_Observers.indexOf(obs);

            if (index >= 0)
            {
                m_Observers.remove(index);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    public int observerCount()
    {
        return m_Observers.size();
    }


    /**
     * Parse the bucket results from inputstream and perist
     * via the JobDataPersister.
     *
     * Trigger renormalisation of past results when new quantiles
     * are seen.
     *
     * @param inputStream
     * @param persister
     * @param renormaliser
     * @param logger
     * @return
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public void parseResults(InputStream inputStream, JobResultsPersister persister,
            Renormaliser renormaliser, Logger logger)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        synchronized (m_AcknowledgedFlushes)
        {
            m_ParsingStarted = true;
            m_ParsingInProgress = true;
            m_AcknowledgedFlushes.notifyAll();
        }

        try
        {
            parseResultsInternal(inputStream, persister, renormaliser, logger);
        }
        finally
        {
            // Don't leave any threads waiting for flushes in the lurch
            synchronized (m_AcknowledgedFlushes)
            {
                // Leave m_ParsingStarted set to true to avoid deadlock in the
                // case where the entire parse happens without the interested
                // thread getting scheduled
                m_ParsingInProgress = false;
                m_AcknowledgedFlushes.notifyAll();
            }
        }
    }


    /**
     * Wait for a particular flush ID to be received by the parser.  In
     * order to wait, this method must be called after parsing has started.
     * It will give up waiting if parsing finishes before the flush ID is
     * seen.
     * @param flushId The ID to wait for.
     * @return true if the supplied flush ID was seen; false if parsing finished
     * before the supplied flush ID was seen.
     */
    public boolean waitForFlushAcknowledgement(String flushId)
    throws InterruptedException
    {
        synchronized (m_AcknowledgedFlushes)
        {
            while (m_ParsingInProgress && !m_AcknowledgedFlushes.contains(flushId))
            {
                m_AcknowledgedFlushes.wait();
            }
            return m_AcknowledgedFlushes.remove(flushId);
        }
    }


    /**
     * Can be used by unit tests to ensure the pre-condition of the
     * {@link #waitForFlushAcknowledgement(String) waitForFlushAcknowledgement} method is met.
     */
    void waitForParseStart()
    throws InterruptedException
    {
        synchronized (m_AcknowledgedFlushes)
        {
            while (!m_ParsingStarted)
            {
                m_AcknowledgedFlushes.wait();
            }
        }
    }


    private void parseResultsInternal(InputStream inputStream, JobResultsPersister persister,
            Renormaliser renormaliser, Logger logger)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        JsonParser parser = new JsonFactory().createParser(inputStream);
        parser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        JsonToken token = parser.nextToken();
        // if start of an array ignore it, we expect an array of buckets
        if (token == JsonToken.START_ARRAY)
        {
            token = parser.nextToken();
            logger.debug("JSON starts with an array");
        }

        if (token == JsonToken.END_ARRAY)
        {
            logger.info("Empty results array, 0 buckets parsed");
            return;
        }
        else if (token != JsonToken.START_OBJECT)
        {
            logger.error("Expecting Json Start Object token after the Start Array token");
            throw new AutoDetectParseException(
                    "Invalid JSON should start with an array of objects or an object = " + token);
        }

        // Parse the buckets from the stream
        int bucketCount = 0;
        while (token != JsonToken.END_ARRAY)
        {
            if (token == null) // end of input
            {
                logger.error("Unexpected end of Json input");
                break;
            }
            if (token == JsonToken.START_OBJECT)
            {
                token = parser.nextToken();
                if (token == JsonToken.FIELD_NAME)
                {
                    String fieldName = parser.getCurrentName();
                    switch (fieldName)
                    {
                    case Bucket.TIMESTAMP:
                        Bucket bucket = BucketParser.parseJsonAfterStartObject(parser);
                        persister.persistBucket(bucket);
                        persister.incrementBucketCount(1);
                        notifyObservers(bucket);

                        logger.debug("Bucket number " + ++bucketCount + " parsed from output");
                        break;
                    case Quantiles.QUANTILE_STATE:
                        Quantiles quantiles = QuantilesParser.parseJsonAfterStartObject(parser);
                        persister.persistQuantiles(quantiles);

                        logger.debug("Quantiles parsed from output - will " +
                                    "trigger renormalisation of scores");
                        renormaliser.renormalise(quantiles, logger);
                        break;
                    case ModelSizeStats.TYPE:
                        ModelSizeStats modelSizeStats = ModelSizeStatsParser.parseJsonAfterStartObject(parser);
                        logger.trace(String.format("Parsed ModelSizeStats: %d / %d / %d / %d / %d / %s",
                            modelSizeStats.getModelBytes(),
                            modelSizeStats.getTotalByFieldCount(),
                            modelSizeStats.getTotalOverFieldCount(),
                            modelSizeStats.getTotalPartitionFieldCount(),
                            modelSizeStats.getBucketAllocationFailuresCount(),
                            modelSizeStats.getMemoryStatus()));

                        persister.persistModelSizeStats(modelSizeStats);
                        break;
                    case FlushAcknowledgement.FLUSH:
                        FlushAcknowledgement ack = FlushAcknowledgementParser.parseJsonAfterStartObject(parser);
                        logger.debug("Flush acknowledgement parsed from output for ID " +
                                    ack.getId());
                        // Commit previous writes here, effectively continuing
                        // the flush from the C++ autodetect process right
                        // through to the data store
                        persister.commitWrites();
                        synchronized (m_AcknowledgedFlushes)
                        {
                            m_AcknowledgedFlushes.add(ack.getId());
                            m_AcknowledgedFlushes.notifyAll();
                        }
                        break;
                    case CategoryDefinition.TYPE:
                        CategoryDefinition category = CategoryDefinitionParser.parseJsonAfterStartObject(parser);
                        persister.persistCategoryDefinition(category);
                        break;
                    default:
                        logger.error("Unexpected object parsed from output - first field " + fieldName);
                        throw new AutoDetectParseException(
                                "Invalid JSON  - unexpected object parsed from output - first field " + fieldName);
                    }
                }
            }
            else
            {
                logger.error("Expecting Json Field name token after the Start Object token");
                throw new AutoDetectParseException(
                        "Invalid JSON  - object should start with a field name, not " + parser.getText());
            }

            token = parser.nextToken();
        }

        logger.info(bucketCount + " buckets parsed from autodetect output - about to refresh indexes");

        // commit data to the datastore
        persister.commitWrites();
    }

    private void notifyObservers(Bucket bucket)
    {
        // Never alert on interim results
        if (bucket.isInterim() != null &&
            bucket.isInterim() == true)
        {
            return;
        }

        List<AlertObserver> observersToFire = new ArrayList<>();

        // one-time alerts so remove them from the list before firing
        synchronized (m_Observers)
        {
            Iterator<AlertObserver> iter = m_Observers.iterator();
            while (iter.hasNext())
            {
                AlertObserver ao = iter.next();

                if (ao.evaluate(bucket.getAnomalyScore(), bucket.getMaxNormalizedProbability()))
                {
                    observersToFire.add(ao);
                    iter.remove();
                }
            }
        }

        for (AlertObserver ao : observersToFire)
        {
            ao.fire(bucket);
        }
    }


}

