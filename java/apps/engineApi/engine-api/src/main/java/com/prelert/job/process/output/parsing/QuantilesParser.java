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
package com.prelert.job.process.output.parsing;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.utils.json.FieldNameParser;

final class QuantilesParser extends FieldNameParser<Quantiles>
{
    private static final Logger LOGGER = Logger.getLogger(QuantilesParser.class);

    public QuantilesParser(JsonParser jsonParser)
    {
        super("Quantiles", jsonParser, LOGGER);
    }

    @Override
    protected Quantiles supply()
    {
        return new Quantiles();
    }

    @Override
    protected void handleFieldName(String fieldName, Quantiles quantiles) throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case Quantiles.TIMESTAMP:
            long seconds = parseAsLongOrZero(fieldName);
            // convert seconds to ms
            quantiles.setTimestamp(new Date(seconds * 1000));
            break;
        case Quantiles.QUANTILE_STATE:
            quantiles.setQuantileState(parseAsStringOrNull(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in Quantiles %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
