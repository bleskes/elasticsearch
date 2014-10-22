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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

/**
 * Anomaly Record POJO.
 * Uses the object wrappers Boolean and Double so <code>null</code> values
 * can be returned if the members have not been set.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties({"parent", "id", "detectorName"})
public class AnomalyRecord
{
	/**
	 * Serialisation fields
	 */
	static final public String TYPE = "record";

	/**
	 * Data store ID field
	 */
	static final public String ID = "id";

	/**
	 * Result fields (all detector types)
	 */
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
	static final public String CAUSES = "causes";

	/**
	 * Normalisation
	 */
	static final public String ANOMALY_SCORE = "anomalyScore";
	static final public String NORMALIZED_PROBABILITY = "normalizedProbability";
	
	private static final Logger s_Logger = Logger.getLogger(AnomalyRecord.class);
	
	private String m_DetectorName;
	private int m_IdNum;
	private double m_Probability;
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
	private List<AnomalyCause> m_Causes;

	private double m_AnomalyScore;
	private double m_NormalizedProbability;
	private Date   m_Timestamp;

	private boolean m_HadBigNormalisedUpdate;

	private String m_Parent;

	/**
	 * Data store ID of this record.  May be null for records that have not been
	 * read from the data store.
	 */
	public String getId()
	{
		if (m_IdNum == 0)
		{
			return null;
		}
		return m_Parent + m_DetectorName + m_IdNum;
	}

	/**
	 * This should only be called by code that's reading records from the data
	 * store.  The ID must be set to the data stores's unique key to this
	 * anomaly record.
	 *
	 * TODO - this is a breach of encapsulation that should be rectified when
	 * a big enough change is made to justify invalidating all previously
	 * stored data.  Currently it makes an assumption about the format of the
	 * detector name, which should be opaque to the Java code.
	 */
	public void setId(String id)
	{
		final int epochLen = 10;
		int idStart = -1;
		if (m_PartitionFieldValue == null || m_PartitionFieldValue.isEmpty())
		{
			idStart = id.lastIndexOf("/") + 1;
		}
		else
		{
			idStart = id.lastIndexOf("/" + m_PartitionFieldValue);
			if (idStart >= epochLen)
			{
				idStart += 1 + m_PartitionFieldValue.length();
			}
		}
		if (idStart <= epochLen)
		{
			return;
		}
		m_Parent = id.substring(0, epochLen).intern();
		m_DetectorName = id.substring(epochLen, idStart).intern();
		m_IdNum = Integer.parseInt(id.substring(idStart));
	}


	/**
	 * Generate the data store ID for this record.
	 *
	 * TODO - the current format is hard to parse back into its constituent
	 * parts, but cannot be changed without breaking backwards compatibility.
	 * If backwards compatibility is ever broken for some other reason then the
	 * opportunity should be taken to change this format.
	 */
	public String generateNewId(String parent, String detectorName, int count)
	{
		m_Parent = parent.intern();
		m_DetectorName = detectorName.intern();
		m_IdNum = count;
		return getId();
	}


	public double getAnomalyScore()
	{
		return m_AnomalyScore;
	}
	
	public void setAnomalyScore(double anomalyScore)
	{
		m_HadBigNormalisedUpdate |= isBigUpdate(m_AnomalyScore, anomalyScore);
		m_AnomalyScore = anomalyScore;
	}
	
	public double getNormalizedProbability()
	{
		return m_NormalizedProbability;
	}
	
	public void setNormalizedProbability(double normalizedProbability)
	{
		m_HadBigNormalisedUpdate |= isBigUpdate(m_NormalizedProbability, normalizedProbability);
		m_NormalizedProbability = normalizedProbability;
	}
	
	
	public Date getTimestamp() 
	{
		return m_Timestamp;
	}
	
	public void setTimestamp(Date timestamp) 
	{
		m_Timestamp = timestamp;
	}
	
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

	public List<AnomalyCause> getCauses()
	{
		return m_Causes;
	}

	public void setCauses(List<AnomalyCause> causes)
	{
		m_Causes = causes;
	}

	private void addCause(AnomalyCause cause)
	{
		if (m_Causes == null)
		{
			m_Causes = new ArrayList<>();
		}
		m_Causes.add(cause);
	}

	public String getParent()
	{
		return m_Parent;
	}
	
	public void setParent(String parent)
	{
		m_Parent = parent.intern();
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
	 * @return The new AnomalyRecord
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
				s_Logger.error("Start object parsed in anomaly record");
				break;
			case END_OBJECT:
				s_Logger.error("End object parsed in anomaly record");
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				switch (fieldName)
				{
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
				case NORMALIZED_PROBABILITY:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
					{
						record.setNormalizedProbability(parser.getDoubleValue());
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
				case CAUSES:
					token = parser.nextToken();
					if (token != JsonToken.START_ARRAY)
					{
						String msg = "Invalid value Expecting an array of causes";
						s_Logger.warn(msg);
						throw new AutoDetectParseException(msg);
					}
					
					token = parser.nextToken();
					while (token != JsonToken.END_ARRAY)
					{
						AnomalyCause cause = AnomalyCause.parseJson(parser);
						record.addCause(cause);
						
						token = parser.nextToken();
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
	
	
	private boolean bothNullOrEqual(Object o1, Object o2)
	{
		if (o1 == null && o2 == null)
		{
			return true;
		}
		
		if (o1 == null || o2 == null)
		{
			return false;
		}
		
		return o1.equals(o2);	
	}

	@Override
	public int hashCode()
	{
		// ID is NOT included in the hash, so that a record from the data store
		// will hash the same as a record representing the same anomaly that did
		// not come from the data store

		// m_HadBigNormalisedUpdate is also deliberately excluded from the hash
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(m_Probability);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(m_AnomalyScore);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(m_NormalizedProbability);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((m_Actual == null) ? 0 : m_Actual.hashCode());
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
		result = prime * result
				+ ((m_Causes == null) ? 0 : m_Causes.hashCode());
		result = prime * result
				+ ((m_Parent == null) ? 0 : m_Parent.hashCode());
		result = prime
				* result
				+ ((m_PartitionFieldName == null) ? 0 : m_PartitionFieldName
						.hashCode());
		result = prime
				* result
				+ ((m_PartitionFieldValue == null) ? 0 : m_PartitionFieldValue
						.hashCode());
		result = prime * result
				+ ((m_Timestamp == null) ? 0 : m_Timestamp.hashCode());
		result = prime * result
				+ ((m_Typical == null) ? 0 : m_Typical.hashCode());

		return result;
	}

	
	@Override 
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof AnomalyRecord == false)
		{
			return false;
		}
		
		AnomalyRecord that = (AnomalyRecord)other;

		// ID is NOT compared, so that a record from the data store will compare
		// equal to a record representing the same anomaly that did not come
		// from the data store

		// m_HadBigNormalisedUpdate is also deliberately excluded from the test
		boolean equal = this.m_Probability == that.m_Probability &&
				this.m_AnomalyScore == that.m_AnomalyScore &&
				this.m_NormalizedProbability == that.m_NormalizedProbability &&
				bothNullOrEqual(this.m_Typical, that.m_Typical) &&
				bothNullOrEqual(this.m_Actual, that.m_Actual) &&
				bothNullOrEqual(this.m_Function, that.m_Function) &&
				bothNullOrEqual(this.m_FieldName, that.m_FieldName) &&
				bothNullOrEqual(this.m_ByFieldName, that.m_ByFieldName) &&
				bothNullOrEqual(this.m_ByFieldValue, that.m_ByFieldValue) &&
				bothNullOrEqual(this.m_PartitionFieldName, that.m_PartitionFieldName) &&
				bothNullOrEqual(this.m_PartitionFieldValue, that.m_PartitionFieldValue) &&
				bothNullOrEqual(this.m_OverFieldName, that.m_OverFieldName) &&
				bothNullOrEqual(this.m_OverFieldValue, that.m_OverFieldValue) &&
				bothNullOrEqual(this.m_Timestamp, that.m_Timestamp) &&
				bothNullOrEqual(this.m_Parent, that.m_Parent);

		if (this.m_Causes == null && that.m_Causes == null)
		{
			equal &= true;
		}
		else if (this.m_Causes != null && that.m_Causes != null)
		{
			equal &= this.m_Causes.size() == that.m_Causes.size();
			if (equal)
			{
				for (int i=0; i<this.m_Causes.size(); i++)
				{
					equal &= this.m_Causes.get(i).equals(that.m_Causes.get(i));
				}
			}
		}
		else
		{
			// one null the other not
			equal = false;
		}

		return equal;
	}


	public boolean hadBigNormalisedUpdate()
	{
		return m_HadBigNormalisedUpdate;
	}


	public void resetBigNormalisedUpdateFlag()
	{
		m_HadBigNormalisedUpdate = false;
	}


	/**
	 * Encapsulate the logic for deciding whether a change to a normalised score
	 * is "big".
	 *
	 * Current logic is that a big change is a change of at least 1 or more than
	 * than 50% of the higher of the two values.
	 *
	 * @param oldVal The old value of the normalised score
	 * @param newVal The new value of the normalised score
	 * @return true if the update is considered "big"
	 */
	static boolean isBigUpdate(double oldVal, double newVal)
	{
		if (Math.abs(oldVal - newVal) >= 1.0)
		{
			return true;
		}

		if (oldVal > newVal)
		{
			if (oldVal * 0.5 > newVal)
			{
				return true;
			}
		}
		else
		{
			if (newVal * 0.5 > oldVal)
			{
				return true;
			}
		}

		return false;
	}
}
