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

import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.job.verification.Verifiable;
import com.prelert.rs.data.ErrorCode;

/**
 * This class encapsulates all the data required to create a new job it
 * does not represent the state of a created job (see {@linkplain JobDetails}
 * for that).
 * <p/>
 * If a value has not been set it will be <code>null</code> Object wrappers
 * are used around integral types & booleans so they can take <code>null</code>
 * values.
 */
public class JobConfiguration implements Verifiable
{
    private static final int MAX_JOB_ID_LENGTH = 64;

    /**
     * Characters that cannot be in a job id: '\\', '/', '*', '?', '"', '<', '>', '|', ' ', ','
     */
    private static final Set<Character> PROHIBITED_JOB_ID_CHARACTERS_SET;
    private static final String PROHIBITED_JOB_ID_CHARACTERS;
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
     * @param name
     */
    public void setId(String id)
    {
        m_ID = id;
    }

    /**
     * The job's human readable description
     * @return
     */
    public String getDescription()
    {
        return m_Description;
    }

    /**
     * Set the human readable description
     * @return
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

    /**
     * Checks the job configuration settings and throws an exception
     * if any values are invalid
     *
     * <ol>
     * <li>Either an AnalysisConfig or Job reference must be set</li>
     * <li>Verify {@link AnalysisConfig#verify() AnalysisConfig}</li>
     * <li>Verify {@link AnalysisLimits#verify() AnalysisLimits}</li>
     * <li>Verify {@link DataDescription#verify() DataDescription}</li>
     * <li>Verify {@link TransformConfigs#verify() Transforms}</li>
     * <li>Check timeout is a +ve number</li>
     * <li>The job ID cannot contain any upper case characters, control
     * characters or any characters in {@link #PROHIBITED_JOB_ID_CHARACTERS_SET}</li>
     * <li>The job is cannot be longer than {@link MAX_JOB_ID_LENGTH }</li>
     * <li></li>
     * </ol>
     */
    @Override
    public boolean verify()
    throws JobConfigurationException
    {
        checkEitherAnalysisConfigOrJobReferenceIdPresent();
        verifyIfNotNull(m_AnalysisConfig);
        verifyIfNotNull(m_AnalysisLimits);
        verifyIfNotNull(m_DataDescription);

        if (m_Transforms != null)
        {
            new TransformConfigs(m_Transforms).verify();
            checkTransformOutputIsUsed();
        }

        checkAtLeastOneTransformIfDataFormatIsSingleLine();

        if (m_Timeout != null && m_Timeout < 0)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_NEGATIVE_FIELD_VALUE,
                            "timeout", m_Timeout),
                    ErrorCode.INVALID_VALUE);
        }

        if (m_ID != null && m_ID.isEmpty() == false)
        {
            checkValidId();
        }

        return true;
    }

    private void checkEitherAnalysisConfigOrJobReferenceIdPresent()
            throws JobConfigurationException
    {
        if (m_AnalysisConfig == null && m_ReferenceJobId == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_MISSING_ANALYSISCONFIG),
                    ErrorCode.INCOMPLETE_CONFIGURATION);
        }
    }

    private static void verifyIfNotNull(Verifiable verifiable) throws JobConfigurationException
    {
        if (verifiable != null)
        {
            verifiable.verify();
        }
    }

    /**
     * Transform outputs should be used in either the date field,
     * as an analysis field or input to another transform
     * @return
     * @throws TransformConfigurationException
     */
    private boolean checkTransformOutputIsUsed() throws TransformConfigurationException
    {
        Set<String> usedFields = new TransformConfigs(m_Transforms).inputFieldNames();
        usedFields.addAll(m_AnalysisConfig.analysisFields());
        boolean isSummarised = m_AnalysisConfig.getSummaryCountFieldName() != null &&
                                m_AnalysisConfig.getSummaryCountFieldName().isEmpty() == false;
        if (isSummarised)
        {
            usedFields.remove(m_AnalysisConfig.getSummaryCountFieldName());
        }

        String timeField = DataDescription.DEFAULT_TIME_FIELD;
        if (m_DataDescription != null)
        {
            timeField = m_DataDescription.getTimeField();
        }
        usedFields.add(timeField);

        for (TransformConfig tc : m_Transforms)
        {
            // if the type has no default outputs it doesn't need an output
            boolean usesAnOutput = tc.type().defaultOutputNames().isEmpty();
            for (String outputName : tc.getOutputs())
            {
                if (usedFields.contains(outputName))
                {
                    usesAnOutput = true;
                    break;
                }
            }

            if (isSummarised)
            {
                if (tc.getOutputs().contains(m_AnalysisConfig.getSummaryCountFieldName()))
                {
                    String msg = String.format("Transform '%s' has a output with the same name as the "
                            + "summary count field. Transform outputs cannot be used the summary count "
                            + "field please review your configuration",
                                tc.type().prettyName());

                    throw new TransformConfigurationException(msg, ErrorCode.DUPLICATED_TRANSFORM_OUTPUT_NAME);

                }
            }

            if (!usesAnOutput)
            {
                String msg = String.format("None of the outputs of transform '%s' are used."
                                            + " Please review your configuration",
                                                tc.type().prettyName());

                throw new TransformConfigurationException(msg, ErrorCode.TRANSFORM_OUTPUTS_UNUSED);
            }
        }

        return false;
    }

    private void checkAtLeastOneTransformIfDataFormatIsSingleLine() throws JobConfigurationException
    {
        if (m_DataDescription != null && m_DataDescription.getFormat() == DataFormat.SINGLE_LINE)
        {
            if (m_Transforms == null || m_Transforms.isEmpty())
            {
                String msg = Messages.getMessage(
                                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                                DataFormat.SINGLE_LINE);

                throw new JobConfigurationException(msg,
                        ErrorCode.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS);
            }
        }
    }

    private void checkValidId() throws JobConfigurationException
    {
        if (m_ID.length() > MAX_JOB_ID_LENGTH)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_ID_TOO_LONG, MAX_JOB_ID_LENGTH),
                    ErrorCode.JOB_ID_TOO_LONG);
        }

        for (Character ch : PROHIBITED_JOB_ID_CHARACTERS_SET)
        {
            if (m_ID.indexOf(ch) >= 0)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS,
                                ch, PROHIBITED_JOB_ID_CHARACTERS),
                        ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
        }

        for (char c : m_ID.toCharArray())
        {
            if (Character.isUpperCase(c))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_ID_CONTAINS_UPPERCASE_CHARS),
                        ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
            if (Character.isISOControl(c))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_ID_CONTAINS_CONTROL_CHARS),
                        ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
        }
    }
}
