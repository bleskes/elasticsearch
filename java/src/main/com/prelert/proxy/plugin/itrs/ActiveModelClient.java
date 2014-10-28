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

package com.prelert.proxy.plugin.itrs;

import static com.itrsgroup.activemodel.core.ActiveModelRepositoryHolder.getRepository;
import static com.itrsgroup.activemodel.printer.ActiveModelPrinter.print;

import java.util.Date;
import java.util.Properties;

import com.itrsgroup.activemodel.dataview.ActiveDataView;
import com.itrsgroup.activemodel.dataview.ActiveDataViewEvent;
import com.itrsgroup.activemodel.dataview.ActiveDataViewListener;
import com.itrsgroup.activemodel.dataview.ActiveDataViewModel;
import com.itrsgroup.activemodel.list.ActiveList;
import com.itrsgroup.activemodel.list.ActiveListEvent;
import com.itrsgroup.activemodel.list.ActiveListItem;
import com.itrsgroup.activemodel.list.ActiveListListener;
import com.itrsgroup.activemodel.list.ActiveListModel;
import com.itrsgroup.activemodel.statetree.ActiveStateTree;
import com.itrsgroup.activemodel.statetree.ActiveStateTreeEvent;
import com.itrsgroup.activemodel.statetree.ActiveStateTreeListener;
import com.itrsgroup.activemodel.statetree.ActiveStateTreeModel;

import com.itrsgroup.activemodel.chart.ActiveChartDataModel;
import com.itrsgroup.activemodel.chart.ActiveChartDataSource;
import com.itrsgroup.activemodel.chart.ActiveChartDataSourceEvent;
import com.itrsgroup.activemodel.chart.ActiveChartDataSourceListener;
import com.itrsgroup.activemodel.client.ActiveModelClientException;
import com.itrsgroup.activemodel.client.ActiveModelClientUtil;
import com.itrsgroup.activemodel.client.core.UserDetailsImpl;



public class ActiveModelClient
{
	public static void main(final String[] args) throws ActiveModelClientException
	{
		//String host = "192.168.62.233"; // vm-win2008r2-64-1
		String host = "vm-centos-60-64-1"; // geneosopenaccess
		int port = 8001;

	    ActiveModelClientUtil.init(host, port, new UserDetailsImpl("user", "ROLE_USER"), "password");
	    

		//String example = System.getProperty("example");
	    //String example = "list";
	    String example = "dataview";

		if( "dataview".equals(example) )
		{
			dataViewExample();
		}
		else if( "list".equals(example) )
		{
			listExample();
		}
		else if( "tree".equals(example) )
		{
			treeExample();
		}
		else if ("chart".equals(example))
		{
			chartExample();
		}
		else
		{
			System.err.println("Please specify an example to run: -Dexample=dataview|list|tree");
			System.exit(1);
		}
	}
	
	
	static Properties createProps(String url)
	{
	    Properties props = new Properties();
	    props.put("java.naming.factory.initial", "com.kaazing.gateway.jms.client.stomp.StompInitialContextFactory");
	    props.put("java.naming.provider.url", url);
	    return props;
	}
	

	static void dataViewExample()
	{
		ActiveDataViewModel activeDataViewModel = getRepository().getActiveDataViewModel();
		ActiveDataView activeDataView = activeDataViewModel.create();
		
		String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/"
			+ "probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/"
			+ "sampler[(@name='cpu')][(@type='Basic')]/dataview[(@name='cpu')]";		
		
		
		//String path = "/geneos/gateway[(@name=\"Demo Gateway\")]/directory/probe[(@name=\"Basic Probe\")]/managedEntity[(@name=\"Basic Entity\")]";
		//String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Processes Probe')]/managedEntity[(@name='Processes Entity')]";
		//String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Gateway Monitor Probe')]/managedEntity[(@name='Gateway Monitor Entity')]";
		
		//String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Test Probe')]/managedEntity[(@name='Test Entity')]";
		
		//String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/sampler[(@name='cpu')][(@type='Basic')]";
		// Elliot's example
	    //String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Processes Probe')]/managedEntity[(@name='Processes Entity')]/sampler[(@name='processes')][(@type='Processes')]/dataview[(@name='processes')]";
		
		//String path = "/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/sampler[(@name='cpu')][(@type='Basic')]/dataview[(@name='cpu')]";
		
		activeDataView.setPath(path);
		
		activeDataView.addListener(new ActiveDataViewListener()
		{
			@Override
			public void onActiveDataViewEvent(final ActiveDataViewEvent paramEvent)
			{
				//System.out.println(new Date().getSeconds());
				System.out.println(print(paramEvent.getDataView()));
			}
		});
		
		

		activeDataViewModel.register(activeDataView);
	}

