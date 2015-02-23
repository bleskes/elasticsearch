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

package com.prelert.job;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.prelert.rs.data.ErrorCode;

/**
 * Autodetect analysis configuration options describes which fields are
 * analysed and the functions to use.
 * <p/>
 * The configuration can contain multiple detectors, a new anomaly detector will
 * be created for each detector configuration. The other fields
 * <code>bucketSpan, summaryCountFieldName</code> etc apply to all detectors.<p/>
 * If a value has not been set it will be <code>null</code>
 * Object wrappers are used around integral types & booleans so they can take
 * <code>null</code> values.
 */
public class AnalysisConfig
{
    /**
     * Serialisation names
     */
    public static final String BUCKET_SPAN = "bucketSpan";
    public static final String BATCH_SPAN = "batchSpan";
    public static final String LATENCY = "latency";
    public static final String PERIOD = "period";
    public static final String SUMMARY_COUNT_FIELD_NAME = "summaryCountFieldName";
    public static final String DETECTORS = "detectors";

    /**
     * These values apply to all detectors
     */
    private Long m_BucketSpan;
    private Long m_BatchSpan;
    private Long m_Latency = 0L;
    private Long m_Period;
    private String m_SummaryCountFieldName;
    private List<Detector> m_Detectors;

    /**
     * Default constructor
     */
    public AnalysisConfig()
    {
        m_Detectors = new ArrayList<>();
    }

    /**
     * The size of the interval the analysis is aggregated into measured in
     * seconds
     * @return The bucketspan or <code>null</code> if not set
     */
    public Long getBucketSpan()
    {
        return m_BucketSpan;
    }

    public void setBucketSpan(Long span)
    {
        m_BucketSpan = span;
    }

    /**
     * Interval into which to batch seasonal data measured in seconds
     * @return The batchspan or <code>null</code> if not set
     */
    public Long getBatchSpan()
    {
        return m_BatchSpan;
    }

    public void setBatchSpan(Long batchSpan)
    {
        m_BatchSpan = batchSpan;
    }

    /**
     * The latency interval (seconds) during which out-of-order records should be handled.
     * @return The latency interval (seconds) or <code>null</code> if not set
     */
    public Long getLatency()
    {
        return m_Latency;
    }

    /**
     * Set the latency interval during which out-of-order records should be handled.
     * @param latency the latency interval in seconds
     */
    public void setLatency(Long latency)
    {
        m_Latency = latency;
    }

    /**
     * The repeat interval for periodic data in multiples of
     * {@linkplain #getBatchSpan()}
     * @return The period or <code>null</code> if not set
     */
    public Long getPeriod()
    {
        return m_Period;
    }

    public void setPeriod(Long period)
    {
        m_Period = period;
    }

    /**
     * The name of the field that contains counts for pre-summarised input
     * @return The field name or <code>null</code> if not set
     */
    public String getSummaryCountFieldName()
    {
        return m_SummaryCountFieldName;
    }

    public void setSummaryCountFieldName(String summaryCountFieldName)
    {
        m_SummaryCountFieldName = summaryCountFieldName;
    }

    /**
     * The list of analysis detectors. In a valid configuration the list should
     * contain at least 1 {@link Detector}
     * @return The Detectors used in this job
     */
    public List<Detector> getDetectors()
    {
        return m_Detectors;
    }

    public void setDetectors(List<Detector> detectors)
    {
        m_Detectors = detectors;
    }

    /**
     * Return the list of fields required by the analysis.
     * These are the metric field, partition field, by field and over
     * field of each detector, plus the summary count field of the job.
     * <code>null</code> and empty strings are filtered from the
     * config
     *
     * @return List of required fields.
     */
    public List<String> analysisFields()
    {
        Set<String> fields = new TreeSet<>();

        if (m_SummaryCountFieldName != null)
        {
            fields.add(m_SummaryCountFieldName);
        }

        for (Detector d : getDetectors())
        {
            if (d.getFieldName() != null)
            {
                fields.add(d.getFieldName());
            }
            if (d.getByFieldName() != null)
            {
                fields.add(d.getByFieldName() );
            }
            if (d.getOverFieldName() != null)
            {
                fields.add(d.getOverFieldName());
            }
            if (d.getPartitionFieldName() != null)
            {
                fields.add(d.getPartitionFieldName());
            }
        }

        // remove empty strings
        fields.remove("");

        return new ArrayList<String>(fields);
    }



