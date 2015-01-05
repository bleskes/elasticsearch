/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.rs.data;

import java.io.IOException;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Anomaly Cause POJO.
 * Used as a nested level inside population anomaly records.
 */
@JsonInclude(Include.NON_NULL)
public class AnomalyCause
{
    /**
     * Result fields
     */
    public static final String PROBABILITY = "probability";
    public static final String OVER_FIELD_NAME = "overFieldName";
    public static final String OVER_FIELD_VALUE = "overFieldValue";
    public static final String BY_FIELD_NAME = "byFieldName";
    public static final String BY_FIELD_VALUE = "byFieldValue";
    public static final String PARTITION_FIELD_NAME = "partitionFieldName";
    public static final String PARTITION_FIELD_VALUE = "partitionFieldValue";
    public static final String FUNCTION = "function";
    public static final String TYPICAL = "typical";
    public static final String ACTUAL = "actual";

    /**
     * Metric Results
     */
    public static final String FIELD_NAME = "fieldName";

    private static final Logger LOGGER = Logger.getLogger(AnomalyCause.class);

    private double m_Probability;
    private String m_ByFieldName;
    private String m_ByFieldValue;
    private String m_PartitionFieldName;
    private String m_PartitionFieldValue;
    private String m_Function;
    private double m_Typical;
    private double m_Actual;

    private String m_FieldName;

    private String m_OverFieldName;
    private String m_OverFieldValue;


    public double getProbability()
    {
        return m_Probability;
    }

    public void setProbability(double value)
    {
        m_Probability = value;
    }


    public String getByFieldName()
    {
        return m_ByFieldName;
    }

    public void setByFieldName(String value)
    {
        m_ByFieldName = value.intern();
    }

    public String getByFieldValue()
    {
        return m_ByFieldValue;
    }

    public void setByFieldValue(String value)
    {
        m_ByFieldValue = value.intern();
    }

    public String getPartitionFieldName()
    {
        return m_PartitionFieldName;
    }

    public void setPartitionFieldName(String field)
    {
        m_PartitionFieldName = field.intern();
    }

    public String getPartitionFieldValue()
    {
        return m_PartitionFieldValue;
    }

    public void setPartitionFieldValue(String value)
    {
        m_PartitionFieldValue = value.intern();
    }

    public String getFunction()
    {
        return m_Function;
    }

    public void setFunction(String name)
    {
        m_Function = name.intern();
    }

    public double getTypical()
    {
        return m_Typical;
    }

    public void setTypical(double typical)
    {
        m_Typical = typical;
    }

    public double getActual()
    {
        return m_Actual;
    }

    public void setActual(double actual)
    {
        m_Actual = actual;
    }

    public String getFieldName()
    {
        return m_FieldName;
    }

    public void setFieldName(String field)
    {
        m_FieldName = field.intern();
    }

    public String getOverFieldName()
    {
        return m_OverFieldName;
    }

    public void setOverFieldName(String name)
    {
        m_OverFieldName = name.intern();
    }

    public String getOverFieldValue()
    {
        return m_OverFieldValue;
    }

    public void setOverFieldValue(String value)
    {
        m_OverFieldValue = value.intern();
    }


    /**
     * Create a new <code>AnomalyCause</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new AnomalyCause
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static AnomalyCause parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        AnomalyCause cause = new AnomalyCause();

        JsonToken token = parser.getCurrentToken();
        if (JsonToken.START_OBJECT != token)
        {
            String msg = "Cannot parse anomaly cause. First token '" +
                    parser.getText() + ", is not the start object token";
            LOGGER.error(msg);
            throw new AutoDetectParseException(msg);
        }

        token = parser.nextToken();
        while (token != JsonToken.END_OBJECT)
        {
            switch(token)
            {
            case START_OBJECT:
                LOGGER.error("Start object parsed in anomaly cause");
                break;
            case END_OBJECT:
                LOGGER.error("End object parsed in anomaly cause");
                break;
            case FIELD_NAME:
                String fieldName = parser.getCurrentName();
                switch (fieldName)
                {
                case PROBABILITY:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
                    {
                        cause.setProbability(parser.getDoubleValue());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a double");
                    }
                    break;
                case BY_FIELD_NAME:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setByFieldName(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case BY_FIELD_VALUE:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setByFieldValue(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case PARTITION_FIELD_NAME:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setPartitionFieldName(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case PARTITION_FIELD_VALUE:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setPartitionFieldValue(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case FUNCTION:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setFunction(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case TYPICAL:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
                    {
                        cause.setTypical(parser.getDoubleValue());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a double");
                    }
                    break;
                case ACTUAL:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
                    {
                        cause.setActual(parser.getDoubleValue());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a double");
                    }
                    break;
                case FIELD_NAME:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setFieldName(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case OVER_FIELD_NAME:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setOverFieldName(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                case OVER_FIELD_VALUE:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_STRING)
                    {
                        cause.setOverFieldValue(parser.getText());
                    }
                    else
                    {
                        LOGGER.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                + " as a string");
                    }
                    break;
                default:
                    LOGGER.warn(String.format("Parse error unknown field in Anomaly Cause %s:%s",
                            fieldName, parser.nextToken().asString()));
                    break;
                }
                break;
            default:
                LOGGER.warn("Parsing error: Only simple fields expected in Anomaly Cause not "
                                + token);
                break;
            }

            token = parser.nextToken();
        }

        return cause;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(m_Probability);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(m_Actual);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(m_Typical);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result
                + ((m_ByFieldName == null) ? 0 : m_ByFieldName.hashCode());
        result = prime * result
                + ((m_ByFieldValue == null) ? 0 : m_ByFieldValue.hashCode());
        result = prime * result
                + ((m_FieldName == null) ? 0 : m_FieldName.hashCode());
        result = prime * result
                + ((m_Function == null) ? 0 : m_Function.hashCode());
        result = prime * result
                + ((m_OverFieldName == null) ? 0 : m_OverFieldName.hashCode());
        result = prime
                * result
                + ((m_OverFieldValue == null) ? 0 : m_OverFieldValue.hashCode());
        result = prime
                * result
                + ((m_PartitionFieldName == null) ? 0 : m_PartitionFieldName
                        .hashCode());
        result = prime
                * result
                + ((m_PartitionFieldValue == null) ? 0 : m_PartitionFieldValue
                        .hashCode());

        return result;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof AnomalyCause == false)
        {
            return false;
        }

        AnomalyCause that = (AnomalyCause)other;

        return this.m_Probability == that.m_Probability &&
                Objects.equals(this.m_Typical, that.m_Typical) &&
                Objects.equals(this.m_Actual, that.m_Actual) &&
                Objects.equals(this.m_Function, that.m_Function) &&
                Objects.equals(this.m_FieldName, that.m_FieldName) &&
                Objects.equals(this.m_ByFieldName, that.m_ByFieldName) &&
                Objects.equals(this.m_ByFieldValue, that.m_ByFieldValue) &&
                Objects.equals(this.m_PartitionFieldName, that.m_PartitionFieldName) &&
                Objects.equals(this.m_PartitionFieldValue, that.m_PartitionFieldValue) &&
                Objects.equals(this.m_OverFieldName, that.m_OverFieldName) &&
                Objects.equals(this.m_OverFieldValue, that.m_OverFieldValue);
    }

}
