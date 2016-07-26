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

package com.prelert.job.process.normaliser;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.normalisation.NormalisedResult;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.output.parsing.NormalisedResultsParser;
import com.prelert.job.process.writer.LengthEncodedWriter;

/**
 * Normalises bucket scores and anomaly records for either
 * System Change, Unusual behaviour or both.
 * <br>
 * Creates and initialises the normaliser process, pipes the probabilities/
 * anomaly scores through them and adds the normalised values to
 * the records/buckets.
 * <br>
 * Relies on the C++ normaliser process returning an answer for every input
 * and in exactly the same order as the inputs.
 */
public class Normaliser
{
    /** Field names of expected input in C++ normalisation process */
    private  static final String NORMALIZATION_LEVEL = "level";
    private static final String PARTITION_FIELD_NAME = "partitionFieldName";
    private static final String PARTITION_FIELD_VALUE = "partitionFieldValue";
    private static final String PER_PERSON_FIELD_NAME = "personFieldName";
    private static final String FUNCTION_NAME = "functionName";
    private static final String VALUE_FIELD_NAME = "valueFieldName";
    private static final String PROBABILITY = "probability";

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
     * Launches a normalisation process seeded with the quantiles state provided
     * and normalises the given results.
     *
     * @param bucketSpan If <code>null</code> the default is used
     * @param results Will be updated with the normalised results
     * @param quantilesState The state to be used to seed the system change
     * normaliser
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public void normalise(Integer bucketSpan, List<Normalisable> results, String quantilesState)
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
                                    PARTITION_FIELD_VALUE,PER_PERSON_FIELD_NAME, FUNCTION_NAME,
                                    VALUE_FIELD_NAME, PROBABILITY });

            for (Normalisable result: results)
            {
                writeNormalisableAndChildrenRecursively(result, writer);
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

        mergeNormalisedScoresIntoResults(resultsParser.getNormalisedResults(), results);
    }

    private static void writeNormalisableAndChildrenRecursively(Normalisable normalisable,
            LengthEncodedWriter writer) throws IOException
    {
        if (normalisable.isContainerOnly() == false)
        {
            writer.writeRecord(new String[] {
                    normalisable.getLevel().asString(),
                    Strings.nullToEmpty(normalisable.getPartitionFieldName()),
                    Strings.nullToEmpty(normalisable.getPartitionFieldValue()),
                    Strings.nullToEmpty(normalisable.getPersonFieldName()),
                    Strings.nullToEmpty(normalisable.getFunctionName()),
                    Strings.nullToEmpty(normalisable.getValueFieldName()),
                    Double.toString(normalisable.getProbability())
            });
        }
        for (Normalisable child : normalisable.getChildren())
        {
            writeNormalisableAndChildrenRecursively(child, writer);
        }
    }

    /**
     * Updates the normalised scores on the results.
     *
     * @param normalisedScores
     * @param results
     * @throws NativeProcessRunException
     */
    private void mergeNormalisedScoresIntoResults(List<NormalisedResult> normalisedScores,
            List<Normalisable> results) throws NativeProcessRunException
    {
        Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();
        for (Normalisable result: results)
        {
            mergeRecursively(scoresIter, null, false, result);
        }
        if (scoresIter.hasNext())
        {
            m_Logger.error("Unused normalized scores remain after updating all results: "
                    + normalisedScores.size() + " for " + results.size() + " results");
        }
    }

    /**
     * Recursively merges the scores returned by the normalisation process into the results
     *
     * @param scoresIter an Iterator of the scores returned by the normalisation process
     * @param parent the parent result
     * @param parentHadBigChange whether the parent had a big change
     * @param result the result to be updated
     * @return the effective normalised score of the given result
     * @throws NativeProcessRunException
     */
    private double mergeRecursively(Iterator<NormalisedResult> scoresIter, Normalisable parent,
            boolean parentHadBigChange, Normalisable result) throws NativeProcessRunException
    {
        boolean hasBigChange = false;
        if (result.isContainerOnly() == false)
        {
            if (!scoresIter.hasNext())
            {
                String msg = "Error iterating normalised results";
                m_Logger.error(msg);
                throw new NativeProcessRunException(msg, ErrorCodes.NATIVE_PROCESS_ERROR);
            }

            result.resetBigChangeFlag();
            if (parent != null && parentHadBigChange)
            {
                result.setParentScore(parent.getNormalisedScore());
                result.raiseBigChangeFlag();
            }

            double normalisedScore = scoresIter.next().getNormalizedScore();
            hasBigChange = isBigUpdate(result.getNormalisedScore(), normalisedScore);
            if (hasBigChange)
            {
                result.setNormalisedScore(normalisedScore);
                result.raiseBigChangeFlag();
                if (parent != null)
                {
                    parent.raiseBigChangeFlag();
                }
            }
        }

        for (Integer childrenType : result.getChildrenTypes())
        {
            List<Normalisable> children = result.getChildren(childrenType);
            if (!children.isEmpty())
            {
                double maxChildrenScore = 0.0;
                for (Normalisable child : children)
                {
                    maxChildrenScore = Math.max(
                            mergeRecursively(scoresIter, result, hasBigChange, child),
                            maxChildrenScore);
                }
                hasBigChange |= result.setMaxChildrenScore(childrenType, maxChildrenScore);
            }
        }
        return result.getNormalisedScore();
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
