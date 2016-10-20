/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.ListDocument;
import com.prelert.job.config.DefaultDetectorDescription;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.utils.PrelertStrings;
import com.prelert.utils.Strings;

public class FieldConfigWriter
{
    private static final String DETECTOR_PREFIX = "detector.";
    private static final String DETECTOR_CLAUSE_SUFFIX = ".clause";
    private static final String DETECTOR_RULES_SUFFIX = ".rules";
    private static final String INFLUENCER_PREFIX = "influencer.";
    private static final String CATEGORIZATION_FIELD_OPTION = " categorizationfield=";
    private static final String CATEGORIZATION_FILTER_PREFIX = "categorizationfilter.";
    private static final String LIST_PREFIX = "list.";

    // Note: for the Engine API summarycountfield is currently passed as a
    // command line option to prelert_autodetect rather than in the field
    // config file

    private static final char NEW_LINE = '\n';

    private final AnalysisConfig m_Config;
    private final Set<ListDocument> m_Lists;
    private final OutputStreamWriter m_Writer;
    private final Logger m_Logger;
    private final ObjectMapper m_ObjectMapper;

    public FieldConfigWriter(AnalysisConfig config, Set<ListDocument> lists,
            OutputStreamWriter writer, Logger logger)
    {
        m_Config = Objects.requireNonNull(config);
        m_Lists = Objects.requireNonNull(lists);
        m_Writer = Objects.requireNonNull(writer);
        m_Logger = Objects.requireNonNull(logger);
        m_ObjectMapper = new ObjectMapper();
    }

    /**
     * Write the Prelert autodetect field options to the output stream.
     *
     * @throws IOException
     */
    public void write() throws IOException
    {
        StringBuilder contents = new StringBuilder();

        writeDetectors(contents);
        writeLists(contents);
        writeAsEnumeratedSettings(CATEGORIZATION_FILTER_PREFIX, m_Config.getCategorizationFilters(),
                contents, true);

        // As values are written as entire settings rather than part of a
        // clause no quoting is needed
        writeAsEnumeratedSettings(INFLUENCER_PREFIX, m_Config.getInfluencers(), contents, false);

        m_Logger.debug("FieldConfig:\n" + contents.toString());

        m_Writer.write(contents.toString());
    }

    private void writeDetectors(StringBuilder contents) throws IOException
    {
        int counter = 0;
        for (Detector detector : m_Config.getDetectors())
        {
            int detectorId = counter++;
            writeDetectorClause(detectorId, detector, contents);
            writeDetectorRules(detectorId, detector, contents);
        }
    }

    private void writeDetectorClause(int detectorId, Detector detector, StringBuilder contents)
    {
        contents.append(DETECTOR_PREFIX).append(detectorId)
                .append(DETECTOR_CLAUSE_SUFFIX).append(EQUALS);

        DefaultDetectorDescription.appendOn(detector, contents);

        if (Strings.isNullOrEmpty(m_Config.getCategorizationFieldName()) == false)
        {
            contents.append(CATEGORIZATION_FIELD_OPTION)
                    .append(quoteField(m_Config.getCategorizationFieldName()));
        }

        contents.append(NEW_LINE);
    }

    private void writeDetectorRules(int detectorId, Detector detector, StringBuilder contents)
            throws IOException
    {
        List<DetectionRule> rules = detector.getDetectorRules();
        if (rules == null || rules.isEmpty())
        {
            return;
        }

        contents.append(DETECTOR_PREFIX).append(detectorId)
                .append(DETECTOR_RULES_SUFFIX).append(EQUALS);

        String rulesAsJson = m_ObjectMapper.writeValueAsString(detector.getDetectorRules());
        contents.append(rulesAsJson);

        contents.append(NEW_LINE);
    }

    private void writeLists(StringBuilder buffer) throws JsonProcessingException
    {
        for (ListDocument list : m_Lists)
        {
            String listAsJson = m_ObjectMapper.writeValueAsString(list.getItems());
            buffer.append(LIST_PREFIX).append(list.getId()).append(EQUALS).append(listAsJson)
                    .append(NEW_LINE);
        }
    }

    private static void writeAsEnumeratedSettings(String settingName, List<String> values,
            StringBuilder buffer, boolean quote)
    {
        if (values == null)
        {
            return;
        }

        int counter = 0;
        for (String value : values)
        {
            buffer.append(settingName).append(counter++).append(EQUALS)
                  .append(quote ? quoteField(value) : value).append(NEW_LINE);
        }
    }

    private static String quoteField(String field)
    {
        return PrelertStrings.doubleQuoteIfNotAlphaNumeric(field);
    }
}
