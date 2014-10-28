/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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
 ***********************************************************/

package com.prelert.data;

/**
 * Class encapsulating a source of data analysed by the Prelert engine.
 * Each data source has a data source type, such as p2pslogs or system_udp,
 * and the name of the source (server). 
 * @author Pete Harverson
 */
public class DataSource
{
	private DataSourceType 	m_DataSourceType;
	private String 			m_Source;
	private int 			m_Count = -1;


	/**
	 * Returns the type of data obtained from this source e.g. p2ps logs,
	 * p2psmon user usage.
	 * @return dataSourceType the data source type.
	 */
	public DataSourceType getDataSourceType()
	{
		return m_DataSourceType;
	}


	/**
	 * Sets the type of data obtained from this source e.g. p2ps logs,
	 * p2psmon user usage.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		m_DataSourceType = dataSourceType;
	}


	/**
	 * Returns the name of the source (server) for this data source.
	 * @return the name of the source (server).
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Sets the name of the source (server) for this data source.
	 * @param source the name of the source (server).
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}


	/**
	 * If available, returns the count of data obtained from this data source.
	 * @return the data point count for this source, or -1 if the count has not
	 * 		been set for this object.
	 */
	public int getCount()
	{
		return m_Count;
	}


	/**
	 * Sets the count of data obtained from this data source.
	 * @param count the data point count for this source.
	 */
	public void setCount(int count)
	{
		m_Count = count;
	}
	
	
	/**
	 * Returns a String representation of this data source.
	 * @return String representation of the data source.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append("{type=");
		strRep.append(m_DataSourceType);
		strRep.append(", source=");
		strRep.append(m_Source);
		if (m_Count >= 0)
		{
			strRep.append(", count=");
			strRep.append(m_Count);
		}
		strRep.append('}');
		
		return strRep.toString();
	}

}
