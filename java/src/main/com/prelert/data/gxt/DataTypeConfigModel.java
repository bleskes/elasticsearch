/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.data.gxt;

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Extension of the GXT BaseModelData class encapsulating configuration data for 
 * a data type.
 * @author Pete Harverson
 */
public class DataTypeConfigModel extends BaseModelData
{
    private static final long serialVersionUID = -5936951990766803609L;
    
    @SuppressWarnings("unused")
	private DataTypeConnectionModel		m_ConnectionConfig;		// DO NOT DELETE - custom field serializer.

    
    /**
	 * Returns the name of the data type. This refers to the type of data 
	 * being collected, such as Introscope, Scom or Nimsoft.
	 * @return the name of the data type.
	 */
	public String getDataType()
	{
		return get("dataType");
	}
	
	
	/**
	 * Sets the name of the data type. This should refer to the type of data 
	 * being collected, such as Introscope, Scom or Nimsoft.
	 * @param dataType the name of the data type.
	 */
	public void setDataType(String dataType)
	{
		set("dataType", dataType);
	}
    
	
	/**
	 * Returns the configuration data for the connection to this data type.
	 * @return <code>DataTypeConnectionModel</code> encapsulating properties
	 * 	of the connection. Note that not all fields may be populated.
	 */
	public DataTypeConnectionModel getConnectionConfig()
	{
		return get("connectionConfig", new DataTypeConnectionModel());
	}
	
	
	/**
	 * Sets the configuration data for the connection to this data type.
	 * @param connectionConfig <code>DataTypeConnectionModel</code> encapsulating 
	 * 	properties of the connection. Not all fields in the object need be populated.
	 */
	public void setConnectionConfig(DataTypeConnectionModel connectionConfig)
	{
		set("connectionConfig", connectionConfig);
	}
	
	
	/**
	 * Returns the name of the database storing the data for analysis.
	 * @return name of the database holding data for analysis.
	 */
	public String getDatabaseName()
	{
		return get("databaseName");
	}
	
	
	/**
	 * Sets the name of the database storing the data for analysis. 
	 * @param databaseName name of the database holding data for analysis.
	 */
	public void setDatabaseName(String databaseName)
	{
		set("databaseName", databaseName);
	}
	
	
	/**
	 * Returns a summary of the configuration properties for this data type.
	 * @return <code>String</code> representation of the configuration.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
