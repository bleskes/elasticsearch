package com.prelert.job.persistence.elasticsearch;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

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
	private final static Logger s_Logger = Logger.getLogger(ElasticSearchResultsReaderFactory.class);
	
	private Node m_Node;
	
	/**
	 * Construct the factory
	 * 
	 * @param node The ElasticSearch node
	 */
	public ElasticSearchResultsReaderFactory(Node node)
	{
		m_Node = node;
	}
	
	@Override
	public Runnable newResultsParser(String jobId, InputStream autoDetectOutput,
			Logger logger) 
	{
		return new ReadAutoDetectOutput(jobId, autoDetectOutput, m_Node.client(),
				logger);
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
				s_Logger.info("Error parsing autodetect_api output", e);
			}
			catch (IOException e) 
			{
				s_Logger.info("Error parsing autodetect_api output", e);
			}
			catch (AutoDetectParseException e) 
			{
				s_Logger.info("Error parsing autodetect_api output", e);
			}

			m_Logger.info("Parse results Complete");
		}		
	}
}
