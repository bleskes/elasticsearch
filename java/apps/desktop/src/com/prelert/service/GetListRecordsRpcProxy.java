package com.prelert.service;

import java.util.ArrayList;

import com.extjs.gxt.ui.client.data.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.*;

public class GetListRecordsRpcProxy extends RpcProxy<PagingLoadConfig, ListViewLoadResult<EventRecord>>
{
	private ListViewQueryServiceAsync 		m_ListViewQueryService = null;
	
	private ListView			m_View;
	private String 				m_SelectRowFilter;
	private ArrayList<Object>	m_SelectRowFilterParams;
	private boolean m_InitialCallDone = false;


	public GetListRecordsRpcProxy(ListView view)
    { 
	    m_View = view;
	    m_ListViewQueryService = DatabaseServiceLocator.getInstance().getListViewQueryService();
    }
	
	
	public GetListRecordsRpcProxy(ListView view, String selectRowFilter, 
			ArrayList<Object> selectRowFilterParams)
    { 
	    this(view);
	    m_SelectRowFilter = selectRowFilter;
	    m_SelectRowFilterParams = selectRowFilterParams;   
    }


	public void load(PagingLoadConfig loadConfig, 
					AsyncCallback<ListViewLoadResult<EventRecord>> callback)
    {
		if (m_InitialCallDone == true)
		{
			loadRecords(loadConfig, callback);
		}
		else
		{
			if (m_SelectRowFilter != null)
			{
				loadRecordsWithRow(loadConfig, callback);
			}
			else
			{
				loadRecords(loadConfig, callback);
			}
			m_InitialCallDone = true;
		}
    }
	
	
	protected void loadRecords(PagingLoadConfig loadConfig, 
			AsyncCallback<ListViewLoadResult<EventRecord>> callback)
	{
		m_ListViewQueryService.getRecords(m_View, loadConfig, callback);
	}
	
	
	protected void loadRecordsWithRow(PagingLoadConfig loadConfig, 
			AsyncCallback<ListViewLoadResult<EventRecord>> callback)
	{
		m_ListViewQueryService.getRecordsWithRow(m_View, loadConfig.getLimit(), 
				m_SelectRowFilter, m_SelectRowFilterParams, callback);
	}


	public boolean isInitialCallDone()
    {
    	return m_InitialCallDone;
    }


	public void setInitialCallDone(boolean initialCallDone)
    {
    	m_InitialCallDone = initialCallDone;
    }
	

}
