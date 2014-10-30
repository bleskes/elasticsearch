/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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
 ************************************************************/

package com.prelert.api.test.consumer;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.core4j.Enumerable;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OObject;
import org.odata4j.edm.EdmDataServices;

/**
 * This test is designed to stress test the query API by continually making 
 * query requests. It spawns {@link #NUM_THREADS} threads, creates an ODataConsumer 
 * for each then runs a fixed set of 15 queries in each of those threads. The  
 * threads execute as fast as possible and don't ever sleep. 
 * <p/>
 * The <code>main</code> method will run indefinitely and can only be 
 * stopped by <code>Ctrl+c</code> or killing it from a terminal. 
 * <p/>
 * The only parameter to the program is the URl of the ODATA web service, other 
 * options are defined as system properties using the '-D' syntax.<br/>
 * <b>Options:</b>
 * <ul>
 * <li>NumThreads - The number of threads to simultaneously run the queries in. Defaults to {@link #NUM_THREADS}</li> 
 * <li>QueryWindowSize - The number of hours to query for. All queries run from the time now to QueryWindowSize
 * hours ago. If <= 0 then no datetime filter is added to the queries so all results will be returned.
 * Default = {@link #QUERY_WINDOW_SIZE_HOURS}</li>
 * </ul>
 * Example:
 * 	java -cp './*' -DNumThreads=10 -DQueryWindowSize=-1 com.prelert.api.test.consumer.SaturationTest
 * <p/>
 * This test requires <a href=https://code.google.com/p/odata4j/>Odata4J</a> and 
 * <a href=http://logging.apache.org/log4j/1.2/>Apache Log4j</a> (Apache Licence 2.0)
 */ 
public class SaturationTest 
{
	private final static Logger s_Logger = Logger.getLogger(SaturationTest.class);
    /**
     * The Activities Entity Set name
     */
    public final static String ACTIVITIES_SET = "Activities";
    
    /**
     * The MetricConfig Entity set name.
     */
	public final static String METRIC_CONFIG_SET = "MetricConfigs";
	
    /**
     * Earliest Activity time function name
     */
    public final static String EARLIEST_ACTIVITY_FUNC = "EarliestActivityTime"; 
    
    /**
     * Latest Activity time function name
     */
    public final static String LATEST_ACTIVITY_FUNC = "LatestActivityTime"; 
	
    
    /**
     * The number of thread running the queries against the OData 
     * web service.
     */
    public final static int NUM_THREADS = 15; 
    
    /**
     * The length of the queries. All queries run from the time now to QUERY_WINDOW_SIZE_HOURS
     * hours ago.
     */
    public final static int QUERY_WINDOW_SIZE_HOURS = 4;
    
    
    /**
     * Main entry point. Creates the ODataConsumers and threads then 
     * starts the queries running.
     * 
     * @param args 1 optional argument is expected which is the service URI.
     * If not set the default <code>http://localhost:8080/prelertApi/prelert.svc</code> is used.
     */ 
    public static void main(String[] args)
    {
    	
        String serviceUri = "http://localhost:8080/prelertApi/prelert.svc";
        if (args.length > 0)
        {
            serviceUri = args[0];
        }
        
        
        int numThreads = NUM_THREADS;
		if (System.getProperties().containsKey("NumThreads"))
		{
			numThreads = Integer.parseInt(System.getProperty("NumThreads"));
		}

		int windowSize = QUERY_WINDOW_SIZE_HOURS;
		if (System.getProperties().containsKey("QueryWindowSize"))
		{
			windowSize = Integer.parseInt(System.getProperty("QueryWindowSize"));
		}
        
        
        s_Logger.info(String.format("Running Prelert API Saturation test on %d threads with query window of %d hours.", 
        		numThreads, windowSize));
        s_Logger.info("Using host " + serviceUri);
        
        List<ApiQuery> queriers = new ArrayList<ApiQuery>();

        for (int i=0; i<numThreads; i++)
		{
        	ODataConsumer oDataConsumer = ODataConsumers.newBuilder(serviceUri).build();
        	queriers.add(new ApiQuery(oDataConsumer, windowSize));
		}
       
        
        List<Thread> threads = new ArrayList<Thread>();
        for (int i=0; i<numThreads; i++)
        {
        	Thread thread = new Thread(queriers.get(i), "Query-" + i);
        	threads.add(thread);
        	thread.start();
        }
    }

