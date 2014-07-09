package com.prelert.job.warnings.elasticsearch;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.prelert.job.JobDetails;
import com.prelert.job.warnings.StatusReporter;

import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateRequestBuilder;
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
		
	public ElasticSearchStatusReporter(Client client, String jobId, 
			JobDetails.Counts counts, Logger logger)
	{
		super(jobId, counts, logger);
		m_Client = client;
	}
		

	/**
	 * Log a message then write to elastic search.
	 */
	@Override
	protected void reportStatus(long totalRecords)
	{
		String status = String.format("%d records written to autodetect %d had "
				+ "missing fields, %d were discarded because the date could not be "
				+ "read and %d were ignored as because they weren't in ascending "
				+ "chronological order.", getRecordsWrittenCount(), 
				getMissingFieldErrorCount(), getDateParseErrorsCount(), 
				getOutOfOrderRecordCount()); 
		
		m_Logger.info(status);
		
		persistStats();
	}
	
	/**
	 * Write the status counts to the datastore
	 * @return
	 */
	private boolean persistStats()
	{
		try
		{
			UpdateRequestBuilder updateBuilder = m_Client.prepareUpdate(m_JobId, JobDetails.TYPE, m_JobId);
			updateBuilder.setRetryOnConflict(1);
			
			Map<String, Object> updates = new HashMap<>();
			updates.put(JobDetails.PROCESSED_RECORD_COUNT, getRecordsWrittenCount());
			updates.put(JobDetails.PROCESSED_VOLUME, getVolume());
			updates.put(JobDetails.INVALID_DATE_COUNT, getDateParseErrorsCount());
			updates.put(JobDetails.MISSING_FIELD_COUNT, getMissingFieldErrorCount());
			updates.put(JobDetails.OUT_OF_ORDER_TIME_COUNT, getOutOfOrderRecordCount());
			
			Map<String, Object> counts = new HashMap<>();
			counts.put(JobDetails.COUNTS, updates);
			
			updateBuilder.setDoc(counts);
			m_Client.update(updateBuilder.request()).get();
			
			return true;
		}
		catch (IndexMissingException | InterruptedException | ExecutionException e)
		{
			String msg = String.format("Error writing the job '%s' status stats.",
					m_JobId);
			
			m_Logger.warn(msg, e);
			
			return false;
		}
	}
}
