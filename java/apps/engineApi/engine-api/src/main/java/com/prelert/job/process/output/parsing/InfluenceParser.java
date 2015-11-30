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
package com.prelert.job.process.output.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.Influence;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;


final class InfluenceParser extends FieldNameParser<List<Influence>>
{
    private static final Logger LOGGER = Logger.getLogger(InfluenceParser.class);

    public InfluenceParser(JsonParser jsonParser)
    {
        super("Influences", jsonParser, LOGGER);
    }

    @Override
    protected List<Influence> supply()
    {
        return new ArrayList<>();
    }

    @Override
    protected void handleFieldName(String fieldName, List<Influence> influences)
            throws AutoDetectParseException, JsonParseException, IOException
    {
        Influence influence = new Influence();
        influence.setInfluencerFieldName(fieldName);
        m_Parser.nextToken();
        influence.setInfluencerFieldValues(parseValues(fieldName));

        influences.add(influence);
    }

    private List<String> parseValues(String fieldName) throws AutoDetectParseException,
            IOException, JsonParseException
    {
        List<String> influenceValues = new ArrayList<>();
        parseArray(fieldName, () -> parseAsStringOrNull(fieldName), influenceValues);
        return influenceValues;
    }
}

