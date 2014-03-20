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
@JsonIgnoreProperties({"Id"})
@JsonInclude(Include.NON_NULL)
public class Bucket 
{
	/*
	 * Field Names
	 */
	public static final String ID = "Id";
	public static final String TIMESTAMP = "timestamp";
	public static final String ANOMALY_SCORE =  "anomalyScore";
	public static final String RECORD_COUNT = "recordCount";
	public static final String DETECTORS = "detectors";
	public static final String RECORDS = "records";
	
	
	static public final String TYPE = "bucket";
	
	private static final Logger s_Logger = Logger.getLogger(Bucket.class);
	
	private Date m_Timestamp;
	private double m_AnomalyScore;	
	private int m_RecordCount;
	private List<Detector> m_Detectors = new ArrayList<>();
	private String m_Id;
	private List<AnomalyRecord> m_Records = new ArrayList<>();
	
	/**
	 * The bucket Id is the bucket's timestamp in seconds 
	 * from the epoch. As the id is derived from the timestamp 
	 * field it doesn't need to be serialised.
	 *  
	 * @return
	 */
	public String getId()
	{
		return m_Id;
	}
	
	public Date getTimestamp() 
	{
		return m_Timestamp;
	}
	
	public void setTimestamp(Date timestamp) 
	{
		this.m_Timestamp = timestamp;
		
		Long epoch = m_Timestamp.getTime() / 1000;
		m_Id = epoch.toString();
	}
	
	public double getAnomalyScore() 
	{
		return m_AnomalyScore;
	}	

	public void setAnomalyScore(double anomalyScore) 
	{
		this.m_AnomalyScore = anomalyScore;
	}
	
	public int getRecordCount() 
	{
		return m_RecordCount;
	}
			
	public void setRecordCount(int recordCount) 
	{
		this.m_RecordCount = recordCount;
	}
	
	/**
	 * Get the list of detectors.
	 * 
	 * @return 
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
	 * Get all the anomaly records associated with this bucket
	 * @return
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
	 * @return 
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public Bucket parseJson(JsonParser parser) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		Bucket bucket = new Bucket();
		
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse Bucket. The first token '" +
					parser.getText() + ", is not the start token";
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
				case TIMESTAMP:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)	
					{
						// convert seconds to ms
						long val = parser.getLongValue()  * 1000;
						bucket.setTimestamp(new Date(val));
					}
					else
					{
						s_Logger.warn("Cannot parse " + TIMESTAMP + " : " + parser.getText() 
										+ " as a long");
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
						bucket.getDetectors().add(detector);
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
		
		return bucket;
	}
	

}
