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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

/**
 * Anomaly Record POJO
 * Uses the object wrappers Boolean Double so null values
 * can be returned if the members are not set.
 */
@JsonInclude(Include.NON_NULL)
public class AnomalyRecord
{
	/**
	 * Serialisation fields
	 */
	static final public String DETECTOR_NAME = "detectorName";
	static final public String TYPE = "record";

	/**
	 * Result fields (all detector types)
	 */
	static final public String ANOMALY_SCORE =  "anomalyScore";
	static final public String PROBABILITY = "probability";
	static final public String BY_FIELD_NAME = "byFieldName";
	static final public String BY_FIELD_VALUE = "byFieldValue";
	static final public String PARTITION_FIELD_NAME = "partitionFieldName";
	static final public String PARTITION_FIELD_VALUE = "partitionFieldValue";
	static final public String FUNCTION = "function";
	static final public String TYPICAL = "typical";
	static final public String ACTUAL = "actual";

	/**
	 * Metric Results (including population metrics)
	 */
	static final public String FIELD_NAME = "fieldName";

	/**
	 * Population results
	 */
	static final public String OVER_FIELD_NAME = "overFieldName";
	static final public String OVER_FIELD_VALUE = "overFieldValue";
	static final public String IS_OVERALL_RESULT = "isOverallResult";

	/**
	 * Simple count detector
	 */
	static final public String IS_SIMPLE_COUNT = "isSimpleCount";

	private static final Logger s_Logger = Logger.getLogger(AnomalyRecord.class);
	
	private String m_DetectorName;

	private Double m_AnomalyScore;
	private Double m_Probability;
	private String m_ByFieldName;
	private String m_ByFieldValue;
	private String m_PartitionFieldName;
	private String m_PartitionFieldValue;
	private String m_Function;
	private Double m_Typical;
	private Double m_Actual;

	private String m_FieldName;

	private String m_OverFieldName;
	private String m_OverFieldValue;
	private Boolean m_IsOverallResult;

	private Boolean m_IsSimpleCount;


	public Double getAnomalyScore()
	{
		return m_AnomalyScore;
	}

	public void setAnomalyScore(Double anomalyScore)
	{
		m_AnomalyScore = anomalyScore;
	}

	public Double getProbability()
	{
		return m_Probability;
	}

	public void setProbability(Double value)
	{
		m_Probability = value;
	}


	public String getByFieldName()
	{
		return m_ByFieldName;
	}

	public void setByFieldName(String value)
	{
		m_ByFieldName = value;
	}

	public String getByFieldValue()
	{
		return m_ByFieldValue;
	}

	public void setByFieldValue(String value)
	{
		m_ByFieldValue = value;
	}

	public String getPartitionFieldName()
	{
		return m_PartitionFieldName;
	}

	public void setPartitionFieldName(String field)
	{
		m_PartitionFieldName = field;
	}

	public String getPartitionFieldValue()
	{
		return m_PartitionFieldValue;
	}

	public void setPartitionFieldValue(String value)
	{
		m_PartitionFieldValue = value;
	}

	public String getFunction()
	{
		return m_Function;
	}

	public void setFunction(String name)
	{
		m_Function = name;
	}

	public Double getTypical()
	{
		return m_Typical;
	}

	public void setTypical(Double typical)
	{
		m_Typical = typical;
	}

	public Double getActual()
	{
		return m_Actual;
	}

	public void setActual(Double actual)
	{
		m_Actual = actual;
	}

	public String getFieldName()
	{
		return m_FieldName;
	}

	public void setFieldName(String field)
	{
		m_FieldName = field;
	}

	public String getOverFieldName()
	{
		return m_OverFieldName;
	}

	public void setOverFieldName(String name)
	{
		m_OverFieldName = name;
	}

	public String getOverFieldValue()
	{
		return m_OverFieldValue;
	}

	public void setOverFieldValue(String value)
	{
		m_OverFieldValue = value;
	}


	public Boolean isOverallResult()
	{
		return m_IsOverallResult;
	}

	public void setOverallResult(boolean value)
	{
		m_IsOverallResult = value;
	}

	
	public String getDetectorName()
	{
		return m_DetectorName;
	}

	public void setDetectorName(String name)
	{
		m_DetectorName = name;
	}
	

	public Boolean isSimpleCount()
	{
		return m_IsSimpleCount;
	}

	public void setSimpleCount(boolean value)
	{
		m_IsSimpleCount = value;
	}


	/**
	 * Create a new <code>AnomalyRecord</code> and populate it from the JSON parser.
	 * The parser must be pointing at the start of the object then all the object's
	 * fields are read and if they match the property names then the appropriate
	 * members are set.
	 *
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 *
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public AnomalyRecord parseJson(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		AnomalyRecord record = new AnomalyRecord();

		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse anomaly record. First token '" +
					parser.getText() + ", is not the start object token";
			s_Logger.error(msg);
			throw new AutoDetectParseException(msg);
		}

		token = parser.nextToken();
		while (token != JsonToken.END_OBJECT)
		{
			switch(token)
			{
			case START_OBJECT:
				s_Logger.error("Start object parsed in bucket");
				break;
			case END_OBJECT:
				s_Logger.error("End object parsed in bucket");
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				switch (fieldName)
				{
				case ANOMALY_SCORE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
					{
						record.setAnomalyScore(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
										+ " as a double");
					}
					break;
				case PROBABILITY:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
					{
						record.setProbability(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a double");
					}
					break;
				case BY_FIELD_NAME:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setByFieldName(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case BY_FIELD_VALUE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setByFieldValue(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case PARTITION_FIELD_NAME:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setPartitionFieldName(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case PARTITION_FIELD_VALUE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setPartitionFieldValue(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case FUNCTION:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setFunction(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case TYPICAL:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
					{
						record.setTypical(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a double");
					}
					break;
				case ACTUAL:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
					{
						record.setActual(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a double");
					}
					break;
				case FIELD_NAME:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setFieldName(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case OVER_FIELD_NAME:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setOverFieldName(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case OVER_FIELD_VALUE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						record.setOverFieldValue(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a string");
					}
					break;
				case IS_OVERALL_RESULT:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE)
					{
						record.setOverallResult(parser.getBooleanValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a bool");
					}
					break;
				case IS_SIMPLE_COUNT:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE)
					{
						record.setSimpleCount(parser.getBooleanValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
								+ " as a bool");
					}
					break;
				default:
					s_Logger.warn(String.format("Parse error unknown field in Anomaly Record %s:%s",
							fieldName, parser.nextTextValue()));
					break;
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in Anomaly Record not "
								+ token);
				break;
			}

			token = parser.nextToken();
		}

		return record;
	}
}
