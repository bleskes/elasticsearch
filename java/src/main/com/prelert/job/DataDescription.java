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

package com.prelert.job;

import java.text.SimpleDateFormat;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Describes the format of the data used in the job and how it should 
 * be interpreted by autodetect.
 * <p/>
 * Data must either be in a textual delineated format (e.g. csv, tsv) or JSON
 * the {@linkplain DataFormat} enum indicates which. {@link #getTimeField()} 
 * is the name of the field containing the timestamp and {@link #getTimeFormat()} 
 * is the format code for the date string in as described by 
 * {@link java.text.SimpleDateFormat}. The default quote character for 
 * delineated formats is {@value #DEFAULT_QUOTE_CHAR} but any other character can be
 * used.  
 */
public class DataDescription 
{
	/**
	 * Enum of the acceptable data formats. 
	 */
	public enum DataFormat 
	{
		JSON, DELINEATED;
	
		/**
		 * Case-insensitive from string method. 
		 * Works with either JSON, json, etc. 
		 * 
		 * @param value
		 * @return The data format
		 */
		@JsonCreator
		static public DataFormat forString(String value)
		{
			return DataFormat.valueOf(value.toUpperCase());
		}
	}
	
	/**
	 * The format field name
	 */
	static final public String FORMAT = "format"; 
	/**
	 * The time field name
	 */
	static final public String TIME_FIELD_NAME = "timeField";
	/**
	 * The timeFormat field name
	 */	
	static final public String TIME_FORMAT = "timeFormat";
	/**
	 * The field delimiter field name
	 */
	static final public String FIELD_DELIMITER = "fieldDelimiter";
	/**
	 * The quote char field name
	 */
	static final public String QUOTE_CHARACTER = "quoteCharacter";
	
	/**
	 * The default field delimiter expected by the native autodetect_api
	 * program.
	 */
	static final public char DEFAULT_DELIMITER = '\t';
	
	/**
	 * Csv data must have this line ending
	 */
	static final public char LINE_ENDING = '\n';
	
	/**
	 * The default quote character used to escape text in 
	 * delineated data formats 
	 */
	static final public char DEFAULT_QUOTE_CHAR = '"';
	
	private DataFormat m_DataFormat;
	private String m_TimeFieldName;
	private String m_TimeFormat;
	private String m_FieldDelimiter;
	private char m_QuoteCharacter;
	
	public DataDescription()
	{
		m_DataFormat = DataFormat.DELINEATED;
		m_QuoteCharacter = DEFAULT_QUOTE_CHAR;
	}
	
	/**
	 * Construct a DataDescription from the map 
	 * @param values
	 */
	public DataDescription(Map<String, Object> values)
	{
		this();
		
		if (values.containsKey(FORMAT))
		{
			Object obj = values.get(FORMAT);
			if (obj != null)
			{
				m_DataFormat = DataFormat.valueOf(obj.toString().toUpperCase());
			}
		}		
		if (values.containsKey(TIME_FIELD_NAME))
		{
			Object obj = values.get(TIME_FIELD_NAME);
			if (obj != null)
			{
				m_TimeFieldName = obj.toString();
			}
		}
		if (values.containsKey(TIME_FORMAT))
		{
			Object obj = values.get(TIME_FORMAT);
			if (obj != null)
			{
				m_TimeFormat = obj.toString();
			}
		}
		if (values.containsKey(FIELD_DELIMITER))
		{
			Object obj = values.get(FIELD_DELIMITER);
			if (obj != null)
			{
				m_FieldDelimiter = obj.toString();
			}
		}
		if (values.containsKey(QUOTE_CHARACTER))
		{
			Object obj = values.get(QUOTE_CHARACTER);
			if (obj != null)
			{
				m_QuoteCharacter = obj.toString().charAt(0);
			}
		}	
	}
	
	/**
	 * The format of the data to be processed. 
	 * Defaults to {@link DataDescription.DataFormat#DELINEATED}
	 * @return The data format
	 */
	public DataFormat getFormat()
	{
		return m_DataFormat;
	}
	
	public void setFormat(DataFormat format)
	{
		m_DataFormat = format;
	}
	
	/**
	 * The name of the field containing the timestamp
	 * @return A String if set or <code>null</code>
	 */
	public String getTimeField()
	{
		return m_TimeFieldName;
	}

	public void setTimeField(String fieldName)
	{
		m_TimeFieldName = fieldName;
	}
	
	/**
	 * The strptime format of the time stamp
	 * If not set (is <code>null</code> or an empty string) then the date is
	 * assumed to be in seconds from the epoch
	 * @return A String if set or <code>null</code>
	 */
	public String getTimeFormat()
	{
		return m_TimeFormat;
	}
	
	public void setTimeFormat(String format)
	{
		m_TimeFormat = format;
	}
	
	/**
	 * If the data is in a delineated format with a header e.g. csv or tsv
	 * this is the delimiter character used. This is only applicable if
	 * {@linkplain #getFormat()} is {@link DataDescription.DataFormat#DELINEATED} 
	 * @return A String if set or <code>null</code>
	 */
	public String getFieldDelimiter()
	{
		return m_FieldDelimiter;
	}
	
	public void setFieldDelimiter(String delimiter)
	{
		m_FieldDelimiter = delimiter;
	}	
	
	/**
	 * The quote character used in delineated formats.
	 * Defaults to {@value #DEFAULT_QUOTE_CHAR}
	 * @return The delineated format quote character
	 */
	public char getQuoteCharacter()
	{
		return m_QuoteCharacter;
	}
	
	public void setQuoteCharacter(char value)
	{
		m_QuoteCharacter = value;
	}
	
	
	/**
	 * Returns true if the data described by this object needs
	 * at transforming before processing by autodetect.
	 * A transformation must be applied if either a timeformat is
	 * set or the data is in Json format.
	 */
	public boolean transform()
	{
		return m_TimeFormat != null || m_DataFormat == DataFormat.JSON;
	}
	
	/**
	 * Overridden equality test
	 */
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof DataDescription == false)
		{
			return false;
		}
		
		DataDescription that = (DataDescription)other;
		
		return this.m_DataFormat == that.m_DataFormat &&
				this.m_QuoteCharacter == that.m_QuoteCharacter &&
				JobDetails.bothNullOrEqual(this.m_TimeFieldName, that.m_TimeFieldName) &&
				JobDetails.bothNullOrEqual(this.m_TimeFormat, that.m_TimeFormat) &&
				JobDetails.bothNullOrEqual(this.m_FieldDelimiter, that.m_FieldDelimiter);	
	}
	
	/**
	 * Verify the data description configuration
	 * <ol>
	 * <li>Check the timeFormat - if set - is a valid format string</li>
	 * <li></li>
	 * </ol>
	 * @return true
	 * @throws JobConfigurationException
	 */
	public boolean verify()
	throws JobConfigurationException
	{
		if (m_TimeFormat != null && m_TimeFormat.isEmpty() == false)
		{
			try
			{
				new SimpleDateFormat(m_TimeFormat);
			}
			catch (IllegalArgumentException e)
			{
				
				throw new JobConfigurationException("Invalid Time format string", e);
			}
		}
		
		return true;
	}
}
