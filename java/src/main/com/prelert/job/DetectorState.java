/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
package com.prelert.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

/**
 * This stores the serialised models from autodetect. The serialised form 
 * is a long Xml string, each detector has its own models so the data is 
 * stored in a map of detector name -> model xml.  
 *  
 * The Strings containing the serialised models can get very large but it is 
 * unlikely they will exceed the Java max string size (2GB).
 */
public class DetectorState 
{
	private static final Logger s_Logger = Logger.getLogger(DetectorState.class);
	
	/**
	 * The type of this class used when persisting the data
	 */
	public static final String TYPE = "detectorState";
	/**
	 * Detector Name used when persisting this class data
	 */
	public static final String DETECTOR_NAME = "detectorName";
	/**
	 * Serialised model constant used when persisting this class data
	 */
	public static final String SERIALISED_MODEL = "serialisedModel";
	
	public static final String DOCUMENT_TYPE = "type";
	public static final String MODEL_STATE = "model_state";
	
	private static final String DETECTOR_BASE_KEY = "detector";
	
	private String m_DocumentType;
	
	private Map<String, String> m_DetectorKeyToState;
	
	public DetectorState()
	{
		m_DetectorKeyToState = new HashMap<>();
	}
	
	/**
	 * The document type in most cases this will be {@value #MODEL_STATE}
	 * @return
	 */
	public String getDocumentType()
	{
		return m_DocumentType;
	}
	
	
	public void setDocumentType(String type)
	{
		m_DocumentType = type;
	}
	
	/**
	 * Expose the map of detector names -> state
	 * @return
	 */
	public Map<String, String> getMap()
	{
		return m_DetectorKeyToState;
	}
	
	/**
	 * Get the set of all detector keys
	 * @return
	 */
	public Set<String> getDetectorKeys()
	{
		return m_DetectorKeyToState.keySet();
	}
	
	/**
	 * Return the serialised model state of the detector or 
	 * <code>null</code> if the <code>detectorKey</code> is not 
	 * recognised. The model is saved as a potentially very 
	 * long Xml string.
	 * 
	 * @param detectorKey
	 * @return <code>null</code> or the serialised state
	 */
	public String getDetectorState(String detectorKey)
	{
		return m_DetectorKeyToState.get(detectorKey);
	}
	
	/**
	 * Set the state of the detector where state is the serialised model.
	 * 
	 * @param detectorKey
	 * @param state
	 */
	public void setDetectorState(String detectorKey, String state)
	{
		m_DetectorKeyToState.put(detectorKey, state);
	}
	
	/**
	 * Parse the detector state from Json.<br/>
	 * The Json can contain the serialised models for multiple detectors
	 * each is stored in a field starting with 'detector' with a numerical
	 * suffix e.g. detector1, detector2, etc.
	 * 
	 * @param parser
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public DetectorState parseJson(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		DetectorState state = new DetectorState();
		
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse detector state." + 
					"The first token '" + parser.getText() + ", is not the start token";
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
				s_Logger.error("Start object parsed in bucket");	
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				if (DOCUMENT_TYPE.equals(fieldName))
				{
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)	
					{
						state.setDocumentType(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText() 
								+ " as a string");
					}					
				}
				else if (fieldName.startsWith(DETECTOR_BASE_KEY))
				{
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)	
					{
						state.setDetectorState(fieldName, parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText() 
								+ " as a string");
					}
				}
				else 
				{
					s_Logger.warn(String.format("Parse error unknown field in detectorstate %s:%s", 
							fieldName, parser.getText()));
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in "
						+ "Detector State not " + token);
				break;
			}
			token = parser.nextToken();
			
		}
		
		return state;
	}
}
