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
 * {"anomalyScore":"0.0","normalizedUnusualScore":"","normalizedSysChangeScore":"0"}
 *
 */
public class NormalisedResult 
{
	static public final String ANOMALY_SCORE = "anomalyScore";
	static public final String UNUSUAL_SCORE = "normalizedUnusualScore";
	static public final String SYS_CHANGE_SCORE = "normalizedSysChangeScore";
	
	
	private double m_AnomalyScore;
	private double m_NormalizedUnusualScore;
	private double m_NormalizedSysChangeScore;
	
	public double getAnomalyScore() 
	{
		return m_AnomalyScore;
	}
	
	public void setAnomalyScore(double anomalyScore) 
	{
		this.m_AnomalyScore = anomalyScore;
	}
	
	public double getNormalizedUnusualScore() 
	{
		return m_NormalizedUnusualScore;
	}
	
	public void setNormalizedUnusualScore(double normalizedUnusualScore) 
	{
		this.m_NormalizedUnusualScore = normalizedUnusualScore;
	}
	
	public double getNormalizedSysChangeScore() 
	{
		return m_NormalizedSysChangeScore;
	}
	
	public void setNormalizedSysChangeScore(double normalizedSysChangeScore) 
	{
		this.m_NormalizedSysChangeScore = normalizedSysChangeScore;
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
				case ANOMALY_SCORE:
					// TODO this is string should be output as a double
//					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
//					{
//						result.setAnomalyScore(parser.getDoubleValue());
//					}
					
					if (token == JsonToken.VALUE_STRING)
					{
						try
						{
							result.setAnomalyScore(Double.parseDouble(parser.getValueAsString()));
						}
						catch (NumberFormatException nfe)
						{
							logger.warn("Cannot parse " + ANOMALY_SCORE + " : " + parser.getText() 
								+ " as a double");
						}							
					}	
					else
					{
						logger.warn("Cannot parse " + ANOMALY_SCORE + " : " + parser.getText() 
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
						try
						{
							result.setNormalizedSysChangeScore(Double.parseDouble(parser.getValueAsString()));
						}
						catch (NumberFormatException nfe)
						{
							logger.warn("Cannot parse " + SYS_CHANGE_SCORE + " : " + parser.getText() 
								+ " as a double");
						}							
					}					
					else
					{
						logger.warn("Cannot parse " + SYS_CHANGE_SCORE + " : " + parser.getText() 
										+ " as a double");
					}					
					break;
				
				case UNUSUAL_SCORE:
					// TODO this is string should be output as a double
//					if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)	
//					{
//						result.setNormalizedUnusualScore(parser.getDoubleValue());
//					}
					
					if (token == JsonToken.VALUE_STRING)
					{
						try
						{
							result.setNormalizedUnusualScore(Double.parseDouble(parser.getValueAsString()));
						}
						catch (NumberFormatException nfe)
						{
							logger.warn("Cannot parse " + UNUSUAL_SCORE + " : " + parser.getText() 
								+ " as a double");
						}
					}
					else
					{
						logger.warn("Cannot parse " + UNUSUAL_SCORE + " : " + parser.getText() 
										+ " as a double");
					}	
					break;
				default:
					logger.warn(String.format("Parsed unknown field in NormalisedResult %s:%s", 
							fieldName, parser.nextTextValue()));
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
