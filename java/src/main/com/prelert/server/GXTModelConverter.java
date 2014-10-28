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

package com.prelert.server;

import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.SourceConnectionConfig;


/**
 * Class containing a number of static methods for converting Prelert data types
 * to and from GXT <code>BaseModelData</code> sub-classes.
 * @author Pete Harverson
 */
public class GXTModelConverter
{
	
	/**
     * Builds a <code>SourceConnectionConfig</code> object from a GXT
     * <code>DataTypeConnectionModel</code>.
     * @param connectionModel GXT <code>DataTypeConnectionModel</code> from which to return
     * 	the <code>SourceConnectionConfig</code>.
     */
	public static SourceConnectionConfig getConnectionConfig(DataTypeConnectionModel connectionModel)
    {
    	SourceConnectionConfig config = new SourceConnectionConfig();
    	config.setHost(connectionModel.getHost());
    	config.setPort(connectionModel.getPort());
    	config.setUsername(connectionModel.getUsername());
    	config.setPassword(connectionModel.getPassword());
    	
    	return config;
    }
	
	
	/**
     * Builds a GXT <code>DataTypeConfigModel</code> model from the supplied
     * <code>DataTypeConfig</code> data type configuration object.
     * @param config <code>DataTypeConfig</code> data type configuration.
     * @return GXT model class.
     */
    public static DataTypeConfigModel createDataTypeConfigModel(DataTypeConfig config)
    {
    	DataTypeConfigModel model = new DataTypeConfigModel();
		model.setDataType(config.getDataType());
		
		// If a plugin property, set the name of the database that the plugin will connect to.
		String databaseName = config.getPluginProperties().get("DataBaseName");
		if (databaseName != null)
		{
			model.setDatabaseName(databaseName);
		}
		
		DataTypeConnectionModel connectionModel = new DataTypeConnectionModel();
		
		SourceConnectionConfig connectionConfig = config.getSourceConnectionConfig();
		if (connectionConfig != null)
		{			
			connectionModel.setHost(connectionConfig.getHost());
			connectionModel.setUsername(connectionConfig.getUsername());
			connectionModel.setPassword(connectionConfig.getPassword());
			
			Integer port = connectionConfig.getPort();
			if (port != null)
			{
				connectionModel.setPort(port);
			}	
		}
		model.setConnectionConfig(connectionModel);

		return model;

    }
}
