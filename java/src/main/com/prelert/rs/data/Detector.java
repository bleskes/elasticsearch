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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

/**
 * Represents the anomaly detector.
 * Only the detector name is serialised anomaly records aren't.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties({"records"})
public class Detector 
{
	static final public String TYPE = "detector";
	static final public String NAME = "name";
	static final public String RECORDS = "records";
	
	static final private Logger s_Logger = Logger.getLogger(Detector.class);
	
	private String m_Name;
	private List<AnomalyRecord> m_Records;
	
	private boolean m_IsSimpleCount;
	private long m_EventCount;
	
	
	public Detector()
	{
		m_Records = new ArrayList<>();
	}
	
	public Detector(String name)
	{
		this();
		m_Name = name;		
	}
	
	/**
	 * Create the detector from a map. Only the name field is read
	 * @param values
	 */
	public Detector(Map<String, Object> values)
	{
		if (values.containsKey(NAME))
		{
			m_Name = values.get(NAME).toString();
		}
		else
		{
			s_Logger.error("Constructing detector from map with no " + NAME + " field");
		}
	}
	
	public String getName()
	{
		return m_Name;
	}
	
	private void setName(String name)
	{
		m_Name = name;
	}
	
	public void addRecord(AnomalyRecord record)
	{
		m_Records.add(record);
	}
	
	public List<AnomalyRecord> getRecords()
	{
		return m_Records;
	}
	
	/**
	 * True if this is the simple count detector
	 * @return
	 */
	public boolean isSimpleCount()
	{
		return m_IsSimpleCount;
	}
	
	/**
	 * If {@link Detector#isSimpleCount()} is true
	 * this is the event count for the bucket
	 * @return
	 */
	public long getEventCount()
	{
		return m_EventCount;
	}
	
	
	/**
	 * Create a new <code>Detector</code> and populate it from the JSON parser.
	 * The parser must be pointing at the start of the object then all the object's 
	 * fields are read and if they match the property names the appropriate 
	 * members are set.
	 * 
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 * 
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new Detector
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public Detector parseJson(JsonParser parser) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		Detector detector = new Detector();
		
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse detector. First token '" +
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
				s_Logger.error("Start object parsed in detector");				
				break;
			case END_OBJECT:
				s_Logger.error("End object parsed in detector");				
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				switch (fieldName)
				{
				case NAME:
					token = parser.nextToken(); 
					if (token == JsonToken.VALUE_STRING)
					{
						detector.setName(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + NAME + " : " + 
								parser.getText() + " as a string");
					}
					break;					
				case RECORDS:
					token = parser.nextToken(); 
					if (token == JsonToken.START_ARRAY)
					{
						token = parser.nextToken(); 
						while (token != JsonToken.END_ARRAY)
						{
							AnomalyRecord record = AnomalyRecord.parseJson(parser);
							detector.addRecord(record);		
							
							if (record.isSimpleCount() != null && record.isSimpleCount() == true)
							{
								detector.m_IsSimpleCount = true;
								detector.m_EventCount = record.getActual().longValue();
							}
							
							token = parser.nextToken();
						}						
					}
					else
					{
						s_Logger.warn("Expected the start of an array for field '" 
									+ fieldName + "' not " + parser.getText());
					}
					break;	
				default:
					s_Logger.warn(String.format("Parse error unknown field in detector %s:%s", 
							fieldName, parser.nextTextValue()));
					break;				
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in bucket not " + token);
				break;			
			}
			
			token = parser.nextToken();
		}
		
		return detector;
	}
}
