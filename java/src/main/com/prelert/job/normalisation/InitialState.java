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

/**
 * The initialisation state for the normalisation process. 
 */
public class InitialState 
{
	private double m_AnomalyScore;
	private double m_Probabilty;
	private long m_Epoch;
	private String m_Distinguisher;
	
	public InitialState(long epoch, double anomalyScore)
	{
		m_Epoch = epoch;
		m_AnomalyScore = anomalyScore;
	}
	
	public InitialState(long epoch, double probability, String distinguisher)
	{
		m_Epoch = epoch;
		m_Probabilty = probability;
		m_Distinguisher = distinguisher;
	}

	public long getEpoch()
	{
		return m_Epoch;
	}
	
	public double getAnomalyScore()
	{
		return m_AnomalyScore;
	}
	
	public double getProbabilty()
	{
		return m_Probabilty;
	}
	
	public String getDistinguisher()
	{
		return m_Distinguisher;
	}
	
}