	static void listExample()
	{
		ActiveListModel activeListModel = getRepository().getActiveListModel();
		ActiveList activeList = activeListModel.create();

		
		//activeList.setPath("//managedEntity[(@name='Basic Entity')]//dataview");
		//activeList.setPath("//dataview");
		//activeList.setPath("//sampler");
		//activeList.setPath("//probe");
		//activeList.setPath("//dataview");		
		//activeList.setPath("//managedEntity");
		activeList.setPath("//managedEntity[(@name=\"BPD Entity\"//dataview");
		
		//activeList.setPath("/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/sampler[(@name='cpu')][(@type='Basic')]/dataview");

		
		//activeList.setPath("/geneos/gateway[(@name='Demo Gateway')]/directory/probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/sampler[(@name='cpu')][(@type='Basic')]");
		
		
		activeList.addListener(new ActiveListListener()
		{
			@Override
			public void onActiveListEvent(final ActiveListEvent event)
			{
				System.out.println(print(event.getList()));
				for (ActiveListItem item : event.getList().getContent().getItems() )
				{
					System.out.println(item.getPath());
				}
			}
		});

		activeListModel.register(activeList);
	}
	

	static void treeExample()
	{
		ActiveStateTreeModel activeStateTreeModel = getRepository().getActiveStateTreeModel();
		ActiveStateTree activeTree= activeStateTreeModel.create();
		activeTree.setPath("//managedEntity");

		activeTree.addListener(new ActiveStateTreeListener()
		{
			@SuppressWarnings("deprecation")
			@Override
			public void onActiveStateTreeEvent(final ActiveStateTreeEvent event)
			{
				//System.out.println(print(event.getTree()));
				
				System.out.println(new Date().getSeconds());
			}
		});

		activeStateTreeModel.register(activeTree);
	}
	
	
	static void chartExample()
	{
		ActiveChartDataModel model = getRepository().getActiveChartDataModel();
		ActiveChartDataSource source = model.create();
		//source.setPath("//managedEntity");
		
		source.setPath("/geneos/gateway[(@name='Demo Gateway')]/directory/"
		+ "probe[(@name='Basic Probe')]/managedEntity[(@name='Basic Entity')]/"
		+ "sampler[(@name='cpu')][(@type='Basic')]/" + "dataview[(@name='cpu')]");

		source.addActiveChartDataSourceListener(new ActiveChartDataSourceListener()
		{
			@Override
			public void onActiveChartDataSourceEvent(final ActiveChartDataSourceEvent event)
			{
				System.out.println(event.getEventSource());
			}
		});

		model.register(source);
		
	}
	
	/***
	static void pathExample()
	{
		ActivePathModel activePathModel = getRepository().getActivePathModel();
		ActivePath activePath = activePathModel.createActivePath();
		activePath.addPaths(new String[] {"//managedEntity"});

		activeList.addListener(new ActiveListListener()
		{
			@Override
			@SuppressWarnings("PMD.SystemPrintln")
			public void onActiveListEvent(final ActiveListEvent event)
			{
				System.out.println(print(event.getList()));
			}
		});

		activeListModel.register(activeList);
	}
	**/
}
