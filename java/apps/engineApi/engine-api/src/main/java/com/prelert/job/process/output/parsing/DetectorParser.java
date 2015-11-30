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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Detector;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

final class DetectorParser extends FieldNameParser<Detector>
{
    private static final Logger LOGGER = Logger.getLogger(DetectorParser.class);

    public DetectorParser(JsonParser jsonParser)
    {
        super("Detector", jsonParser, LOGGER);
    }

    @Override
    protected Detector supply()
    {
        return new Detector();
    }

    @Override
    protected void handleFieldName(String fieldName, Detector detector)
            throws AutoDetectParseException, JsonParseException, IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case Detector.NAME:
            detector.setName(parseAsStringOrNull(fieldName));
            break;
        case Detector.RECORDS:
            if (token == JsonToken.START_ARRAY)
            {
                token = m_Parser.nextToken();
                while (token != JsonToken.END_ARRAY)
                {
                    AnomalyRecord record = new AnomalyRecordParser(m_Parser).parseJson();
                    detector.addRecord(record);

                    token = m_Parser.nextToken();
                }
            }
            else
            {
                LOGGER.warn("Expected the start of an array for field '"
                            + fieldName + "' not " + m_Parser.getText());
            }
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in detector %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
