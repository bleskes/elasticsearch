package com.prelert.job.warnings;

import com.prelert.rs.data.ErrorCode;

/**
 * If the timestamp field of a record cannot be read or the 
 * date format is incorrect the record is ignored. This 
 * exception is thrown when a high proportion of records
 * have a bad timestamp.
 */
public class HighProportionOfBadTimestampsException extends Exception 
{
	private static final long serialVersionUID = -7776085998658495251L;

	private long m_NumberBad;
	private long m_TotalNumber;
	
	public HighProportionOfBadTimestampsException(long numberBadRecords, 
			long totalNumberRecords, ErrorCode errorCode)
	{
		m_NumberBad = numberBadRecords;
		m_TotalNumber = totalNumberRecords;
	}
	
	public ErrorCode getErrorCode()
	{
		return ErrorCode.TOO_MANY_BAD_DATES;
	}
	
	/**
	 * The number of bad records
	 * @return
	 */
	public long getNumberBad()
	{
		return m_NumberBad;
	}
	
	/**
	 * Total number of records (good + bad)
	 * @return
	 */
	public long getTotalNumber()
	{
		return m_TotalNumber;
	}
	
	@Override
	public String getMessage()
	{
		return String.format("A high proportion of records are have a timestamp "
				+ "that cannot be interpreted (%d of %d).",
				m_NumberBad, m_TotalNumber);
	}
	
}
