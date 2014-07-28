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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The initialisation state for the normalisation process. 
 * 
 * The distinguisher string should be the concatenation of 
 * <ul>
 * <li>By Field Value</li>
 * <li>By Field Name</li>
 * <li>Over Field Value</li>
 * <li>Over Field Name</li>
 * <li>Partition Field Value</li>
 * <li>Partition Field Name</li>
 * </ul>
 * Not the value fields come before the name fields
 */
public class InitialState implements Iterable<InitialState.InitialStateRecord>
{
	private List<InitialStateRecord> m_StateRecords;
	
	public InitialState()
	{
		m_StateRecords = new ArrayList<>();		
	}
	
	public void addStateRecord(String epoch, String anomalyScore)
	{
		m_StateRecords.add(this.new InitialStateRecord(epoch, anomalyScore));
	}
	
	public void addStateRecord(String epoch, String probability, String distinguisher)
	{
		m_StateRecords.add(this.new InitialStateRecord(epoch, probability, distinguisher));
	}
	
	/**
	 * Individual state record
	 */
	public class InitialStateRecord 
	{
		private String m_AnomalyScore;
		private String m_Probabilty;
		private String m_Epoch;
		private String m_Distinguisher;

		public InitialStateRecord(String epoch, String anomalyScore)
		{
			m_Epoch = epoch;
			m_AnomalyScore = anomalyScore;
		}

		public InitialStateRecord(String epoch, String probability, String distinguisher)
		{
			m_Epoch = epoch;
			m_Probabilty = probability;
			m_Distinguisher = distinguisher;
		}

		public String getEpoch()
		{
			return m_Epoch;
		}

		public String getAnomalyScore()
		{
			return m_AnomalyScore;
		}

		public String getProbabilty()
		{
			return m_Probabilty;
		}

		public String getDistinguisher()
		{
			return m_Distinguisher;
		}
		
		public String toSysChangeCsv()
		{
			return String.format("%s,%s\n", m_Epoch, m_AnomalyScore);
		}
		
		public String toUnusualCsv()
		{
			return String.format("%s,%s,%s\n", m_Epoch, m_Probabilty, m_Distinguisher);
		}
	}

	@Override
	public Iterator<InitialState.InitialStateRecord> iterator() 
	{
		return m_StateRecords.iterator();
	}
}
