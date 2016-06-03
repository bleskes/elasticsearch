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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.utils.json.FieldNameParser;

final class CategoryDefinitionParser extends FieldNameParser<CategoryDefinition>
{
    private static final Logger LOGGER = Logger.getLogger(CategoryDefinitionParser.class);

    public CategoryDefinitionParser(JsonParser jsonParser)
    {
        super("CategoryDefinition", jsonParser, LOGGER);
    }

    @Override
    protected CategoryDefinition supply()
    {
        return new CategoryDefinition();
    }

    @Override
    protected void handleFieldName(String fieldName, CategoryDefinition category) throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case CategoryDefinition.TYPE:
            category.setCategoryId(parseAsLongOrZero(fieldName));
            break;
        case CategoryDefinition.TERMS:
            category.setTerms(parseAsStringOrNull(fieldName));
            break;
        case CategoryDefinition.REGEX:
            category.setRegex(parseAsStringOrNull(fieldName));
            break;
        case CategoryDefinition.MAX_MATCHING_LENGTH:
            category.setMaxMatchingLength(parseAsLongOrZero(fieldName));
            break;
        case CategoryDefinition.EXAMPLES:
            category.setExamples(parseExamples(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in CategoryDefinition %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }

    private List<String> parseExamples(String fieldName) throws IOException
    {
        List<String> examples = new ArrayList<>();
        parseArray(fieldName, () -> parseAsStringOrNull(fieldName), examples);
        return examples;
    }
}
