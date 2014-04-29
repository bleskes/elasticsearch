package com.prelert.job.warnings.elasticsearch;


import com.prelert.job.JobDetails;
import com.prelert.job.warnings.HighProportionOfBadRecordsException;
import com.prelert.job.warnings.OutOfOrderRecordsException;
import com.prelert.job.warnings.StatusReporter;
import com.prelert.rs.data.ErrorCodes;

import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;


/**
 * 
 * The total number of records processed is m_RecordsWritten + 
 * m_RecordsDiscarded
 */
public class ElasticSearchStatusReporter implements StatusReporter
{
	public static final int ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS = 25;
	public static final String ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP = 
			"max.percent.date.errors";
	
	private int m_RecordsWritten = 0;
	private int m_RecordsDiscarded = 0;
	private int m_DateParseErrorsCount = 0;
	private int m_MissingFieldErrorCount = 0;
	private int m_OutOfOrderRecordCount = 0;
	
	private boolean m_Seen100Records;
	private int m_1000RecordsModulus = 0;
	
	private int m_AcceptablePercentDateParseErrors;
	
	private Client m_Client;
	private String m_JobId;
	
	
	private Logger m_Logger;
	
	public ElasticSearchStatusReporter(Client client, String jobId, Logger logger)
	{
		m_Client = client;
		m_JobId = jobId;
		m_Logger = logger;
		
		m_AcceptablePercentDateParseErrors = ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS;
		
		String prop = System.getProperty(ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP);
		try
		{
			m_AcceptablePercentDateParseErrors = Integer.parseInt(prop);
		}
		catch (NumberFormatException e)
		{
			
		}
	}
		
	@Override
	public void reportRecordsWritten(int recordsWritten, int recordsDiscarded)
	throws HighProportionOfBadRecordsException 
	{
		m_RecordsWritten = recordsWritten;
		m_RecordsDiscarded = recordsDiscarded;
		
		// report at various boundaries
		int totalRecords = m_RecordsWritten + m_RecordsDiscarded;
		if (isReportingBoundary(totalRecords))
		{
			reportStatus(totalRecords);
		}
		
	}

	@Override
	public void reportDateParseError(String date)
	throws HighProportionOfBadRecordsException 
	{
		m_DateParseErrorsCount++;
	}

	@Override
	public void reportMissingField(String field) 
	{
		m_MissingFieldErrorCount++;
	}


	@Override
	public void reportOutOfOrderRecord(long date, long previousDate)
	throws OutOfOrderRecordsException 
	{
		m_OutOfOrderRecordCount++;
	}
	
	public int getRecordsWrittenCount() 
	{
		return m_RecordsWritten;
	}

	public int getRecordsDiscarded() 
	{
		return m_RecordsDiscarded;
	}

	public int getDateParseErrorsCount() 
	{
		return m_DateParseErrorsCount;
	}

	public int getMissingFieldErrorCount() 
	{
		return m_MissingFieldErrorCount;
	}

	public int getOutOfOrderRecordCount() 
	{
		return m_OutOfOrderRecordCount;
	}
	
	
	public int getAcceptablePercentDateParseErrors()
	{
		return m_AcceptablePercentDateParseErrors;
	}
	
	public void setAcceptablePercentDateParseErrors(int value)
	{
		m_AcceptablePercentDateParseErrors = value;
	}
	
	
	private boolean isReportingBoundary(int totalRecords)
	{
		if (totalRecords > 100 && !m_Seen100Records)
		{
			return true;
		}
		
		int thousandCount = totalRecords % 1000;
		if (thousandCount > m_1000RecordsModulus)
		{
			m_1000RecordsModulus = thousandCount;
			return true;
		}
		
		return false;
	}
	
	
	private void reportStatus(int totalRecords)
	throws HighProportionOfBadRecordsException
	{
		String status = String.format("%d records written to autodetect %d had "
				+ "missing fields, %d were discarded because the date could not be "
				+ "read and %d were ignored as because they weren't in ascending "
				+ "chronological order.", m_RecordsWritten, m_DateParseErrorsCount,
				m_MissingFieldErrorCount, m_OutOfOrderRecordCount); 
		
		m_Logger.info(status);
		
		persistStatus();
		
		
		int percentBadDate = (m_DateParseErrorsCount * 100) / totalRecords;
		if (percentBadDate > m_AcceptablePercentDateParseErrors)
		{
			throw new HighProportionOfBadRecordsException(m_DateParseErrorsCount,
					totalRecords, ErrorCodes.TOO_MANY_BAD_DATES);
		}
		
		
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
			job.setProcessedRecordCount(m_RecordsWritten);
			job.setInvalidDateCount(m_DateParseErrorsCount);
			job.setMissingFieldCount(m_MissingFieldErrorCount);
			job.setOutOfOrderTimeStampCount(m_OutOfOrderRecordCount);
			

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