    /**
     * Simple <code>Runnable</code> object which runs the API queries in a loop.
     * <br/>
     * <b>Queries:</b>
     * <ol>
     * <li>http://host:port/prelertApi/prelert.svc/$metadata</li>
     * <li>http://host:port/prelertApi/prelert.svc/MetricConfigs(1)</li>
     * <li>http://host:port/prelertApi/prelert.svc/Activities/$count</li>
	 * <li>http://host:port/prelertApi/prelert.svc/LatestActivityTime</li>
	 * <li>http://host:port/prelertApi/prelert.svc/EarliestActivityTime</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore+desc</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=PeakEvidenceTime</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=PeakEvidenceTime+desc</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=UpdateTime</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=UpdateTime+desc</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$top=5&$expand=ActivityMetrics</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore+desc&$filter=(MetricPath+eq+'SuperDomain%7CCustom+Metric+Host+(Virtual)%7CCustom+Metric+Process+(Virtual)%7CCustom+Metric+Agent+(Virtual)%7CEnterprise+Manager%7CInternal%7CThreads%7CTimer-:User+Time+(ms)'+and+MetricPath+eq+'SuperDomain%7CCustom+Metric+Host+(Virtual)%7CCustom+Metric+Process+(Virtual)%7CCustom+Metric+Agent+(Virtual)%7CEnterprise+Manager%7CInternal%7CThreads%7CTimer-:CPU+Time+(ms)')&$expand=ActivityMetrics</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore+desc&$filter=(MetricPath+eq+'SuperDomain%7CCustom+Metric+Host+(Virtual)%7CCustom+Metric+Process+(Virtual)%7CCustom+Metric+Agent+(Virtual)%7CEnterprise+Manager%7CData+Store%7CTransactions:Number+of+Traces+in+Insert+Queue'+or+MetricPath+eq+'SuperDomain%7CCustom+Metric+Host+(Virtual)%7CCustom+Metric+Process+(Virtual)%7CCustom+Metric+Agent+(Virtual)%7CEnterprise+Manager%7CData+Store%7CTransactions:Number+of+Traces+in+Insert+Queue')&$expand=ActivityMetrics</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore+desc&$filter=(MPQuery+eq+'%25:User+Time+(ms)'+and+MPQuery+eq+'%25:CPU+Time+(ms)')&$expand=ActivityMetrics</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore+desc&$filter=(MPQuery+eq+'%25:Number+of+Traces+in+Insert+Queue'+or+MPQuery+eq+'%25:Number+of+Traces+in+Insert+Queue')&$expand=ActivityMetrics</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$orderby=AnomalyScore+desc&$expand=ActivityMetrics</li>
	 * <li>http://host:port/prelertApi/prelert.svc/Activities?$filter=(MPQuery+eq+'%25(^%25)%25')+and+EscapeChar+eq+'^'&$expand=ActivityMetrics</li>
	 * </ul> 
     */
    static public class ApiQuery implements Runnable
    {
    	private ODataConsumer m_OdataConsumer;
    	private int m_WindowSize;

    	public ApiQuery(ODataConsumer consumer, int windowSize)
    	{
    		m_OdataConsumer = consumer;
    		m_WindowSize = windowSize;
    	}
    	
