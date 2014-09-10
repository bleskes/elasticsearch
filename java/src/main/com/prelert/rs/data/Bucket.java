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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

import org.apache.log4j.Logger;

/**
 * Bucket Result POJO 
 */
@JsonIgnoreProperties({"epoch", "detectors"})
@JsonInclude(Include.NON_NULL)
public class Bucket 
{
	/*
	 * Field Names
	 */
	public static final String ID = "id";
	public static final String TIMESTAMP = "timestamp";
	public static final String RAW_ANOMALY_SCORE =  "rawAnomalyScore";
	public static final String ANOMALY_SCORE =  "anomalyScore";
	public static final String MAX_NORMALIZED_PROBABILITY =  "maxNormalizedProbability";
	public static final String RECORD_COUNT = "recordCount";
	public static final String EVENT_COUNT = "eventCount";
	public static final String DETECTORS = "detectors";
	public static final String RECORDS = "records";
	

	/**
	 * Elasticsearch type
	 */
	public static final String TYPE = "bucket";

	private static final Logger s_Logger = Logger.getLogger(Bucket.class);
	
	private String m_Id;
	private Date m_Timestamp;
	private double m_RawAnomalyScore;	
	private double m_AnomalyScore;	
	private double m_MaxNormalizedProbability;	
	private int m_RecordCount;
	private List<Detector> m_Detectors;
	private long m_Epoch;
	private List<AnomalyRecord> m_Records;
	private long m_EventCount;
	private boolean m_HadBigNormalisedUpdate;
	
	public Bucket()
	{
		m_Detectors = new ArrayList<>();
		m_Records = Collections.emptyList();
	}
	
	/**
	 * The bucket Id is the bucket's timestamp in seconds 
	 * from the epoch. As the id is derived from the timestamp 
	 * field it doesn't need to be serialised.
	 *  
	 * @return The bucket id
	 */
	public String getId()
	{
		return m_Id;
	}
	
	public void setId(String id)
	{
		m_Id = id;
	}
	
	public long getEpoch()
	{
		return m_Epoch;
	}
	
	public Date getTimestamp() 
	{
		return m_Timestamp;
	}
	
	public void setTimestamp(Date timestamp) 
	{
		m_Timestamp = timestamp;
		
		// epoch in seconds
		m_Epoch = m_Timestamp.getTime() / 1000;
		
		m_Id = Long.toString(m_Epoch); 
	}


	public double getRawAnomalyScore() 
	{
		return m_RawAnomalyScore;
	}	

	public void setRawAnomalyScore(double rawAnomalyScore) 
	{
		m_RawAnomalyScore = rawAnomalyScore;
	}


	public double getAnomalyScore() 
	{
		return m_AnomalyScore;
	}	

	public void setAnomalyScore(double anomalyScore) 
	{
		m_HadBigNormalisedUpdate |= AnomalyRecord.isBigUpdate(m_AnomalyScore, anomalyScore);
		m_AnomalyScore = anomalyScore;
	}

	public double getMaxNormalizedProbability() 
	{
		return m_MaxNormalizedProbability;
	}	

	public void setMaxNormalizedProbability(double maxNormalizedProbability) 
	{
		m_HadBigNormalisedUpdate |= AnomalyRecord.isBigUpdate(m_MaxNormalizedProbability, maxNormalizedProbability);
		m_MaxNormalizedProbability = maxNormalizedProbability;
	}


	public int getRecordCount() 
	{
		return m_RecordCount;
	}
			
	public void setRecordCount(int recordCount) 
	{
		m_RecordCount = recordCount;
	}


	/**
	 * Get the list of detectors that produced output in this bucket
	 * 
	 * @return A list of detector 
	 */	
	public List<Detector> getDetectors()
	{
		return m_Detectors;
	}
	
	public void setDetectors(List<Detector> detectors)
	{
		m_Detectors = detectors;
	}


	/**
	 * Add a detector that produced output in this bucket
	 * 
	 */	
	private void addDetector(Detector detector)
	{
		m_Detectors.add(detector);
	}


	/**
	 * Get all the anomaly records associated with this bucket
	 * @return All the anomaly records
	 */
	public List<AnomalyRecord> getRecords()
	{
		return m_Records;
	}
		
	public void setRecords(List<AnomalyRecord> records)
	{
		m_Records = records;
	}
	
	/**
	 * The number of records (events) actually processed 
	 * in this bucket.
	 * @return
	 */
	public long getEventCount()
	{
		return m_EventCount;
	}
	
	public void setEventCount(long value)
	{
		m_EventCount = value;
	}
	
