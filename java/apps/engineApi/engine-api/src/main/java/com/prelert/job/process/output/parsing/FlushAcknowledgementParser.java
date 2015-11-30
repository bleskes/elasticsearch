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
import com.prelert.job.process.output.FlushAcknowledgement;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

final class FlushAcknowledgementParser extends FieldNameParser<FlushAcknowledgement>
{
    private static final Logger LOGGER = Logger.getLogger(FlushAcknowledgementParser.class);

    public FlushAcknowledgementParser(JsonParser jsonParser)
    {
        super("FlushAcknowledgement", jsonParser, LOGGER);
    }

    @Override
    protected FlushAcknowledgement supply()
    {
        return new FlushAcknowledgement();
    }

    @Override
    protected void handleFieldName(String fieldName, FlushAcknowledgement ack)
            throws AutoDetectParseException, JsonParseException, IOException
    {
        JsonToken token = m_Parser.nextToken();
        if (FlushAcknowledgement.FLUSH.equals(fieldName))
        {
            ack.setId(parseAsStringOrNull(fieldName));
        }
        else
        {
            LOGGER.warn(String.format("Parse error unknown field in FlushAcknowledgement %s:%s",
                    fieldName, token.asString()));
        }
    }
}

