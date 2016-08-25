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
 ***********************************************************/
package com.prelert.job.process.output.parsing;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Bucket.PartitionScore;
import com.prelert.utils.json.FieldNameParser;

public class PartitionScoreParser extends FieldNameParser<PartitionScore>
{
    private static final Logger LOGGER = Logger.getLogger(PartitionScoreParser.class);

    public PartitionScoreParser(JsonParser jsonParser)
    {
        super(Bucket.PARTITION_SCORES, jsonParser, LOGGER);
    }

    @Override
    protected PartitionScore supply()
    {
        return new PartitionScore();
    }

    @Override
    protected void handleFieldName(String fieldName, PartitionScore score) throws IOException
    {
        score.m_PartitionFieldValue = fieldName;
        m_Parser.nextToken();
        score.m_AnomalyScore = parseAsDoubleOrZero(fieldName);
    }
}
