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
package com.prelert.transforms;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Matches a field against a regex
 */
public class ExcludeFilter extends Transform
{
    private final Pattern m_Pattern;

    public ExcludeFilter(String regex, List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);

        m_Pattern = Pattern.compile(regex);
    }

    /**
     * Returns {@link TransformResult#FATAL_FAIL} if the record matches the regex
     */
    @Override
    public TransformResult transform(String[][] readWriteArea)
    throws TransformException
    {
        TransformIndex readIndex = m_ReadIndicies.get(0);
        String field = readWriteArea[readIndex.array][readIndex.index];

        Matcher match = m_Pattern.matcher(field);

        return match.matches() ? TransformResult.FATAL_FAIL : TransformResult.OK;
    }

}
