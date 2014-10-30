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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.DataSourceDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.data.gxt.DataSourceTreeModel;
import com.prelert.data.gxt.MetricPathListLoadConfig;
import com.prelert.data.gxt.MetricTreeNodeModel;
import com.prelert.service.DataSourceQueryService;


/**
 * Server-side implementation of the service for retrieving information on 
 * Prelert data sources.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class DataSourceQueryServiceImpl extends RemoteServiceServlet 
	implements DataSourceQueryService
{
	static Logger s_Logger = Logger.getLogger(DataSourceQueryServiceImpl.class);
	
	private DataSourceDAO	m_DataSourceDAO;
	
	
	/**
	 * Sets the DataSourceDAO to be used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public void setDataSourceDAO(DataSourceDAO dataSourceDAO)
	{
		m_DataSourceDAO = dataSourceDAO;
	}
	
	
	/**
	 * Returns the DataSourceDAO being used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public DataSourceDAO getDataSourceDAO()
	{
		return m_DataSourceDAO;
	}
	
	
	@Override
	public List<DataSourceTreeModel> getDataSourceTypeTreeModels(boolean includeCounts)
	{
		List<DataSourceType> dataSourceTypes = m_DataSourceDAO.getDataSourceTypes();
		
		// Convert to list of DataSourceTreeModel objects.
		List<DataSourceTreeModel> dataSourceModels =  new ArrayList<DataSourceTreeModel>();
		DataSourceTreeModel model;
		for (DataSourceType sourceType : dataSourceTypes)
		{
			// Only add in Notifications and Time Series.
			if (sourceType.getDataCategory() == DataSourceCategory.NOTIFICATION ||
					sourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				model = new DataSourceTreeModel();
				model.setText(sourceType.getName());
				model.setDataSourceType(sourceType);
				if (includeCounts == true)
				{
					model.setCount(sourceType.getCount());
				}
				dataSourceModels.add(model);
			}
		}
		
		return dataSourceModels;
	}
	
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return the complete list of sources for the given data source type.
	 */
	protected List<DataSourceTreeModel> getDataSourcesForType(
			DataSourceTreeModel sourceType, boolean includeCounts)
	{	
		List<DataSource> dataSources = 
			m_DataSourceDAO.getDataSources(sourceType.getDataSourceType());
		
		// Convert to list of DataSourceTreeModel objects.
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		DataSourceTreeModel model;
		for (DataSource dataSource : dataSources)
		{
			model = new DataSourceTreeModel();
			model.setText(dataSource.getSource());
			model.setSource(dataSource.getSource());
			model.setDataSourceType(dataSource.getDataSourceType());
			
			if (includeCounts == true)
			{
				model.setCount(dataSource.getCount());
			}
			
			dataSourceModels.add(model);
		}
		
		return dataSourceModels;
	}
	
	
	@Override
	public List<DataSourceTreeModel> getDataSourceTreeModelsByCount(
			DataSourceTreeModel dataSourceType)
	{
		List<DataSource> dataSources = m_DataSourceDAO.getDataSources(dataSourceType.getDataSourceType());
		
		// Convert to list of DataSourceTreeModel objects.
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		DataSourceTreeModel model;
		for (DataSource dataSource : dataSources)
		{
			model = new DataSourceTreeModel();
			model.setText(dataSource.getSource());
			model.setSource(dataSource.getSource());
			model.setDataSourceType(dataSource.getDataSourceType());
			model.setCount(dataSource.getCount());
			
			dataSourceModels.add(model);
		}
		
		// Place in descending order of data point count.
		Collections.sort(dataSourceModels, new ModelCountComparator());
		
		return dataSourceModels;
	}
	
	
	@Override
	public List<DataSourceTreeModel> getDataSourceTreeModels(
			DataSourceTreeModel dataSource, boolean includeCounts)
    { 	
    	if (dataSource == null)
    	{
    		// Create the 'Analysed Data' root node.
    		DataSourceTreeModel analysedDataRoot = new DataSourceTreeModel();
    		analysedDataRoot.setText("Analysed Data");	// Overwritten client-side with I18N value.
    		
    		List<DataSourceTreeModel> analysedDataModel = new ArrayList<DataSourceTreeModel>(1);
    		analysedDataModel.add(analysedDataRoot);
    		return analysedDataModel;
    	}
    	else
    	{
    		List<DataSourceTreeModel> dataSourceModels;
    		
    		if (dataSource.getDataSourceCategory() == null)
    		{
    			// Obtain the list of data source types. 
    			dataSourceModels = getDataSourceTypeTreeModels(includeCounts);
        		
        		// Place notification types before time series.
        		Collections.sort(dataSourceModels, new ModelTypeComparator());
    		}
    		else
    		{
    			// Return the list of sources for this data source type.
    			dataSourceModels = getDataSourcesForType(dataSource, includeCounts);
    		}
    		
    		return dataSourceModels;
    	}
    	
    }
	

	@Override
    public List<DataSourceModel> getDataSourceTypes()
    {	
    	List<DataSourceType> dataSourceTypes = m_DataSourceDAO.getDataSourceTypes();
    	
    	// Place in ascending alphabetical order of data source type name.
		Collections.sort(dataSourceTypes, new DataSourceTypeNameComparator());
		
		// Convert to list of DataSourceModel objects.
		List<DataSourceModel> dataSourceModels =  new ArrayList<DataSourceModel>();
		DataSourceModel model;
		for (DataSourceType sourceType : dataSourceTypes)
		{
			// Only add in Notifications and Time Series.
			if (sourceType.getDataCategory() == DataSourceCategory.NOTIFICATION ||
					sourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				model = new DataSourceModel();
				model.setDataSourceType(sourceType);
				model.setCount(sourceType.getCount());
				
				dataSourceModels.add(model);
			}
		}
		
		return dataSourceModels;
    }


	@Override
	public List<DataSourceModel> getDataSources(DataSourceType dataSourceType)
    {
    	List<DataSource> dataSources = m_DataSourceDAO.getDataSources(dataSourceType);
		
		// Convert to list of DataSourceModel objects.
		List<DataSourceModel> dataSourceModels = 
			new ArrayList<DataSourceModel>(dataSources.size());
		DataSourceModel model;
		for (DataSource dataSource : dataSources)
		{
			model = new DataSourceModel();
			model.setDataSourceType(dataSourceType);
			model.setSource(dataSource.getSource());
			model.setCount(dataSource.getCount());
			
			dataSourceModels.add(model);
		}
		
		return dataSourceModels;
    }


	@Override
	public BaseListLoadResult<BaseModelData> getAllDataSourcesListLoadResult()
	{
		List<DataSource> dataSources = m_DataSourceDAO.getAllDataSources();
		
		List<BaseModelData> models = new ArrayList<BaseModelData>();
		
		BaseModelData dataSourceModel;
		for (DataSource dataSource : dataSources)
		{
			dataSourceModel = new BaseModelData();
			dataSourceModel.set("dataSourceName", dataSource.getDataSourceType().getName());
			dataSourceModel.set("dataSourceCategory", dataSource.getDataSourceType().getDataCategory().toString());
			dataSourceModel.set("source", dataSource.getSource());
			dataSourceModel.set("count", dataSource.getCount());
			
			models.add(dataSourceModel);
		}
		
		return new BaseListLoadResult<BaseModelData>(models);
	}
	
	
	@Override
	public BaseListLoadResult<BaseModelData> getDataSourcesListLoadResult(DataSourceType dataSourceType)
    {
    	List<DataSource> dataSources = m_DataSourceDAO.getDataSources(dataSourceType);
    	
    	List<BaseModelData> models = new ArrayList<BaseModelData>();
		
		BaseModelData dataSourceModel;
		for (DataSource dataSource : dataSources)
		{
			dataSourceModel = new BaseModelData();
			dataSourceModel.set("dataSourceName", dataSource.getDataSourceType().getName());
			dataSourceModel.set("dataSourceCategory", dataSource.getDataSourceType().getDataCategory().toString());
			dataSourceModel.set("source", dataSource.getSource());
			dataSourceModel.set("count", dataSource.getCount());
			
			models.add(dataSourceModel);
		}
		
		s_Logger.debug("getDataSourcesList() returning " + models.size());
		
		return new BaseListLoadResult<BaseModelData>(models);
    }
	
	
	@Override
	public BaseListLoadResult<MetricTreeNodeModel> getNextLevel(MetricPathListLoadConfig config)
	{
		List<MetricTreeNode> treeNodes = m_DataSourceDAO.getDataSourceTreeNextLevel(
				config.getType(), config.getPreviousPath(), config.getCurrentValue(), 
				config.getOpaqueNum(), config.getOpaqueString());
		
		return createMetricTreeListLoadResult(treeNodes);
	}
	
	
	@Override
	public BaseListLoadResult<MetricTreeNodeModel> getPreviousLevel(MetricPathListLoadConfig config)
	{
		List<MetricTreeNode> treeNodes = m_DataSourceDAO.getDataSourceTreePreviousLevel(
				config.getType(), config.getPreviousPath(),
				config.getOpaqueNum(), config.getOpaqueString());
		
		return createMetricTreeListLoadResult(treeNodes);
	}
	

    @Override
    public BaseListLoadResult<MetricTreeNodeModel> getCurrentLevel(MetricPathListLoadConfig config)
    {
    	List<MetricTreeNode> treeNodes = m_DataSourceDAO.getDataSourceTreeCurrentLevel(
				config.getType(), config.getPreviousPath(),
				config.getOpaqueNum(), config.getOpaqueString());
    	s_Logger.debug("getCurrentLevel() returning " + treeNodes.size() + " nodes: " + treeNodes);
		
		return createMetricTreeListLoadResult(treeNodes);
    }


	/**
	 * Creates a metric tree list load result from the supplied list of MetricTreeNode data.
	 * @param treeNodes list of MetricTreeNode data
	 * @return list load result.
	 */
	protected BaseListLoadResult<MetricTreeNodeModel> createMetricTreeListLoadResult(
			List<MetricTreeNode> treeNodes)
	{
		ArrayList<MetricTreeNodeModel> modelList = new ArrayList<MetricTreeNodeModel>();
		
		MetricTreeNodeModel model;
		String source;
		String metric;
		List<Attribute> attributes;
		for (MetricTreeNode treeNode : treeNodes)
		{
			model = new MetricTreeNodeModel();
			model.setName(treeNode.getName());
			model.setValue(treeNode.getValue());
			model.setPrefix(treeNode.getPrefix());
			model.setPartialPath(treeNode.getPartialPath());
			model.setOpaqueNum(treeNode.getOpaqueNum());
			model.setOpaqueStr(treeNode.getOpaqueStr());
			model.setType(treeNode.getType());
			model.setCategory(treeNode.getCategory());
			model.setIsLeaf(treeNode.isLeaf());
			model.setIsWildcard(treeNode.isWildcard());
			model.setHasAnyWildcard(treeNode.hasAnyWildcard());

			Integer timeSeriesId = treeNode.getTimeSeriesId();
			if (timeSeriesId != null)
			{
				s_Logger.debug("Time series ID corresponding to " +
								treeNode.getPartialPath() + treeNode.getPrefix() +
								treeNode.getValue() + " is " +
								timeSeriesId);
				model.setTimeSeriesId(timeSeriesId);
			}

			// Only set these properties if non-null to reduce payload size.
			source = treeNode.getSource();
			metric = treeNode.getMetric();
			attributes = treeNode.getAttributes();
			
			if (source != null)
			{
				model.setSource(source);
			}
			if (metric != null)
			{
				model.setMetric(metric);
			}
			if (attributes != null)
			{
				model.setAttributes(attributes);
			}
			
			modelList.add(model);
		}
		
		// Order nodes using collation rules of request locale before returning.
		Locale requestLocale = getThreadLocalRequest().getLocale();
		Collections.sort(modelList, new MetricTreeNodeValueComparator(requestLocale));
		
		return new BaseListLoadResult<MetricTreeNodeModel>(modelList);
	}
	
	
	/**
     * Comparator which sorts DataSourceType objects in ascending alphabetical
     * order of data source type name.
     */
    class DataSourceTypeNameComparator implements Comparator<DataSourceType>
    {
    	
        public int compare(DataSourceType type1, DataSourceType type2)
        {
	        String name1 = type1.getName();
	        String name2 = type2.getName();
	        
	        int comp;
	        
	        if (name1.equals(name2) == false)
	        {
	        	comp = name1.compareTo(name2);
	        }
	        else
	        {
	        	// e.g. names are the same for TIME_SERIES and TIME_SERIES_FEATURE.
	        	DataSourceCategory cat1 = type1.getDataCategory();
	        	DataSourceCategory cat2 = type2.getDataCategory();
	        	
	        	comp = cat1.compareTo(cat2);
	        }
	        
	        return comp;
        }
    }


	/**
     * Comparator which sorts DataSourceModel objects in descending 
     * data point count order.
     */
    class ModelCountComparator implements Comparator<DataSourceTreeModel>
    {
        public int compare(DataSourceTreeModel model1, DataSourceTreeModel model2)
        {
	        Integer count1 = model1.getCount();
	        Integer count2 = model2.getCount();
	        
	        return count2.compareTo(count1);
        }	
    }
	

    /**
     * Comparator which sorts DataSourceModel objects by placing NOTIFICATION types
     * first, and then in ascending alphabetical order of data source type name.
     */
    class ModelTypeComparator implements Comparator<DataSourceTreeModel>
    {
        public int compare(DataSourceTreeModel model1, DataSourceTreeModel model2)
        {
        	int comparison = 0;
        	
        	DataSourceCategory cat1 = model1.getDataSourceCategory();
        	DataSourceCategory cat2 = model2.getDataSourceCategory();
        	
        	if (cat1.equals(cat2) == false)
        	{
        		if (cat1 == DataSourceCategory.NOTIFICATION)
        		{
        			comparison = -1;
        		}
        		else
        		{
        			comparison = 1;
        		}
        	}
        	else
        	{
        		String name1 = model1.getDataSourceName();
    	        String name2 = model2.getDataSourceName();
    	        
    	        comparison = name1.compareTo(name2);
        	} 
	        
	        return comparison;
        }
    }
    
    
    /**
     * Comparator which sorts MetricTreeNodeModel objects by placing leaf nodes first,
     * then wildcard nodes, and then by locale-sensitive String comparison of the node text values.
     */
    class MetricTreeNodeValueComparator implements Comparator<MetricTreeNodeModel>
    {
    	private Locale m_RequestLocale;
    	
    	public MetricTreeNodeValueComparator(Locale locale)
    	{
    		m_RequestLocale = locale;
    	}
    	

		@Override
		public int compare(MetricTreeNodeModel node1, MetricTreeNodeModel node2)
		{
			Boolean isLeaf1 = node1.isLeaf();
			Boolean isLeaf2 = node2.isLeaf();
			int compVal = isLeaf2.compareTo(isLeaf1);
			if (compVal == 0)
			{
				Boolean isWildcard1 = node1.isWildcard();
				Boolean isWildcard2 = node2.isWildcard();
				
				compVal = isWildcard2.compareTo(isWildcard1);
				
				if (compVal == 0)
				{
					String value1 = node1.getValue();
					String value2 = node2.getValue();
					
					if (value1 == null || value2 == null)
					{
						if (value1 == null && value2 == null)
						{
							compVal = 0;
						}
						else
						{
							compVal = (value1 == null) ? -1 : 1;
						}
					}
					else
					{
						Collator collator = Collator.getInstance(m_RequestLocale);
						compVal = collator.compare(value1, value2);
					}
				}
			}

			return compVal;
		}
    	
    }

}
