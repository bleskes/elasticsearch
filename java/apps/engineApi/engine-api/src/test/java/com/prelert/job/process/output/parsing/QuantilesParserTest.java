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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.utils.json.AutoDetectParseException;

public class QuantilesParserTest
{
    @Test
    public void testParseJson()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{\"timestamp\": 1,"
                     + " \"quantileState\": \"quantile-state\"}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        Quantiles quantile = new QuantilesParser(parser).parseJson();
        assertEquals("quantile-state", quantile.getState());
        assertEquals(new Date(1000l), quantile.getTimestamp());
        assertEquals(Quantiles.QUANTILES_ID, quantile.getId());
        assertEquals(Quantiles.CURRENT_VERSION, quantile.getVersion());


        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static final JsonParser createJsonParser(String input) throws JsonParseException,
            IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }

}