    public List<String> fields()
    {
        Set<String> fields = new HashSet<>();

        for (Detector d : getDetectors())
        {
            fields.add(d.getFieldName());
        }

        // remove the null and empty strings
        fields.remove("");
        fields.remove(null);

        return new ArrayList<String>(fields);
    }



    public List<String> byFields()
    {
        Set<String> fields = new HashSet<>();

        for (Detector d : getDetectors())
        {
            fields.add(d.getByFieldName());
        }

        // remove the null and empty strings
        fields.remove("");
        fields.remove(null);

        return new ArrayList<String>(fields);
    }

    public List<String> overFields()
    {
        Set<String> fields = new HashSet<>();

        for (Detector d : getDetectors())
        {
            fields.add(d.getOverFieldName());
        }

        // remove the null and empty strings
        fields.remove("");
        fields.remove(null);

        return new ArrayList<String>(fields);
    }


    public List<String> partitionFields()
    {
        Set<String> fields = new HashSet<>();

        for (Detector d : getDetectors())
        {
            fields.add(d.getPartitionFieldName());
        }

        // remove the null and empty strings
        fields.remove("");
        fields.remove(null);

        return new ArrayList<String>(fields);
    }

    /**
     * The array of detectors are compared for equality but they are not sorted
     * first so this test could fail simply because the detector arrays are in
     * different orders.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof AnalysisConfig == false)
        {
            return false;
        }

        AnalysisConfig that = (AnalysisConfig)other;

        if (this.m_Detectors.size() != that.m_Detectors.size())
        {
            return false;
        }

        for (int i=0; i<m_Detectors.size(); i++)
        {
            if (!this.m_Detectors.get(i).equals(that.m_Detectors.get(i)))
            {
                return false;
            }
        }

        return Objects.equals(this.m_BucketSpan, that.m_BucketSpan) &&
                Objects.equals(this.m_BatchSpan, that.m_BatchSpan) &&
                Objects.equals(this.m_Latency, that.m_Latency) &&
                Objects.equals(this.m_Period, that.m_Period) &&
                Objects.equals(this.m_SummaryCountFieldName, that.m_SummaryCountFieldName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_Detectors, m_BucketSpan, m_BatchSpan, m_Latency, m_Period,
                m_SummaryCountFieldName);
    }

    /**
     * Checks the configuration is valid
     * <ol>
     * <li>Check that if non-null BucketSpan, BatchSpan, Latency and Period are &gt= 0</li>
     * <li>Check that if non-null Latency is &lt= MAX_LATENCY </li>
     * <li>Check there is at least one detector configured</li>
     * <li>Check all the detectors are configured correctly</li>
     * </ol>
     *
     * @return true
     * @throws JobConfigurationException
     */
    public boolean verify() throws JobConfigurationException
    {
        checkFieldIsNotNegativeIfSpecified("BucketSpan", m_BucketSpan);
        checkFieldIsNotNegativeIfSpecified("BatchSpan", m_BatchSpan);
        checkFieldIsNotNegativeIfSpecified("Latency", m_Latency);
        checkFieldIsNotNegativeIfSpecified("Period", m_Period);

        Detector.verifyFieldName(m_SummaryCountFieldName);

        if (m_Detectors.isEmpty())
        {
            throw new JobConfigurationException("No detectors configured",
                    ErrorCode.INCOMPLETE_CONFIGURATION);
        }

        boolean isSummarised = (m_SummaryCountFieldName != null && !m_SummaryCountFieldName.isEmpty());
        for (Detector d : m_Detectors)
        {
            d.verify(isSummarised);
        }

        return true;
    }

    private static void checkFieldIsNotNegativeIfSpecified(String fieldName, Long value)
            throws JobConfigurationException
    {
        if (value != null && value < 0)
        {
            String msg = String.format("%s cannot be < 0. Value = %d", fieldName, value);
            throw new JobConfigurationException(msg, ErrorCode.INVALID_VALUE);
        }
    }
}
