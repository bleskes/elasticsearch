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

package com.prelert.rs.data.parsing;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.DetectorState;
import com.prelert.job.persistence.JobResultsPersister;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.quantiles.QuantilesState;
import com.prelert.rs.data.*;

/**
 * Parses the JSON output of the autodetect program. 
 * 
 * Expects an array of buckets so the first element will always be the
 * start array symbol and the data must be terminated with the end array symbol. 
 */
public class AutoDetectResultsParser 
{
	/**
	 * Parse the bucket results from inputstream and perist
	 * via the JobDataPersister.
	 *
	 * Trigger renormalisation of past results when new quantiles
	 * are seen.
	 *
	 * @param inputStream
	 * @param persister
	 * @param renormaliser
	 * @param logger 
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public void parseResults(InputStream inputStream,
			JobResultsPersister persister, JobRenormaliser renormaliser,
			Logger logger)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		JsonParser parser = new JsonFactory().createParser(inputStream);
		parser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

		JsonToken token = parser.nextToken();
		// if start of an array ignore it, we expect an array of buckets
		if (token == JsonToken.START_ARRAY)
		{
			token = parser.nextToken();
			logger.debug("JSON starts with an array");
		}

		if (token == JsonToken.END_ARRAY)
		{
			logger.info("Empty results array, 0 buckets parsed");

			// Parse the serialised detector state and persist
			DetectorState state = parseState(parser, logger);
			persister.persistDetectorState(state);		
			return;
		}
		else if (token != JsonToken.START_OBJECT)
		{
			logger.error("Expecting Json Start Object token after the Start Array token");
			throw new AutoDetectParseException(
					"Invalid JSON should start with an array of objects or an object = " + token);
		}
		
		// Parse the buckets from the stream
		int bucketCount = 0;
		while (token != JsonToken.END_ARRAY)
		{			
			if (token == null) // end of input
			{				
				logger.error("Unexpected end of Json input");
				break;
			}
			if (token == JsonToken.START_OBJECT)
			{
				token = parser.nextToken();
				if (token == JsonToken.FIELD_NAME)
				{
					String fieldName = parser.getCurrentName();
					switch (fieldName)
					{
					case Bucket.TIMESTAMP:
						Bucket bucket = Bucket.parseJsonAfterStartObject(parser);
						persister.persistBucket(bucket);
						persister.incrementBucketCount(1);
			
						logger.debug("Bucket number " + ++bucketCount + " parsed from output");
						break;
					case Quantiles.QUANTILE_KIND:
					case Quantiles.QUANTILE_STATE:
						Quantiles quantiles = Quantiles.parseJsonAfterStartObject(parser);
						persister.persistQuantiles(quantiles);

						logger.debug("Quantiles parsed from output - will " +
									"trigger renormalisation of " +
									quantiles.getKind() + " scores");
						triggerRenormalisation(quantiles, renormaliser, logger);
						break;
					default:
						logger.error("Unexpected object parsed from output - first field " + fieldName);
						throw new AutoDetectParseException(
								"Invalid JSON  - unexpected object parsed from output - first field " + fieldName);
					}
				}
			}
			else
			{
				logger.error("Expecting Json Field name token after the Start Object token");
				throw new AutoDetectParseException(
						"Invalid JSON  - object should start with a field name, not " + parser.getText());
			}

			token = parser.nextToken();
		}

		logger.info(bucketCount + " buckets parsed from autodetect output");

		
		// All the results have been read now read the serialised state
		logger.debug("Persisting detector state");
		DetectorState state = parseState(parser, logger);
		persister.persistDetectorState(state);		
		
		// commit data to the datastore
		logger.info("Detector state persisted - about to refresh indexes");
		persister.commitWrites();
	}
	
	
	static private DetectorState parseState(JsonParser parser, Logger logger) 
	throws JsonParseException, IOException, AutoDetectParseException
	{
		logger.debug("Parsing serialised detector state");
		
		JsonToken token = parser.nextToken();
		if (token == null)
		{
			logger.info("End of input no detector state to parse");
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


	static private void triggerRenormalisation(Quantiles quantiles,
			JobRenormaliser renormaliser, Logger logger)
	{
		if (QuantilesState.SYS_CHANGE_QUANTILES_KIND.equals(quantiles.getKind()))
		{
			renormaliser.updateBucketSysChange(quantiles.getState(), quantiles.getTimestamp(), logger);
		}
		else if (QuantilesState.UNUSUAL_QUANTILES_KIND.equals(quantiles.getKind()))
		{
			renormaliser.updateBucketUnusualBehaviour(quantiles.getState(), quantiles.getTimestamp(), logger);
		}
		else
		{
			logger.error("Unexpected kind of quantiles: " + quantiles.getKind());
		}
	}
}

