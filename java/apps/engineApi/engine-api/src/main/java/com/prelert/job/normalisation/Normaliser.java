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

package com.prelert.job.normalisation;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.writer.LengthEncodedWriter;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;

/**
 * Normalises bucket scores and anomaly records for either
 * System Change, Unusual behaviour or both.
 * <br/>
 * Creates and initialises the normaliser process, pipes the probabilities/
 * anomaly scores through them and adds the normalised values to
 * the records/buckets.
 * <br/>
 * Relies on the C++ normaliser process returning an answer for every input
 * and in exactly the same order as the inputs.
 */
public class Normaliser
{
    /** Field names of expected input in C++ normalisation process */
    private  static final String NORMALIZATION_LEVEL = "level";
    private static final String PARTITION_FIELD_NAME = "partitionFieldName";
    private static final String PER_PERSON_FIELD_NAME = "personFieldName";
    private static final String FUNCTION_NAME = "functionName";
    private static final String VALUE_FIELD_NAME = "valueFieldName";
    private static final String RAW_SCORE = "rawScore";

    /** Normalisation levels */
    private static final String ROOT = "root";
    private static final String LEAF = "leaf";

    private final String m_JobId;
    private final NormaliserProcessFactory m_ProcessFactory;
    private final Logger m_Logger;

    public Normaliser(String jobId, NormaliserProcessFactory processFactory, Logger logger)
    {
        m_JobId = jobId;
        m_ProcessFactory = processFactory;
        m_Logger = logger;
    }

    /**
     * Normalise buckets anomaly scores and records normalized probability.
     * Seed the normaliser with the state provided.
     *
     * @param bucketSpan If <code>null</code> the default is used
     * @param buckets Will be modified to have the normalised result
     * @param quantilesState The state to be used to seed the system change
     * normaliser
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public List<Bucket> normalise(Integer bucketSpan, List<Bucket> buckets, String quantilesState)
    throws NativeProcessRunException, UnknownJobException
    {
        NormaliserProcess process = createNormaliserProcess(quantilesState, bucketSpan);

        NormalisedResultsParser resultsParser = process.createNormalisedResultsParser(m_Logger);

        Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
        parserThread.start();

        LengthEncodedWriter writer = process.createProcessWriter();

        try
        {
            writer.writeRecord(new String[] { NORMALIZATION_LEVEL, PARTITION_FIELD_NAME,
                    PER_PERSON_FIELD_NAME, FUNCTION_NAME, VALUE_FIELD_NAME, RAW_SCORE });

            for (Bucket bucket : buckets)
            {
                writer.writeRecord(new String[] {
                        ROOT, "", "", "", "", Double.toString(bucket.getRawAnomalyScore())});
                for (AnomalyRecord record : bucket.getRecords())
                {
                    writer.writeRecord(new String[] {
                            LEAF,
                            Strings.nullToEmpty(record.getPartitionFieldName()),
                            Strings.nullToEmpty(getPersonFieldName(record)),
                            Strings.nullToEmpty(record.getFunction()),
                            Strings.nullToEmpty(record.getFieldName()),
                            Double.toString(record.getProbability())
                            });
                }
            }
        }
        catch (IOException e)
        {
            m_Logger.warn("Error writing to the normalizer", e);
        }
        finally
        {
            try
            {
                process.closeOutputStream();
            }
            catch (IOException e)
            {
                m_Logger.warn("Error closing normalizer output stream", e);
            }
        }

        // Wait for the output parser
        try
        {
            parserThread.join();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        return mergeNormalisedScoresIntoBuckets(resultsParser.getNormalisedResults(), buckets);
    }

    /**
     * Updates the normalised scores on the results.
     *
     * @param normalisedScores
     * @param buckets
     * @return
     * @throws NativeProcessRunException
     */
    private List<Bucket> mergeNormalisedScoresIntoBuckets(List<NormalisedResult> normalisedScores,
            List<Bucket> buckets) throws NativeProcessRunException
    {
        Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();

        for (Bucket bucket : buckets)
        {
            bucket.resetBigNormalisedUpdateFlag();

            double anomalyScore = scoresIter.next().getNormalizedScore();
            boolean anomalyScoreHadBigUpdate = isBigUpdate(bucket.getAnomalyScore(), anomalyScore);
            if (anomalyScoreHadBigUpdate)
            {
                bucket.setAnomalyScore(anomalyScore);
                bucket.raiseBigNormalisedUpdateFlag();
            }

            double maxNormalizedProbability = 0.0;
            for (AnomalyRecord record : bucket.getRecords())
            {
                if (!scoresIter.hasNext())
                {
                    String msg = "Error iterating normalised results";
                    m_Logger.error(msg);
                    throw new NativeProcessRunException(msg, ErrorCodes.NATIVE_PROCESS_ERROR);
                }
                record.resetBigNormalisedUpdateFlag();
                if (anomalyScoreHadBigUpdate)
                {
                    record.setAnomalyScore(anomalyScore);
                    record.raiseBigNormalisedUpdateFlag();
                }

                NormalisedResult recordNormalisedResult = scoresIter.next();
                double normalizedProbability = recordNormalisedResult.getNormalizedScore();
                boolean normalizedProbabilityHadBigUpdate =
                        isBigUpdate(record.getNormalizedProbability(), normalizedProbability);
                if (normalizedProbabilityHadBigUpdate)
                {
                    record.setNormalizedProbability(normalizedProbability);
                    record.raiseBigNormalisedUpdateFlag();
                    bucket.raiseBigNormalisedUpdateFlag();
                }
                maxNormalizedProbability =
                        Math.max(maxNormalizedProbability, record.getNormalizedProbability());
            }

            bucket.setMaxNormalizedProbability(maxNormalizedProbability);
        }

        return buckets;
    }

    /**
     * Create and start the normalization process
     *
     * @param quantilesState
     * @param bucketSpan If <code>null</code> the default is used
     * @return
     * @throws NativeProcessRunException
     */
    private NormaliserProcess createNormaliserProcess(String quantilesState, Integer bucketSpan)
    throws NativeProcessRunException
    {
        try
        {
            return m_ProcessFactory.create(m_JobId, quantilesState, bucketSpan, m_Logger);
        }
        catch (IOException e)
        {
            String msg = "Failed to start normalisation process for job " + m_JobId;
            m_Logger.error(msg, e);
            throw new NativeProcessRunException(msg,
                    ErrorCodes.NATIVE_PROCESS_START_ERROR, e);
        }
    }

    private static String getPersonFieldName(AnomalyRecord record)
    {
        String over = record.getOverFieldName();
        return over != null ? over : record.getByFieldName();
    }

    /**
     * Encapsulate the logic for deciding whether a change to a normalised score
     * is "big".
     *
     * Current logic is that a big change is a change of at least 1 or more than
     * than 50% of the higher of the two values.
     *
     * @param oldVal The old value of the normalised score
     * @param newVal The new value of the normalised score
     * @return true if the update is considered "big"
     */
    private static boolean isBigUpdate(double oldVal, double newVal)
    {
        if (Math.abs(oldVal - newVal) >= 1.0)
        {
            return true;
        }
        if (oldVal > newVal)
        {
            if (oldVal * 0.5 > newVal)
            {
                return true;
            }
        }
        else
        {
            if (newVal * 0.5 > oldVal)
            {
                return true;
            }
        }

        return false;
    }
}
