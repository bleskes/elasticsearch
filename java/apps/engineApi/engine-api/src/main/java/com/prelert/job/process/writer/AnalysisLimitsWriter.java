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
import static com.prelert.job.process.writer.WriterConstants.NEW_LINE;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

import com.prelert.job.AnalysisLimits;

public class AnalysisLimitsWriter
{
    /*
     * The configuration fields used in limits.conf
     */
    private static final String MEMORY_STANZA_STR = "[memory]";
    private static final String RESULTS_STANZA_STR = "[results]";
    private static final String MODEL_MEMORY_LIMIT_CONFIG_STR = "modelmemorylimit";
    private static final String MAX_EXAMPLES_LIMIT_CONFIG_STR = "maxexamples";

    private final AnalysisLimits m_Limits;
    private final OutputStreamWriter m_Writer;

    public AnalysisLimitsWriter(AnalysisLimits limits, OutputStreamWriter writer)
    {
        m_Limits = Objects.requireNonNull(limits);
        m_Writer = Objects.requireNonNull(writer);
    }

    public void write() throws IOException
    {
        StringBuilder contents = new StringBuilder(MEMORY_STANZA_STR).append(NEW_LINE);
        if (m_Limits.getModelMemoryLimit() != 0)
        {
            contents.append(MODEL_MEMORY_LIMIT_CONFIG_STR + EQUALS)
                    .append(m_Limits.getModelMemoryLimit()).append(NEW_LINE);
        }

        contents.append(RESULTS_STANZA_STR).append(NEW_LINE);
        if (m_Limits.getCategorizationExamplesLimit() != null)
        {
            contents.append(MAX_EXAMPLES_LIMIT_CONFIG_STR + EQUALS)
                    .append(m_Limits.getCategorizationExamplesLimit())
                    .append(NEW_LINE);
        }

        m_Writer.write(contents.toString());
    }
}
