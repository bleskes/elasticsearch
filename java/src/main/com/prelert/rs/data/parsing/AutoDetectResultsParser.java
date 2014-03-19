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

package com.prelert.rs.data.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.DetectorState;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.rs.data.*;

/**
 * Parses the JSON output of the autodetect program. 
 * 
 * Expects an array of buckets so the first element will always be the
 * start array symbol and the data must be terminated with the end array symbol. 
 */
public class AutoDetectResultsParser 
{
	static public final Logger s_Logger = Logger.getLogger(AutoDetectResultsParser.class);
	
	/**
	 * Utility class for grouping the parsed buckets and detector state.
	 * If the state isn't saved then it will be <code>null</code>
	 */
	static public class BucketsAndState
	{
		List<Bucket> m_Buckets = Collections.emptyList();
		DetectorState m_State;
		
		/**
		 * Return (possibly empty) list of buckets.
		 * @return
		 */
		public List<Bucket> getBuckets()
		{
			return m_Buckets;
		}
		
		/**
		 * Get the (possibly <code>null</code>) detector state.
		 * @return The detector state or <code>null</code>
		 */
		public DetectorState getDetectorState()
		{
			return m_State;
		}
	}
	
	/**
	 * Parse the JSON from the input stream returning a list of simple POJO 
	 * {@linkplain com.prelert.rs.data.Bucket}s objects.
	 * 
	 * @param inputStream Source of the JSON input
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public BucketsAndState parseResults(InputStream inputStream) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		return AutoDetectResultsParser.parseResults(inputStream, 
				new JobDataPersister() {					
					// empty methods
					@Override
					public void persistBucket(Bucket bucket) 
					{			
					}
					@Override
					public void persistDetectorState(DetectorState state)
					{						
					}
					@Override
					public boolean isDetectorStatePersisted() 
					{
						return false;
					}
				});
	}
	
	
	static public BucketsAndState parseResults(InputStream inputStream,
			JobDataPersister persister) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		JsonParser parser = new JsonFactory().createParser(inputStream);
		parser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

		JsonToken token = parser.nextToken();
		// if start of an array ignore it, we expect an array of buckets
		if (token == JsonToken.START_ARRAY)
		{
			token = parser.nextToken();
			s_Logger.debug("JSON starts with an array");
		}

		if (token == JsonToken.END_ARRAY)
		{
			s_Logger.info("Empty results array, 0 buckets parsed");
			
			// Parse the serialised detector state and persist
			DetectorState state = parseState(parser);
			persister.persistDetectorState(state);		
			BucketsAndState parsedData = new BucketsAndState();
			parsedData.m_State = state;
			
			return parsedData;
		}
		else if (token != JsonToken.START_OBJECT)
		{
			s_Logger.error("Expecting Json Start Object token after the Start Array token");
			throw new AutoDetectParseException(
					"Invalid JSON should start with an array of objects or an object = " + token);
		}
		
		// Parse the buckets from the stream
		List<Bucket> buckets = new ArrayList<>();
		while (token != JsonToken.END_ARRAY)
		{			
			if (token == null) // end of input
			{				
				s_Logger.error("Unexpected end of Json input");
				break;
			}
			Bucket bucket = Bucket.parseJson(parser);
			persister.persistBucket(bucket);
			
			buckets.add(bucket);
			s_Logger.debug("Bucket number " + buckets.size() + " parsed from output");

			token = parser.nextToken();
		}

		BucketsAndState parsedData = new BucketsAndState();
		parsedData.m_Buckets = buckets;
		
		s_Logger.info(buckets.size() + " buckets parsed from autodetect output");

		
		// All the results have been read now read the serialised state
		DetectorState state = parseState(parser);
		persister.persistDetectorState(state);		
		parsedData.m_State = state;
		
		return parsedData;
	}
	
	
	static private DetectorState parseState(JsonParser parser) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		s_Logger.debug("Parsing serialised detector state");
		
		JsonToken token = parser.nextToken();
		if (token == null)
		{
			s_Logger.info("End of input no detector state to parse");
			return null;
		}
		else if (token != JsonToken.START_OBJECT)
		{
			throw new AutoDetectParseException(
					"Invalid JSON should start with an array of objects or an object = " + token);
		}
		
		// Parse the serialised detector state and persist
		DetectorState state = DetectorState.parseJson(parser);
		return state;
	}
}		

