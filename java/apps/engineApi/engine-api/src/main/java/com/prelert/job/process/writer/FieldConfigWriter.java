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

package com.prelert.job.process.writer;

import static com.prelert.job.process.writer.WriterConstants.EQUALS;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;

public class FieldConfigWriter
{

    private static final String CATEGORIZATION_FIELD_KEY = "categorizationfield";
    private static final String DOT_IS_ENABLED = ".isEnabled";
    private static final String DOT_USE_NULL = ".useNull";
    private static final String DOT_BY = ".by";
    private static final String DOT_OVER = ".over";
    private static final String DOT_PARTITION = ".partition";
    private static final String DOT_EXCLUDE_FREQUENT = ".excludefrequent";
    private static final String HYPHEN = "-";
    private static final String INFLUENCERS = "influencers";
    private static final char NEW_LINE = '\n';

    private final AnalysisConfig m_Config;
    private final OutputStreamWriter m_Writer;
    private final Logger m_Logger;

    public FieldConfigWriter(AnalysisConfig config, OutputStreamWriter writer, Logger logger)
    {
        m_Config = Objects.requireNonNull(config);
        m_Writer = Objects.requireNonNull(writer);
        m_Logger = Objects.requireNonNull(logger);
    }

    /**
     * Write the Prelert autodetect field options to the output stream.
     *
     * @throws IOException
     */
    public void write() throws IOException
    {
        StringBuilder contents = new StringBuilder();
        if (isNotNullOrEmpty(m_Config.getCategorizationFieldName()))
        {
            contents.append(CATEGORIZATION_FIELD_KEY).append(EQUALS)
                    .append(m_Config.getCategorizationFieldName()).append(NEW_LINE);
        }

        if (m_Config.getInfluencers().size() > 0)
        {
            contents.append(INFLUENCERS).append(EQUALS);
            StringBuilder fieldsBuilder = new StringBuilder();

            for (String influencer : m_Config.getInfluencers())
            {
                fieldsBuilder.append(influencer).append(HYPHEN);
            }
            int lastHyphen = fieldsBuilder.lastIndexOf(HYPHEN);
            fieldsBuilder.setLength(lastHyphen);
            contents.append(fieldsBuilder.toString()).append(NEW_LINE);
        }

        Set<String> detectorKeys = new HashSet<>();
        for (Detector detector : m_Config.getDetectors())
        {
            StringBuilder keyBuilder = new StringBuilder();
            if (isNotNullOrEmpty(detector.getFunction()))
            {
                keyBuilder.append(detector.getFunction());
                if (detector.getFieldName() != null)
                {
                    keyBuilder.append("(")
                            .append(detector.getFieldName())
                            .append(")");
                }
            }
            else if (isNotNullOrEmpty(detector.getFieldName()))
            {
                keyBuilder.append(detector.getFieldName());
            }

            if (isNotNullOrEmpty(detector.getByFieldName()))
            {
                keyBuilder.append(HYPHEN).append(detector.getByFieldName());
            }
            if (isNotNullOrEmpty(detector.getOverFieldName()))
            {
                keyBuilder.append(HYPHEN).append(detector.getOverFieldName());
            }
            if (isNotNullOrEmpty(detector.getPartitionFieldName()))
            {
                keyBuilder.append(HYPHEN).append(detector.getPartitionFieldName());
            }

            String key = keyBuilder.toString();
            if (detectorKeys.contains(key))
            {
                m_Logger.warn(String.format(
                        "Duplicate detector key '%s' ignorning this detector", key));
                continue;
            }
            detectorKeys.add(key);

            // .isEnabled is only necessary if nothing else is going to be added
            // for this key
            if (detector.isUseNull() == null &&
                    isNullOrEmpty(detector.getByFieldName()) &&
                    isNullOrEmpty(detector.getOverFieldName()) &&
                    isNullOrEmpty(detector.getPartitionFieldName()))
            {
                contents.append(key).append(DOT_IS_ENABLED).append(" = true").append(NEW_LINE);
            }

            if (detector.isUseNull() != null)
            {
                contents.append(key).append(DOT_USE_NULL)
                    .append(detector.isUseNull() ? " = true" : " = false")
                    .append(NEW_LINE);
            }

            if (isNotNullOrEmpty(detector.getExcludeFrequent()))
            {
                contents.append(key).append(DOT_EXCLUDE_FREQUENT).append(EQUALS).
                    append(detector.getExcludeFrequent()).append(NEW_LINE);
            }
            if (isNotNullOrEmpty(detector.getByFieldName()))
            {
                contents.append(key).append(DOT_BY).append(EQUALS)
                        .append(detector.getByFieldName()).append(NEW_LINE);
            }
            if (isNotNullOrEmpty(detector.getOverFieldName()))
            {
                contents.append(key).append(DOT_OVER).append(EQUALS)
                        .append(detector.getOverFieldName()).append(NEW_LINE);
            }
            if (isNotNullOrEmpty(detector.getPartitionFieldName()))
            {
                contents.append(key).append(DOT_PARTITION).append(EQUALS)
                        .append(detector.getPartitionFieldName()).append(NEW_LINE);
            }
        }

        m_Logger.debug("FieldConfig: \n" + contents.toString());

        m_Writer.write(contents.toString());
    }

    private static boolean isNotNullOrEmpty(String arg)
    {
        return arg != null && arg.isEmpty() == false;
    }

    private static boolean isNullOrEmpty(String arg)
    {
        return arg == null || arg.isEmpty();
    }
}
