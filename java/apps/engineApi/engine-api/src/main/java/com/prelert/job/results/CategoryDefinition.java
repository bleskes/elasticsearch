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

package com.prelert.job.results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Objects;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

@JsonInclude(Include.NON_NULL)
public class CategoryDefinition
{
    public static final String TYPE = "categoryDefinition";
    public static final String CATEGORY_ID = "categoryId";
    public static final String TERMS = "terms";
    public static final String REGEX = "regex";
    public static final String EXAMPLES = "examples";

    private static final Logger LOGGER = Logger.getLogger(CategoryDefinition.class);

    private long m_Id = 0L;
    private String m_Terms = "";
    private String m_Regex = "";
    private final Set<String> m_Examples = new TreeSet<>();

    public long getCategoryId()
    {
        return m_Id;
    }

    public void setCategoryId(long categoryId)
    {
        m_Id = categoryId;
    }

    public String getTerms()
    {
        return m_Terms;
    }

    public void setTerms(String terms)
    {
        m_Terms = terms;
    }

    public String getRegex()
    {
        return m_Regex;
    }

    public void setRegex(String regex)
    {
        m_Regex = regex;
    }

    public List<String> getExamples()
    {
        return new ArrayList<>(m_Examples);
    }

    public void setExamples(Collection<String> examples)
    {
        m_Examples.clear();
        m_Examples.addAll(examples);
    }

    public void addExample(String example)
    {
        m_Examples.add(example);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other instanceof CategoryDefinition == false)
        {
            return false;
        }
        CategoryDefinition that = (CategoryDefinition) other;
        return Objects.equal(this.m_Id, that.m_Id)
                && Objects.equal(this.m_Terms, that.m_Terms)
                && Objects.equal(this.m_Regex, that.m_Regex)
                && Objects.equal(this.m_Examples, that.m_Examples);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(m_Id, m_Terms, m_Regex, m_Examples);
    }

    /**
     * Create a new <code>CategoryDefinition</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new CategoryDefinition
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static CategoryDefinition parseJson(JsonParser parser)
            throws JsonParseException, IOException, AutoDetectParseException
    {
        CategoryDefinition category = new CategoryDefinition();
        CategoryDefinitionJsonParser categoryJsonParser = new CategoryDefinitionJsonParser(parser,
                LOGGER);
        categoryJsonParser.parse(category);
        return category;
    }

    /**
     * Create a new <code>CategoryDefinition</code> and populate it from the
     * JSON parser.  The parser must be pointing at the first token inside the
     * object.  It is assumed that prior code has validated that the previous
     * token was the start of an object.  Then all the object's fields are read
     * and if they match the property names the appropriate members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new CategoryDefinition
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static CategoryDefinition parseJsonAfterStartObject(JsonParser parser)
            throws JsonParseException, IOException, AutoDetectParseException
    {
        CategoryDefinition category = new CategoryDefinition();
        CategoryDefinitionJsonParser categoryJsonParser =
                new CategoryDefinitionJsonParser(parser, LOGGER);
        categoryJsonParser.parseAfterStartObject(category);
        return category;
    }

    private static class CategoryDefinitionJsonParser extends FieldNameParser<CategoryDefinition>
    {
        public CategoryDefinitionJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("CategoryDefinition", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, CategoryDefinition category)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case TYPE:
                category.setCategoryId(parseAsLongOrZero(token, fieldName));
                break;
            case TERMS:
                category.setTerms(parseAsStringOrNull(token, fieldName));
                break;
            case REGEX:
                category.setRegex(parseAsStringOrNull(token, fieldName));
                break;
            case EXAMPLES:
                parseExamples(token, fieldName, category);
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in CategoryDefinition %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }

        private void parseExamples(JsonToken token, String fieldName, CategoryDefinition category)
                throws IOException, JsonParseException, AutoDetectParseException
        {
            if (token != JsonToken.START_ARRAY)
            {
                String msg = "Invalid value Expecting an array of examples";
                LOGGER.warn(msg);
                throw new AutoDetectParseException(msg);
            }

            token = m_Parser.nextToken();
            while (token != JsonToken.END_ARRAY)
            {
                category.addExample(parseAsStringOrNull(token, fieldName));
                token = m_Parser.nextToken();
            }
        }
    }
}
