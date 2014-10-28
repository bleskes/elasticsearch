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
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

import org.apache.log4j.Logger;

/**
 * Provide access to the C++ model memory usage numbers
 * for the Java process.
 */
public class MemoryUsage
{
	/**
	 * Field Names
	 */
	public static final String ID = "id";
	public static final String TYPE = "memoryUsage";
	public static final String VALUE = "value";
	public static final String NUMBER_BY_FIELDS = "numberByFields";
	public static final String NUMBER_PARTITION_FIELDS = "numberPartitionFields";

	private static final Logger s_Logger = Logger.getLogger(MemoryUsage.class);

	private long m_MemoryUsage;
	private long m_NumberByFields;
	private long m_NumberPartitionFields;
	private String m_Type;

	public String getId()
	{
		return "memoryUsage";
	}

	public void setMemoryUsage(long m)
	{
		m_MemoryUsage = m;
	}

	public long getMemoryUsage()
	{
		return m_MemoryUsage;
	}

	public void setType(String s) 
	{
		m_Type = s;
	}
	
	public String getType()
	{
		return m_Type;
	}
	
	public void setNumberByFields(long m)
	{
		m_NumberByFields = m;
	}
	
	public long getNumberByFields()
	{
		return m_NumberByFields;
	}
	
	public void setNumberPartitionFields(long m)
	{
		m_NumberPartitionFields = m;
	}
	
	public long getNumberPartitionFields()
	{
		return m_NumberPartitionFields;
	}


	/**
	 * Create a new <code>MemoryUsage</code> and populate it from the JSON parser.
	 * The parser must be pointing at the start of the object then all the object's
	 * fields are read and if they match the property names the appropriate
	 * members are set.
	 *
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 *
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new MemoryUsage
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public MemoryUsage parseJson(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse MemoryUsage. The first token '" +
				parser.getText() + ", is not the start token";
			s_Logger.error(msg);
			throw new AutoDetectParseException(msg);
		}

		token = parser.nextToken();
		return parseJsonAfterStartObject(parser);
	}


	/**
	 * Create a new <code>MemoryUsage</code> and populate it from the JSON parser.
	 * The parser must be pointing at the first token inside the object.  It
	 * is assumed that prior code has validated that the previous token was
	 * the start of an object.  Then all the object's fields are read and if
	 * they match the property names the appropriate members are set.
	 *
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 *
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new MemoryUsage
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public MemoryUsage parseJsonAfterStartObject(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		MemoryUsage memoryUsage = new MemoryUsage();

		JsonToken token = parser.getCurrentToken();

		while (token != JsonToken.END_OBJECT)
		{
			switch(token)
			{
			case START_OBJECT:
				s_Logger.error("Start object parsed in MemoryUsage");
				break;
			case END_OBJECT:
				s_Logger.error("End object parsed in MemoryUsage");
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				memoryUsage.setType(fieldName);
				switch (fieldName)
				{
				case TYPE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						memoryUsage.setMemoryUsage(parser.getLongValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + TYPE + " : " + parser.getText()
										+ " as a long");
					}
					break;
				case NUMBER_BY_FIELDS:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						memoryUsage.setNumberByFields(parser.getLongValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + TYPE + " : " + parser.getText()
										+ " as a long");
					}
					break;
				case NUMBER_PARTITION_FIELDS:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						memoryUsage.setNumberPartitionFields(parser.getLongValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + TYPE + " : " + parser.getText()
										+ " as a long");
					}
					break;
				default:
					token = parser.nextToken();
					s_Logger.warn(String.format("Parse error unknown field in MemoryUsage %s:%s",
							fieldName, parser.nextTextValue()));
					break;
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in MemoryUsage, not "
						+ token);
				break;
			}

			token = parser.nextToken();
		}

		return memoryUsage;
	}

}

