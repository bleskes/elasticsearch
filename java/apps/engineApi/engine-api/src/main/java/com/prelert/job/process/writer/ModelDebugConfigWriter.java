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
import java.io.Writer;
import java.util.Objects;

import com.prelert.job.ModelDebugConfig;
import com.prelert.utils.Strings;

public class ModelDebugConfigWriter
{
    private static final String WRITE_TO_STR = "writeto";
    private static final String BOUNDS_PERCENTILE_STR = "boundspercentile";
    private static final String TERMS_STR = "terms";

    private final ModelDebugConfig m_ModelDebugConfig;
    private final Writer m_Writer;

    public ModelDebugConfigWriter(ModelDebugConfig modelDebugConfig, Writer writer)
    {
        m_ModelDebugConfig = Objects.requireNonNull(modelDebugConfig);
        m_Writer = Objects.requireNonNull(writer);
    }

    public void write() throws IOException
    {
        StringBuilder contents = new StringBuilder();
        if (m_ModelDebugConfig.getWriteTo() != null)
        {
            contents.append(WRITE_TO_STR)
                    .append(EQUALS)
                    .append(m_ModelDebugConfig.getWriteTo())
                    .append(NEW_LINE);
        }

        contents.append(BOUNDS_PERCENTILE_STR)
                .append(EQUALS)
                .append(m_ModelDebugConfig.getBoundsPercentile())
                .append(NEW_LINE);

        contents.append(TERMS_STR)
                .append(EQUALS)
                .append(Strings.nullToEmpty(m_ModelDebugConfig.getTerms()))
                .append(NEW_LINE);

        m_Writer.write(contents.toString());
    }
}
