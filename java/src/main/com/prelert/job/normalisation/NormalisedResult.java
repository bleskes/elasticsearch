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

package com.prelert.job.normalisation;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Store the output of the normaliser process
 * 
 * {"rawAnomalyScore":"0.0","normalizedProbability":"","anomalyScore":"0"}
 *
 */
public class NormalisedResult 
{
	static public final String RAW_ANOMALY_SCORE = "rawAnomalyScore";
	static public final String NORMALIZED_PROBABILITY = "normalizedProbability";
	static public final String SYS_CHANGE_SCORE = "anomalyScore";
	static public final String ID = "id";
	
	
	private double m_RawAnomalyScore;
	private double m_NormalizedProbability;
	private double m_NormalizedSysChangeScore;
	private String m_Id;


	public NormalisedResult()
	{
		
	}
	
	public NormalisedResult(NormalisedResult other)
	{
		m_RawAnomalyScore = other.m_RawAnomalyScore;
		m_NormalizedSysChangeScore = other.m_NormalizedSysChangeScore;
		m_NormalizedProbability = other.m_NormalizedProbability;
		m_Id = other.m_Id;				
	}
	
	public double getRawAnomalyScore() 
	{
		return m_RawAnomalyScore;
	}
	
	public void setRawAnomalyScore(double rawAnomalyScore) 
	{
		this.m_RawAnomalyScore = rawAnomalyScore;
	}
	
	public double getNormalizedProbability() 
	{
		return m_NormalizedProbability;
	}
	
	public void setNormalizedProbability(double normalizedProbability) 
	{
		this.m_NormalizedProbability = normalizedProbability;
	}
	
	public double getNormalizedSysChangeScore() 
	{
		return m_NormalizedSysChangeScore;
	}
	
	public void setNormalizedSysChangeScore(double normalizedSysChangeScore) 
	{
		this.m_NormalizedSysChangeScore = normalizedSysChangeScore;
	}


	public String getId()
	{
		return m_Id;
	}
	
	public void setId(String id)
	{
		m_Id = id;
	}
	
	
	static public NormalisedResult parseJson(JsonParser parser, Logger logger)
	throws JsonParseException, IOException
	{
		NormalisedResult result = new NormalisedResult();
					
		JsonToken token = parser.nextToken();
		while (token != JsonToken.END_OBJECT)
		{						
			switch(token)
			{
			case START_OBJECT:
				break;
			case FIELD_NAME:
				String fieldName = parser.getCurrentName();
				token = parser.nextToken();
				switch (fieldName)
				{
				case RAW_ANOMALY_SCORE:
					// TODO this is string should be output as a double
//					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
//					{
//						result.setRawAnomalyScore(parser.getDoubleValue());
//					}
					
					if (token == JsonToken.VALUE_STRING)
					{
						String val = parser.getValueAsString();
						if (val.isEmpty() == false)
						{
							try						
							{
								result.setRawAnomalyScore(Double.parseDouble(val));
							}
							catch (NumberFormatException nfe)
							{
								logger.warn("Cannot parse " + RAW_ANOMALY_SCORE + " : " + parser.getText() 
										+ " as a double");
							}	
						}
					}	
					else
					{
						logger.warn("Cannot parse " + RAW_ANOMALY_SCORE + " : " + parser.getText() 
										+ " as a double");
					}
					break;
				case SYS_CHANGE_SCORE:
					// TODO this is string should be output as a double
//					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
//					{
//						result.setNormalizedSysChangeScore(parser.getDoubleValue());
//					}
					
					if (token == JsonToken.VALUE_STRING)
					{
						String val = parser.getValueAsString();
						if (val.isEmpty() == false)
						{
							try
							{
								result.setNormalizedSysChangeScore(Double.parseDouble(val));
							}
							catch (NumberFormatException nfe)
							{
								logger.warn("Cannot parse " + SYS_CHANGE_SCORE + " : " + parser.getText() 
										+ " as a double");
							}		
						}
					}					
					else
					{
						logger.warn("Cannot parse " + SYS_CHANGE_SCORE + " : " + parser.getText() 
										+ " as a double");
					}					
					break;
				case NORMALIZED_PROBABILITY:
					// TODO this is string should be output as a double
//					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
//					{
//						result.setNormalizedProbability(parser.getDoubleValue());
//					}
					
					if (token == JsonToken.VALUE_STRING)
					{
						String val = parser.getValueAsString();
						if (val.isEmpty() == false)
						{
							try
							{
								result.setNormalizedProbability(Double.parseDouble(val));
							}
							catch (NumberFormatException nfe)
							{
								logger.warn("Cannot parse " + NORMALIZED_PROBABILITY + " : " + parser.getText() 
										+ " as a double");
							}
						}
					}
					else
					{
						logger.warn("Cannot parse " + NORMALIZED_PROBABILITY + " : " + parser.getText() 
										+ " as a double");
					}	
					break;
				case ID:
					result.setId(parser.getValueAsString());
					break;
				default:
					logger.trace(String.format("Parsed unknown field in NormalisedResult %s:%s", 
							fieldName, parser.getValueAsString()));
					break;
				}
				break;
			default:
				logger.warn("Parsing error: Only simple fields expected in NormalisedResult not "
						+ token);
				break;
			}
			
			token = parser.nextToken();
		}
		
		return result;
	}
}
