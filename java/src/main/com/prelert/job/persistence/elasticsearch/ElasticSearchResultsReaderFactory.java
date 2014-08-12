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
package com.prelert.job.persistence.elasticsearch;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.process.ResultsReaderFactory;
import com.prelert.rs.data.parsing.AutoDetectParseException;
import com.prelert.rs.data.parsing.AutoDetectResultsParser;

/**
 * Factory class to produce Runnable objects that will parse the 
 * autodetect results output and write them to ElasticSearch.
 */
public class ElasticSearchResultsReaderFactory implements ResultsReaderFactory
{
	private ElasticSearchJobProvider m_JobProvider;
	
	/**
	 * Construct the factory
	 * 
	 * @param jobProvider The ElasticSearch job provider
	 */
	public ElasticSearchResultsReaderFactory(ElasticSearchJobProvider jobProvider)
	{
		m_JobProvider = jobProvider;
	}
	
	@Override
	public Runnable newResultsParser(String jobId, InputStream autoDetectOutput,
			Logger logger) 
	{
		return new ReadAutoDetectOutput(jobId, autoDetectOutput, m_JobProvider, logger);
	}

	
	/**
	 * This private class parses the autodetect output stream and writes it
	 * to ElasticSearch
	 */
	private class ReadAutoDetectOutput implements Runnable 
	{
		private String m_JobId;
		private ElasticSearchJobProvider m_JobProvider;
		private InputStream m_Stream;	
		private Logger m_Logger;
		
		public ReadAutoDetectOutput(String jobId, InputStream stream,
				ElasticSearchJobProvider jobProvider, Logger logger)
		{
			m_JobId = jobId;
			m_Stream = stream;
			m_JobProvider = jobProvider;
			m_Logger = logger;
		}
		
		public void run() 
		{			
			ElasticSearchPersister persister = new ElasticSearchPersister(m_JobId, m_JobProvider.getClient());
			ElasticSearchJobRenormaliser renormaliser = new ElasticSearchJobRenormaliser(m_JobId, m_JobProvider);

			try
			{
				AutoDetectResultsParser.parseResults(m_Stream,
						persister, renormaliser, m_Logger);
			}
			catch (JsonParseException e) 
			{
				m_Logger.info("Error parsing autodetect_api output", e);
			}
			catch (IOException e) 
			{
				m_Logger.info("Error parsing autodetect_api output", e);
			}
			catch (AutoDetectParseException e) 
			{
				m_Logger.info("Error parsing autodetect_api output", e);
			}
			finally
			{
				try 
				{
					// read anything left in the stream before
					// closing the stream otherwise it the proccess 
					// tries to write more after the close it gets 
					// a SIGPIPE
					byte [] buff = new byte [512];
					while (m_Stream.read(buff) >= 0)
					{
						;
					}
					m_Stream.close();
				} 
				catch (IOException e) 
				{
					m_Logger.warn("Error closing result parser input stream", e);
				}
			}

			m_Logger.info("Parse results Complete");
		}		
	}
}
