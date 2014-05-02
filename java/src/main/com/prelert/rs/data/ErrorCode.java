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

package com.prelert.rs.data;

/**
 * Static error codes returned in response to internal errors in the API.
 * The codes are grouped in the following way:
 * <ul>
 * <li>10XXX Codes are related to job creation</li>
 * <li>20XXX Codes are related to data store errors</li>
 * <li>30XXX Codes are related to data upload errors</li>
 * <li>40XXX Codes are related to running the native process errors</li>
 * <li>50XXX Codes are related to reading log files</li> 
 * <li>60XXX Codes are related to errors from the REST API</li> 
 * </ul>
 */
public enum ErrorCode 
{
	// job create errors
	/**
	 * The JSON configuration supplied to create a job 
	 * could not be parsed. The JSON is invalid. 
	 */
	JOB_CONFIG_PARSE_ERROR(10101),
	
	/**
	 * The job configuration JSON contains a field that isn't
	 * recognised
	 */
	JOB_CONFIG_UNKNOWN_FIELD_ERROR(10102), 
	
	/**
	 * When creating a new job from an existing job this error
	 * is returned if the reference job id is not known
	 */
	UNKNOWN_JOB_REFERENCE(10103),
	
	/**
	 * The value provided in one of the job configuration fields
	 * is not allowed for example some fields cannot be a number < 0.
	 */
	INVALID_VALUE(10104),
	
	/**
	 * The function argument is not recognised as one of
	 * the valid list of functions.
	 * @see com.prelert.job.Detector#ANALYSIS_FUNCTIONS
	 */
	UNKNOWN_FUNCTION(10105),
		
	/**
	 * In the {@link com.prelert.job.Detector} some combinations
	 * of fieldName/byFieldName/overFieldName and function are invalid.
	 */
	INVALID_FIELD_SELECTION(10106),
	
	/**
	 * The job configuration is not fully defined.
	 */
	INCOMPLETE_CONFIGURATION(10107),
	
	/**
	 * The date format pattern cannot be interpreted as a valid 
	 * Java date format pattern.
	 * @see java.text.SimpleDateFormat
	 */
	INVALID_DATE_FORMAT(10108),
	
	
	// Data store errors
	/**
	 * A generic exception from the data store
	 */
	DATA_STORE_ERROR(20001),
	
	/**
	 * The job cannot be found in the data store
	 */
	MISSING_JOB_ERROR(20101),
	
	/**
	 * The persisted detector state cannot be recovered
	 */
	MISSING_DETECTOR_STATE(20102),	
	
	
	
	// data upload errors
	/**
	 * Generic data error
	 */
	DATA_ERROR(30001),
	
	/**
	 * In the case of CSV data if a job is configured to use
	 * a certain field and that field isn't present in the CSV
	 * header this error is returned.
	 */
	MISSING_FIELD(30101),
	
	/**
	 * Data was defined to be gzip encoded ('Content-Encoding:gzip') 
	 * but isn't actually.
	 */
	UNCOMPRESSED_DATA(30102),
	
	/**
	 * As a proportion of all the records sent too many
	 * are either missing a date or the date cannot be parsed. 
	 */
	TOO_MANY_BAD_DATES(30103),
	
	/**
	 * As a proportion of all the records sent a high number are
	 * missing required fields or cannot be parsed. 
	 */
	TOO_MANY_BAD_RECORDS(30104),	
	
	
	// native process errors
	/**
	 * An unknown error has occurred in the Native process
	 */
	NATIVE_PROCESS_ERROR(40001),

	/**
	 * An error occurred starting the native process
	 */
	NATIVE_PROCESS_START_ERROR(40101),
	
	/**
	 * An error occurred writing data to the native process
	 */
	NATIVE_PROCESS_WRITE_ERROR(40102),
	
	/**
	 * Certain operations e.g. delete cannot be applied 
	 * to running jobs.
	 */
	NATIVE_PROCESS_RUNNING_ERROR(40103),
	
	
	// Log file reading errors
	/**
	 * The log directory does not exist
	 */
	CANNOT_OPEN_DIRECTORY(50101),
	
	/**
	 * The log file cannot be read
	 */
	MISSING_LOG_FILE(50102),
	
	// Rest API errors
	/**
	 * The date query parameter is un-parsable as a date
	 */
	UNPARSEABLE_DATE_ARGUMENT(60101); 
	
	
	
	private long m_ErrorCode;
	private String m_ValueString;
	
	private ErrorCode(long code)
	{
		m_ErrorCode = code;
		m_ValueString = Long.toString(code);
	}
	
	public long getValue()
	{
		return m_ErrorCode;
	}
	
	public String getValueString()
	{
		return m_ValueString;
	}
}
