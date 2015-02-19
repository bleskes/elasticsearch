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
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.quantiles.QuantilesState;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;

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
    private final String m_JobId;

    private final JobProvider m_JobProvider;

    private final NormaliserProcessFactory m_ProcessFactory;

    private final Logger m_Logger;

    public Normaliser(String jobId, JobProvider jobProvider, Logger logger)
    {
        this(jobId, jobProvider, new NormaliserProcessFactory(), logger);
    }

    Normaliser(String jobId, JobProvider jobProvider,
            NormaliserProcessFactory processFactory, Logger logger)
    {
        m_JobId = jobId;
        m_JobProvider = jobProvider;
        m_ProcessFactory = processFactory;
        m_Logger = logger;
    }


    /**
     * Normalise buckets anomaly score for system state change.
     * Seed the normaliser with state retrieved from the data store.
     *
     * @param bucketSpan If <code>null</code> the default is used
     * @param buckets Will be modified to have the normalised result
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public List<Bucket> normaliseForSystemChange(Integer bucketSpan,
            List<Bucket> buckets)
    throws NativeProcessRunException, UnknownJobException
    {
        QuantilesState state = m_JobProvider.getQuantilesState(m_JobId);

        return normaliseForSystemChange(bucketSpan, buckets,
                state.getQuantilesState(QuantilesState.SYS_CHANGE_QUANTILES_KIND));
    }


    /**
     * Normalise buckets anomaly score for system state change.
     * Seed the normaliser with the state provided.
     *
     * @param bucketSpan If <code>null</code> the default is used
     * @param buckets Will be modified to have the normalised result
     * @param sysChangeState The state to be used to seed the system change
     * normaliser
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public List<Bucket> normaliseForSystemChange(Integer bucketSpan,
            List<Bucket> buckets, String sysChangeState)
    throws NativeProcessRunException, UnknownJobException
    {
        NormaliserProcess process = createNormaliserProcess(
                sysChangeState, null, bucketSpan);

        NormalisedResultsParser resultsParser = process.createNormalisedResultsParser(m_Logger);

        Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
        parserThread.start();

        LengthEncodedWriter writer = process.createProcessWriter();

        try
        {
            writer.writeNumFields(1);
            writer.writeField(ProcessCtrl.RAW_ANOMALY_SCORE);

            for (Bucket bucket : buckets)
            {
                writer.writeNumFields(1);
                writer.writeField(Double.toString(bucket.getRawAnomalyScore()));
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
        }

        return mergeNormalisedSystemChangeScoresIntoBuckets(
                resultsParser.getNormalisedResults(), buckets);
    }


    /**
     * Normalise the bucket and its nested records for unusual
     * behaviour.
     * The bucket's anomaly score is set to the max record score.
     * Seed the normaliser with state retrieved from the data store.
     *
     * @param bucketSpan If <code>null</code> the default is used
     * @param expandedBuckets Will be modified to have the normalised result
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public List<Bucket> normaliseForUnusualBehaviour(Integer bucketSpan,
            List<Bucket> expandedBuckets)
    throws NativeProcessRunException, UnknownJobException
    {
        QuantilesState state = m_JobProvider.getQuantilesState(m_JobId);

        return normaliseForUnusualBehaviour(bucketSpan, expandedBuckets,
            state.getQuantilesState(QuantilesState.UNUSUAL_QUANTILES_KIND));
    }


    /**
     * Normalise the bucket and its nested records for unusual
     * behaviour.
     * The bucket's anomaly score is set to the max record score.
     * Seed the normaliser with the state provided.
     *
     * @param bucketSpan If <code>null</code> the default is used
     * @param expandedBuckets Will be modified to have the normalised result
     * @param unusualBehaviourState The state to be used to seed the unusual behaviour
     * normaliser
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public List<Bucket> normaliseForUnusualBehaviour(Integer bucketSpan,
            List<Bucket> expandedBuckets, String unusualBehaviourState)
    throws NativeProcessRunException, UnknownJobException
    {
        NormaliserProcess process = createNormaliserProcess(
                null, unusualBehaviourState, bucketSpan);

        NormalisedResultsParser resultsParser = process.createNormalisedResultsParser(m_Logger);

        Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
        parserThread.start();

        LengthEncodedWriter writer = process.createProcessWriter();

        try
        {
            writer.writeNumFields(1);
            writer.writeField(ProcessCtrl.PROBABILITY);

            for (Bucket bucket : expandedBuckets)
            {
                for (AnomalyRecord record : bucket.getRecords())
                {
                    writer.writeNumFields(1);
                    writer.writeField(Double.toString(record.getProbability()));
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
                m_Logger.warn("Error closing unusual behaviour normalizer output stream", e);
            }
        }

        // Wait for the output parser
        try
        {
            parserThread.join();
        }
        catch (InterruptedException e)
        {
        }

        return mergeNormalisedUnusualIntoBuckets(
                resultsParser.getNormalisedResults(), expandedBuckets);
    }

    /**
     * Replace bucket and record anomaly scores with the new
     * normalised scores
     *
     * @param normalisedScores
     * @param buckets
     * @return
     * @throws NativeProcessRunException
     */
    private List<Bucket> mergeNormalisedSystemChangeScoresIntoBuckets(
            List<NormalisedResult> normalisedScores,
            List<Bucket> buckets)
    throws NativeProcessRunException
    {
        Iterator<NormalisedResult> resultIter = normalisedScores.iterator();

        try
        {
            for (Bucket bucket : buckets)
            {
                bucket.resetBigNormalisedUpdateFlag();

                double anomalyScore = resultIter.next().getNormalizedSysChangeScore();
                bucket.setAnomalyScore(anomalyScore);

                for (AnomalyRecord record : bucket.getRecords())
                {
                    record.resetBigNormalisedUpdateFlag();

                    record.setAnomalyScore(anomalyScore);
                }
            }
        }
        catch (NoSuchElementException e)
        {
            String msg = "Error iterating normalised results";
            m_Logger.error(msg, e);
            throw new NativeProcessRunException(msg, ErrorCode.NATIVE_PROCESS_ERROR);
        }

        return buckets;
    }


    /**
     * Set the bucket's unusual score equal to the highest of those
     * on the records it contains
     *
     * @param normalisedScores
     * @param buckets
     * @return
     * @throws NativeProcessRunException
     */
    private List<Bucket> mergeNormalisedUnusualIntoBuckets(
            List<NormalisedResult> normalisedScores,
            List<Bucket> buckets) throws NativeProcessRunException
    {
        Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();

        for (Bucket bucket : buckets)
        {
            bucket.resetBigNormalisedUpdateFlag();

            double maxNormalizedProbability = 0.0;
            boolean anyRecordHadBigUpdate = false;
            for (AnomalyRecord record : bucket.getRecords())
            {
                if (!scoresIter.hasNext())
                {
                    String msg = "Error iterating normalised results";
                    m_Logger.error(msg);
                    throw new NativeProcessRunException(msg, ErrorCode.NATIVE_PROCESS_ERROR);
                }
                double preNormalizedProbability = record.getNormalizedProbability();
                record.resetBigNormalisedUpdateFlag();

                NormalisedResult normalised = scoresIter.next();

                record.setNormalizedProbability(normalised.getNormalizedProbability());
                anyRecordHadBigUpdate |= record.hadBigNormalisedUpdate();

                maxNormalizedProbability = (record.hadBigNormalisedUpdate()) ?
                        Math.max(maxNormalizedProbability, normalised.getNormalizedProbability())
                        : Math.max(maxNormalizedProbability, preNormalizedProbability);
            }

            bucket.setMaxNormalizedProbability(maxNormalizedProbability);
            if (anyRecordHadBigUpdate)
            {
                bucket.raiseBigNormalisedUpdateFlag();
            }
        }

        return buckets;
    }

    /**
     * Create and start the normalization process
     *
     * @param sysChangeState
     * @param unusualBehaviourState
     * @param bucketSpan If <code>null</code> the default is used
     * @return
     * @throws NativeProcessRunException
     */
    private NormaliserProcess createNormaliserProcess(
            String sysChangeState,
            String unusualBehaviourState,
            Integer bucketSpan)
    throws NativeProcessRunException
    {
        try
        {
            return m_ProcessFactory.create(m_JobId, sysChangeState, unusualBehaviourState,
                    bucketSpan, m_Logger);
        }
        catch (IOException e)
        {
            String msg = "Failed to start normalisation process for job " + m_JobId;
            m_Logger.error(msg, e);
            throw new NativeProcessRunException(msg,
                    ErrorCode.NATIVE_PROCESS_START_ERROR, e);
        }
    }
}
