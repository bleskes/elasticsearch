package com.prelert.job.persistence.elasticsearch;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;

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
	private Client m_Client;
	
	/**
	 * Construct the factory
	 * 
	 * @param node The ElasticSearch node
	 */
	public ElasticSearchResultsReaderFactory(Client client)
	{
		m_Client = client;
	}
	
	@Override
	public Runnable newResultsParser(String jobId, InputStream autoDetectOutput,
			Logger logger) 
	{
		return new ReadAutoDetectOutput(jobId, autoDetectOutput, m_Client, logger);
	}

	
	/**
	 * This private class parses the autodetect output stream and writes it
	 * to ElasticSearch
	 */
	private class ReadAutoDetectOutput implements Runnable 
	{
		private String m_JobId;
		private Client m_Client;
		private InputStream m_Stream;	
		private Logger m_Logger;
		
		public ReadAutoDetectOutput(String jobId, InputStream stream, Client client, 
				Logger logger)
		{
			m_JobId = jobId;
			m_Stream = stream;
			m_Client = client;
			m_Logger = logger;
		}
		
		public void run() 
		{			
			ElasticSearchPersister persister = new ElasticSearchPersister(m_JobId, m_Client);
			
			try 
			{
				AutoDetectResultsParser.parseResults(m_Stream, persister, m_Logger);				
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
