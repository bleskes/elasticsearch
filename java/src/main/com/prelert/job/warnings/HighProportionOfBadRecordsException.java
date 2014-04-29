package com.prelert.job.warnings;

import com.prelert.rs.data.ErrorCodes;

public class HighProportionOfBadRecordsException extends Exception 
{
	private static final long serialVersionUID = -7776085998658495251L;

	private ErrorCodes m_ErrorCode;
	private long m_NumberBad;
	private long m_TotalNumber;
	
	public HighProportionOfBadRecordsException(long numberBadRecords, 
			long totalNumberRecords, ErrorCodes errorCode)
	{
		m_NumberBad = numberBadRecords;
		m_TotalNumber = totalNumberRecords;
		m_ErrorCode = errorCode;
	}
	
	public ErrorCodes getErrorCode()
	{
		return m_ErrorCode;
	}
	
	@Override
	public String getMessage()
	{
		return String.format("A high proportion of records are have a timestamp "
				+ "that cannot be interpreted (%d of %d).",
				m_NumberBad, m_TotalNumber);
	}
	
}
