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

package demo.app.splash.gxt;

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseTreeLoader;
import com.extjs.gxt.ui.client.data.DataProxy;

import demo.app.client.ApplicationResponseHandler;
import demo.app.data.gxt.DataSourceTreeModel;


/**
 * Tree loader for a data source tree.
 * @author Pete Harverson
 */
public class DataSourceTreeLoader extends BaseTreeLoader<DataSourceTreeModel>
{
	
	/**
	 * Creates a new DataSourceTreeLoader.
	 * @param proxy the data reader
	 */
	@SuppressWarnings("unchecked")
	public DataSourceTreeLoader(DataProxy proxy) 
	{
		super(proxy);
	}
	  
	
	/**
	 * Returns whether the given DataSourceTreeModel has children.
	 * @return <code>true</code> if the model has children, <code>false</code> otherwise.
	 */
	@Override
	public boolean hasChildren(DataSourceTreeModel dataSource)
	{
		return dataSource.isSourceType();
	}


	/**
	 * Loads the data source tree data for the specified configuration.
	 * @param config DataSourceTreeModel object whose 'child' data sources to load.
	 */
    @Override
    protected void loadData(Object config)
    {
        final Object loadConfig = config;
        
        ApplicationResponseHandler<List<DataSourceTreeModel>> callback = 
        	new ApplicationResponseHandler<List<DataSourceTreeModel>>() {
                public void uponFailure(Throwable caught) 
                {
                	onLoadFailure(loadConfig, caught);
                }


                public void uponSuccess(List<DataSourceTreeModel> result)
                {
					onLoadSuccess(loadConfig, result);
                }
          };
          
          proxy.load(reader, config, callback);
    }
}