		@SuppressWarnings("unused")
		@Override
		public void run() 
		{
			while (true)
			{
	    		s_Logger.debug("New Query");
	    		
				try 
				{
					String now = ISODateTimeFormat.dateTime().print(new DateTime());
					String start = ISODateTimeFormat.dateTime().print(new DateTime().minusHours(m_WindowSize));
					
					String dateFilter = String.format("(FirstEvidenceTime le datetimeoffset'%s') and " +
							"(LastEvidenceTime ge datetimeoffset'%s')", now, start);
					

					EdmDataServices meta = m_OdataConsumer.getMetadata();
					
					OEntity ent = m_OdataConsumer.getEntity(METRIC_CONFIG_SET, OEntityKey.parse("1")).execute();
					int count = m_OdataConsumer.getEntitiesCount(ACTIVITIES_SET).execute();
				    Enumerable<OObject> result = m_OdataConsumer.callFunction(LATEST_ACTIVITY_FUNC).execute();
				    result = m_OdataConsumer.callFunction(EARLIEST_ACTIVITY_FUNC).execute();

				       
				    Enumerable<OEntity> ents = null;
				    
				    if (m_WindowSize > 0)
				    {
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).filter(dateFilter).orderBy("AnomalyScore") .execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").filter(dateFilter).execute();
				    	s_Logger.debug("Got " + ents.count() + " activities");
				    	
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("PeakEvidenceTime").filter(dateFilter).execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("PeakEvidenceTime desc").filter(dateFilter).execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("UpdateTime").filter(dateFilter).execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("UpdateTime desc").filter(dateFilter).execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).filter(dateFilter).top(5).expand("ActivityMetrics").execute();
				    }
				    else
				    {
				    	// don't use the date filter
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore") .execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").execute();
				    	s_Logger.debug("Got " + ents.count() + " activities");
				    	
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("PeakEvidenceTime").execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("PeakEvidenceTime desc").execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("UpdateTime").execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("UpdateTime desc").execute();
				    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).top(5).expand("ActivityMetrics").execute();
				    }
				    
				    String andDateFilter = dateFilter + " and ";
				    if (m_WindowSize <= 0)
				    {
				    	andDateFilter = "";
				    }

				    String filter = "(MetricPath eq 'SuperDomain%7CCustom Metric Host (Virtual)%7CCustom Metric Process (Virtual)%7CCustom Metric Agent (Virtual)%7CEnterprise Manager%7CInternal%7CThreads%7CTimer-:User Time (ms)' and MetricPath eq 'SuperDomain%7CCustom Metric Host (Virtual)%7CCustom Metric Process (Virtual)%7CCustom Metric Agent (Virtual)%7CEnterprise Manager%7CInternal%7CThreads%7CTimer-:CPU Time (ms)')";
				    ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").filter(andDateFilter + filter).expand("ActivityMetrics").execute();

				    String filter2 = "(MetricPath eq 'SuperDomain%7CCustom Metric Host (Virtual)%7CCustom Metric Process (Virtual)%7CCustom Metric Agent (Virtual)%7CEnterprise Manager%7CData Store%7CTransactions:Number of Traces in Insert Queue' or MetricPath eq 'SuperDomain%7CCustom Metric Host (Virtual)%7CCustom Metric Process (Virtual)%7CCustom Metric Agent (Virtual)%7CEnterprise Manager%7CData Store%7CTransactions:Number of Traces in Insert Queue')";
			    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").filter(andDateFilter + filter2).expand("ActivityMetrics").execute();

			    	String filter3 = "(MPQuery eq '%25:User Time (ms)' and MPQuery eq '%25:CPU Time (ms)')";
			    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").filter(andDateFilter + filter3).expand("ActivityMetrics").execute();

			    	String filter4 = "(MPQuery eq '%25:Number of Traces in Insert Queue' or MPQuery eq '%25:Number of Traces in Insert Queue')";
			    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").filter(andDateFilter + filter4).expand("ActivityMetrics").execute();
			    	
			    	String filter5 = "(MPQuery eq '%25(^%25)%25') and EscapeChar eq '^'";
			    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore desc").filter(andDateFilter + filter5).expand("ActivityMetrics").execute();
			    	
			    	ents = m_OdataConsumer.getEntities(ACTIVITIES_SET).filter(dateFilter).orderBy("AnomalyScore desc").expand("ActivityMetrics").execute();
				}
				catch (Exception e)
				{
					s_Logger.error(e);
				}
			}
		}
    }
}
