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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.prelert.job.transform.TransformConfig;


/**
 * This class encapsulates all the data required to create a new job it
 * does not represent the state of a created job (see {@linkplain JobDetails}
 * for that).
 * <p>
 * If a value has not been set it will be <code>null</code> Object wrappers
 * are used around integral types &amp; booleans so they can take <code>null</code>
 * values.
 */
public class JobConfiguration
{
    public static final int MAX_JOB_ID_LENGTH = 64;

    /**
     * Characters that cannot be in a job id: '\\', '/', '*', '?', '&quot;', '&lt;', '&gt;', '|', ' ', ','
     */
    public static final Set<Character> PROHIBITED_JOB_ID_CHARACTERS_SET;
    public static final String PROHIBITED_JOB_ID_CHARACTERS;
    static
    {
        // frustrating work around to initialise both the set
        // and string from the same chars

        char [] prohibited = {'\\', '/', '*', '?', '"', '<', '>', '|', ' ', ','};
        PROHIBITED_JOB_ID_CHARACTERS = new String(prohibited);

        PROHIBITED_JOB_ID_CHARACTERS_SET = new HashSet<Character>();
        for (char ch : prohibited)
        {
            PROHIBITED_JOB_ID_CHARACTERS_SET.add(ch);
        }


    /**
     * Max number of chars in a job id
     */
    }

    private String m_ID;
    private String m_Description;

    private AnalysisConfig m_AnalysisConfig;
    private AnalysisLimits m_AnalysisLimits;
    private List<TransformConfig> m_Transforms;
    private DataDescription m_DataDescription;
    private String m_ReferenceJobId;
    private Long m_Timeout;
    private ModelDebugConfig m_ModelDebugConfig;

    public JobConfiguration()
    {
    }

    public JobConfiguration(String jobReferenceId)
    {
        this();
        m_ReferenceJobId = jobReferenceId;
    }

    public JobConfiguration(AnalysisConfig analysisConfig)
    {
        this();
        m_AnalysisConfig = analysisConfig;
    }


    /**
     * The human readable job Id
     * @return The provided name or null if not set
     */
    public String getId()
    {
        return m_ID;
    }

    /**
     * Set the job's ID
     * @param id the id of the job
     */
    public void setId(String id)
    {
        m_ID = id;
    }

    /**
     * The job's human readable description
     * @return the job description
     */
    public String getDescription()
    {
        return m_Description;
    }

    /**
     * Set the human readable description
     */
    public void setDescription(String description)
    {
        m_Description = description;
    }


    /**
     * The analysis configuration. A properly configured job must have
     * a valid AnalysisConfig
     * @return AnalysisConfig or null if not set.
     */
    public AnalysisConfig getAnalysisConfig()
    {
        return m_AnalysisConfig;
    }

    public void setAnalysisConfig(AnalysisConfig config)
    {
        m_AnalysisConfig = config;
    }

    /**
     * The analysis limits
     *
     * @return Analysis limits or null if not set.
     */
    public AnalysisLimits getAnalysisLimits()
    {
        return m_AnalysisLimits;
    }

    public void setAnalysisLimits(AnalysisLimits options)
    {
        m_AnalysisLimits = options;
    }

    /**
     * If the job is to be created with the same configuration as a previously
     * run job then this is the id of that job. If set then this option
     * overrides the {@linkplain #getAnalysisConfig()} settings i.e. they will
     * be ignored.
     * @return A String or <code>null</code> if not set
     */
    public String getReferenceJobId()
    {
        return m_ReferenceJobId;
    }

    public void setReferenceJobId(String refId)
    {
        m_ReferenceJobId = refId;
    }

    /**
     * The timeout period for the job in seconds
     * @return The timeout in seconds
     */
    public Long getTimeout()
    {
        return m_Timeout;
    }

    public void setTimeout(Long timeout)
    {
        m_Timeout = timeout;
    }

    public List<TransformConfig> getTransforms()
    {
        return m_Transforms;
    }

    public void setTransforms(List<TransformConfig> transforms)
    {
        m_Transforms = transforms;
    }

    /**
     * If not set the input data is assumed to be csv with a '_time' field
     * in epoch format.
     * @return A DataDescription or <code>null</code>
     * @see DataDescription
     */
    public DataDescription getDataDescription()
    {
        return m_DataDescription;
    }

    public void setDataDescription(DataDescription description)
    {
        m_DataDescription = description;
    }

    public void setModelDebugConfig(ModelDebugConfig modelDebugConfig)
    {
        m_ModelDebugConfig = modelDebugConfig;
    }

    public ModelDebugConfig getModelDebugConfig()
    {
        return m_ModelDebugConfig;
    }
}
