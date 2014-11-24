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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.prelert.rs.data.parsing.AutoDetectParseException;

import org.apache.log4j.Logger;

/**
 * Provide access to the C++ model memory usage numbers
 * for the Java process.
 */
@JsonIgnoreProperties({"id"})
public class ModelSizeStats
{
	/**
	 * Field Names
	 */
	public static final String ID = "id";
	public static final String MODEL_BYTES = "modelBytes";
	public static final String TOTAL_BY_FIELD_COUNT = "totalByFieldCount";
	public static final String TOTAL_OVER_FIELD_COUNT = "totalOverFieldCount";
	public static final String TOTAL_PARTITION_FIELD_COUNT = "totalPartitionFieldCount";

    /**
     * Elasticsearch type
     */
	public static final String TYPE = "modelSizeStats";
    
    private static final Logger s_Logger = Logger.getLogger(ModelSizeStats.class);

	private long m_ModelBytes;
	private long m_TotalByFieldCount;
    private long m_TotalOverFieldCount;
	private long m_TotalPartitionFieldCount;

	public String getId()
	{
		return TYPE;
	}

	public void setId(String id)
	{
	}

	public void setModelBytes(long m)
	{
		m_ModelBytes = m;
	}

	public long getModelBytes()
	{
		return m_ModelBytes;
	}

	public void setTotalByFieldCount(long m)
	{
		m_TotalByFieldCount = m;
	}

	public long getTotalByFieldCount()
	{
		return m_TotalByFieldCount;
	}

	public void setTotalPartitionFieldCount(long m)
	{
		m_TotalPartitionFieldCount = m;
	}

	public long getTotalPartitionFieldCount()
	{
		return m_TotalPartitionFieldCount;
	}

    public void setTotalOverFieldCount(long m)
    {
        m_TotalOverFieldCount = m;
    }

    public long getTotalOverFieldCount()
    {
        return m_TotalOverFieldCount;
    }


	/**
	 * Create a new <code>ModelSizeStats</code> and populate it from the JSON parser.
	 * The parser must be pointing at the start of the object then all the object's
	 * fields are read and if they match the property names the appropriate
	 * members are set.
	 *
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 *
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new ModelSizeStats
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public ModelSizeStats parseJson(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse ModelSizeStats. The first token '" +
				parser.getText() + ", is not the start token";
			s_Logger.error(msg);
			throw new AutoDetectParseException(msg);
		}

		token = parser.nextToken();
		return parseJsonAfterStartObject(parser);
	}


	/**
	 * Create a new <code>ModelSizeStats</code> and populate it from the JSON parser.
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
	 * @return The new ModelSizeStats
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public ModelSizeStats parseJsonAfterStartObject(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		ModelSizeStats modelSizeStats = new ModelSizeStats();

		JsonToken token = parser.getCurrentToken();

		while (token != JsonToken.END_OBJECT)
		{
			switch(token)
			{
			case START_OBJECT:
				s_Logger.error("Start object parsed in ModelSizeStats");
				break;
			case END_OBJECT:
				s_Logger.error("End object parsed in ModelSizeStats");
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				switch (fieldName)
				{
				case TYPE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						modelSizeStats.setModelBytes(parser.getLongValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
										+ " as a long");
					}
					break;
				case TOTAL_BY_FIELD_COUNT:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						modelSizeStats.setTotalByFieldCount(parser.getLongValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
										+ " as a long");
					}
					break;
                case TOTAL_OVER_FIELD_COUNT:
                    token = parser.nextToken();
                    if (token == JsonToken.VALUE_NUMBER_INT)
                    {
                        modelSizeStats.setTotalOverFieldCount(parser.getLongValue());
                    }
                    else
                    {
                        s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
                                        + " as a long");
                    }
                    break;
				case TOTAL_PARTITION_FIELD_COUNT:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						modelSizeStats.setTotalPartitionFieldCount(parser.getLongValue());
					}
					else
					{
						s_Logger.warn("Cannot parse " + fieldName + " : " + parser.getText()
										+ " as a long");
					}
					break;
				default:
					token = parser.nextToken();
					s_Logger.warn(String.format("Parse error unknown field in ModelSizeStats %s:%s",
							fieldName, parser.nextTextValue()));
					break;
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in ModelSizeStats, not "
						+ token);
				break;
			}

			token = parser.nextToken();
		}

		return modelSizeStats;
	}

}