	/**
	 * Create a new <code>Bucket</code> and populate it from the JSON parser.
	 * The parser must be pointing at the start of the object then all the object's 
	 * fields are read and if they match the property names the appropriate 
	 * members are set.
	 * 
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 * 
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new bucket
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public Bucket parseJson(JsonParser parser) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse Bucket. The first token '" +
					parser.getText() + ", is not the start token";
			s_Logger.error(msg);
			
			throw new AutoDetectParseException(msg);
		}

		token = parser.nextToken();
		Bucket bucket = parseJsonAfterStartObject(parser);

		return bucket;
	}


	/**
	 * Create a new <code>Bucket</code> and populate it from the JSON parser.
	 * The parser must be pointing at the first token inside the object.  It
	 * is assumed that prior code has validated that the previous token was
	 * the start of an object.  Then all the object's fields are read and if
	 * they match the property names the appropriate members are set.
	 * 
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 * 
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new bucket
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public Bucket parseJsonAfterStartObject(JsonParser parser) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		Bucket bucket = new Bucket();

		JsonToken token = parser.getCurrentToken();
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
				case TIMESTAMP:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)	
					{
						// convert seconds to ms
						long val = parser.getLongValue() * 1000;
						bucket.setTimestamp(new Date(val));
					}
					else
					{
						s_Logger.warn("Cannot parse " + TIMESTAMP + " : " + parser.getText() 
										+ " as a long");
					}
					break;
				case RAW_ANOMALY_SCORE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
					{
						bucket.setRawAnomalyScore(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + RAW_ANOMALY_SCORE + " : " + parser.getText() 
										+ " as a double");
					}
					break;	
				case ANOMALY_SCORE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
					{
						bucket.setAnomalyScore(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + ANOMALY_SCORE + " : " + parser.getText() 
										+ " as a double");
					}
					break;	
				case MAX_NORMALIZED_PROBABILITY:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
					{
						bucket.setMaxNormalizedProbability(parser.getDoubleValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + MAX_NORMALIZED_PROBABILITY + " : " + parser.getText() 
										+ " as a double");
					}
					break;	
				case RECORD_COUNT:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						bucket.setRecordCount(parser.getIntValue());
					}
					else 
					{
						s_Logger.warn("Cannot parse " + RECORD_COUNT + " : " + parser.getText() 
								+ " as an int");
					}
					break;						
				case EVENT_COUNT:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						bucket.setEventCount(parser.getLongValue());
					}
					else 
					{
						s_Logger.warn("Cannot parse " + EVENT_COUNT + " : " + parser.getText() 
								+ " as an int");
					}
					break;						
				case DETECTORS:
					token = parser.nextToken();
					if (token != JsonToken.START_ARRAY)
					{
						String msg = "Invalid value Expecting an array of detectors";
						s_Logger.warn(msg);
						throw new AutoDetectParseException(msg);
					}
					
					token = parser.nextToken();
					while (token != JsonToken.END_ARRAY)
					{
						Detector detector = Detector.parseJson(parser);
						bucket.addDetector(detector);
						
						token = parser.nextToken();
					}
					break;
				default:
					s_Logger.warn(String.format("Parse error unknown field in Bucket %s:%s", 
							fieldName, parser.nextTextValue()));
					break;
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in bucket not "
						+ token);
				break;			
			}
			
			token = parser.nextToken();
		}
		
		// Set the record count to what was actually read
		bucket.m_RecordCount = 0;
		for (Detector d : bucket.getDetectors())
		{
			bucket.m_RecordCount += d.getRecords().size();
		}
		
		return bucket;
	}

	@Override
	public int hashCode() 
	{
		// m_HadBigNormalisedUpdate is deliberately excluded from the hash
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(m_RawAnomalyScore);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(m_AnomalyScore);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(m_MaxNormalizedProbability);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((m_Detectors == null) ? 0 : m_Detectors.hashCode());
		result = prime * result + (int) (m_Epoch ^ (m_Epoch >>> 32));
		result = prime * result + ((m_Id == null) ? 0 : m_Id.hashCode());
		result = prime * result + m_RecordCount;
		result = prime * result
				+ ((m_Records == null) ? 0 : m_Records.hashCode());
		result = prime * result
				+ ((m_Timestamp == null) ? 0 : m_Timestamp.hashCode());
		return result;
	}

	/**
	 * Compare all the fields and embedded anomaly records
	 * (if any), does not compare detectors as they are not
	 * serialized anyway.
	 */
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof Bucket == false)
		{
			return false;
		}
		
		Bucket that = (Bucket)other;

		// m_HadBigNormalisedUpdate is deliberately excluded from the test
		boolean equals = (this.m_Id.equals(that.m_Id)) &&
				(this.m_Timestamp.equals(that.m_Timestamp)) &&
				(this.m_EventCount == that.m_EventCount) &&
				(this.m_RawAnomalyScore == that.m_RawAnomalyScore) &&
				(this.m_AnomalyScore == that.m_AnomalyScore) &&
				(this.m_MaxNormalizedProbability == that.m_MaxNormalizedProbability) &&
				(this.m_RecordCount == that.m_RecordCount) &&
				(this.m_Epoch == that.m_Epoch);
		
		// don't bother testing detectors
		if (this.m_Records == null && that.m_Records == null)
		{
			equals &= true;
		}
		else if (this.m_Records != null && that.m_Records != null)
		{
			equals &= this.m_Records.size() == that.m_Records.size();
			if (equals)
			{
				for (int i=0; i<this.m_Records.size(); i++)
				{
					equals &= this.m_Records.get(i).equals(that.m_Records.get(i));
				}
			}
		}
		else
		{
			// one null the other not
			equals = false;
		}
		
		return equals;
	}


	public boolean hadBigNormalisedUpdate()
	{
		return m_HadBigNormalisedUpdate;
	}


	public void resetBigNormalisedUpdateFlag()
	{
		m_HadBigNormalisedUpdate = false;
	}
}
