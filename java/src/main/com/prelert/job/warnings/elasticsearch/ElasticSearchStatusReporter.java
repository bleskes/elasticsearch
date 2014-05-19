package com.prelert.job.warnings.elasticsearch;


import com.prelert.job.JobDetails;
import com.prelert.job.warnings.StatusReporter;

import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;


/**
 * The {@link #reportStatus(int)} function logs a status message 
 * and updates the jobs ProcessedRecordCount, InvalidDateCount,
 * MissingFieldCount and OutOfOrderTimeStampCount values in the
 * ElasticSearch document.
 */
public class ElasticSearchStatusReporter extends StatusReporter
{
	private Client m_Client;
	
	public ElasticSearchStatusReporter(Client client, String jobId, Logger logger)
	{
		super(jobId, logger);
		m_Client = client;
	}
		

	/**
	 * Log a message then write to elastic search.
	 */
	@Override
	protected void reportStatus(int totalRecords)
	{
		String status = String.format("%d records written to autodetect %d had "
				+ "missing fields, %d were discarded because the date could not be "
				+ "read and %d were ignored as because they weren't in ascending "
				+ "chronological order.", getRecordsWrittenCount(), 
				getDateParseErrorsCount(), getMissingFieldErrorCount(),
				getOutOfOrderRecordCount()); 
		
		m_Logger.info(status);
		
		persistStatus();
	}
	
	
	private boolean persistStatus()
	{
		try
		{
			GetResponse response = m_Client.prepareGet(m_JobId, JobDetails.TYPE, 
					m_JobId).get();

			if (response.isExists() == false)
			{				
				m_Logger.error("Cannot write status stats " + m_JobId);
				return false;
			}

			long lastVersion = response.getVersion();
			
			JobDetails job = new JobDetails(response.getSource());

			// update record stats
			job.setProcessedRecordCount(getRecordsWrittenCount());
			job.setInvalidDateCount(getDateParseErrorsCount());
			job.setMissingFieldCount(getMissingFieldErrorCount());
			job.setOutOfOrderTimeStampCount(getOutOfOrderRecordCount());
			

			IndexResponse jobIndexResponse = m_Client.prepareIndex(
					m_JobId, JobDetails.TYPE, m_JobId)
					.setSource(job).get();

			if (jobIndexResponse.getVersion() <= lastVersion)
			{
				String msg = String.format("Error writing job '%s' status "
						+ "stats. Document was not updated", m_JobId);
				m_Logger.error(msg);
				return false;
			}
			
			return true;
		}
		catch (IndexMissingException e)
		{
			String msg = String.format("Error writing the job '%s' status stats."
					+ "Missing index error.", m_JobId);
			m_Logger.error(msg);
			
			return false;
		}
		
	}
}
