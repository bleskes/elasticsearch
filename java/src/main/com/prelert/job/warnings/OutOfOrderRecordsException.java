package com.prelert.job.warnings;

import com.prelert.rs.data.ErrorCode;

/**
 *  Records sent to autodetect should be in ascending chronological 
 *  order else they are ignored and a error logged. This exception
 *  represents the case where a high proportion of messages are not
 *  in temporal order. 
 */
public class OutOfOrderRecordsException extends Exception 
{
	private static final long serialVersionUID = -7088347813900268191L;
	
	private ErrorCode m_ErrorCode;
	private long m_NumberBad;
	private long m_TotalNumber;
	
	public OutOfOrderRecordsException(long numberBadRecords, 
			long totalNumberRecords, ErrorCode errorCode)
	{
		m_NumberBad = numberBadRecords;
		m_TotalNumber = totalNumberRecords;
		m_ErrorCode = errorCode;
	}
	
	public ErrorCode getErrorCode()
	{
		return m_ErrorCode;
	}
	
	@Override
	public String getMessage()
	{
		return String.format("A high proportion of records are not in ascending "
				+ "chronological  order (%d of %d).",
				m_NumberBad, m_TotalNumber);
	}
}
