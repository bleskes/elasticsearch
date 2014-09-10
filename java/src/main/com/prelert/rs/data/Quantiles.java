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

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.parsing.AutoDetectParseException;

import org.apache.log4j.Logger;

/**
 * Quantiles Result POJO
 */
@JsonIgnoreProperties({"kind"})
@JsonInclude(Include.NON_NULL)
public class Quantiles
{
	/**
	 * Field Names
	 */
	public static final String ID = "id";
	public static final String TIMESTAMP = "timestamp";
	public static final String QUANTILE_KIND = "quantileKind";
	public static final String QUANTILE_STATE = "quantileState";

	/**
	 * Elasticsearch type
	 */
	public static final String TYPE = "quantiles";

	private static final Logger s_Logger = Logger.getLogger(Quantiles.class);

	private Date m_Timestamp;

	/**
	 * The kind of quantiles is also the Elasticsearch ID, so m_Kind is used for
	 * both
	 */
	private String m_Kind;
	private String m_State;


	public Date getTimestamp()
	{
		return m_Timestamp;
	}


	public void setTimestamp(Date timestamp)
	{
		m_Timestamp = timestamp;
	}


	/**
	 * The ID is the kind of quantiles
	 */
	public String getId()
	{
		return m_Kind;
	}


	/**
	 * The ID is the kind of quantiles
	 */
	public void setId(String id)
	{
		m_Kind = id;
	}


	public String getKind()
	{
		return m_Kind;
	}


	public void setKind(String kind)
	{
		m_Kind = kind;
	}


	public String getState()
	{
		return m_State;
	}


	public void setState(String state)
	{
		m_State = state;
	}


	/**
	 * Create a new <code>Quantiles</code> and populate it from the JSON parser.
	 * The parser must be pointing at the start of the object then all the object's
	 * fields are read and if they match the property names the appropriate
	 * members are set.
	 *
	 * Does not validate that all the properties (or any) have been set but if
	 * parsing fails an exception will be thrown.
	 *
	 * @param parser The JSON Parser should be pointing to the start of the object,
	 * when the function returns it will be pointing to the end.
	 * @return The new quantiles
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public Quantiles parseJson(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		JsonToken token = parser.getCurrentToken();
		if (JsonToken.START_OBJECT != token)
		{
			String msg = "Cannot parse Quantiles. The first token '" +
					parser.getText() + ", is not the start token";
			s_Logger.error(msg);

			throw new AutoDetectParseException(msg);
		}

		token = parser.nextToken();
		return parseJsonAfterStartObject(parser);
	}


	/**
	 * Create a new <code>Quantiles</code> and populate it from the JSON parser.
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
	 * @return The new quantiles
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	static public Quantiles parseJsonAfterStartObject(JsonParser parser)
	throws JsonParseException, IOException, AutoDetectParseException
	{
		Quantiles quantiles = new Quantiles();

		JsonToken token = parser.getCurrentToken();
		while (token != JsonToken.END_OBJECT)
		{
			switch(token)
			{
			case START_OBJECT:
				s_Logger.error("Start object parsed in quantiles");
				break;
			case END_OBJECT:
				s_Logger.error("End object parsed in quantiles");
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				switch (fieldName)
				{
				case TIMESTAMP:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_NUMBER_INT)
					{
						// convert seconds to ms
						long val = parser.getLongValue() * 1000;
						quantiles.setTimestamp(new Date(val));
					}
					else
					{
						s_Logger.warn("Cannot parse " + TIMESTAMP + " : " + parser.getText()
										+ " as a long");
					}
					break;
				case QUANTILE_KIND:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						quantiles.setKind(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + QUANTILE_KIND + " : " + parser.getText()
										+ " as a string");
					}
					break;
				case QUANTILE_STATE:
					token = parser.nextToken();
					if (token == JsonToken.VALUE_STRING)
					{
						quantiles.setState(parser.getText());
					}
					else
					{
						s_Logger.warn("Cannot parse " + QUANTILE_STATE + " : " + parser.getText()
										+ " as a string");
					}
					break;
				default:
					s_Logger.warn(String.format("Parse error unknown field in Quantiles %s:%s",
							fieldName, parser.nextTextValue()));
					break;
				}
				break;
			default:
				s_Logger.warn("Parsing error: Only simple fields expected in quantiles not "
						+ token);
				break;
			}

			token = parser.nextToken();
		}

		return quantiles;
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = (m_Kind == null) ? 0 : m_Kind.hashCode();
		result = prime * result + ((m_State == null) ? 0 : m_State.hashCode());
		return result;
	}


	/**
	 * Compare all the fields.
	 */
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}

		if (other instanceof Quantiles == false)
		{
			return false;
		}

		Quantiles that = (Quantiles)other;

		if (m_Kind == null)
		{
			if (that.m_Kind != null)
			{
				return false;
			}
		}
		else
		{
			if (!m_Kind.equals(that.m_Kind))
			{
				return false;
			}
		}

		if (m_State == null)
		{
			if (that.m_State != null)
			{
				return false;
			}
		}
		else
		{
			if (!m_State.equals(that.m_State))
			{
				return false;
			}
		}

		return true;
	}
}

