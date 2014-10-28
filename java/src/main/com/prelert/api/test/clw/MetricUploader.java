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

package com.prelert.api.test.clw;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.format.FormatType;

import com.wily.introscope.clws.ISessionHandle;
import com.wily.introscope.clws.protocol.LogonCredentials;
import com.wily.introscope.spec.server.beans.clw.ICommandLineWorkstationService;
import com.wily.isengard.IsengardException;
import com.wily.isengard.messageprimitives.service.MessageServiceFactory;
import com.wily.isengard.messageprimitives.service.ServiceException;
import com.wily.util.feedback.ApplicationFeedback;
import com.wily.introscope.clws.CLWClientService;
import com.wily.introscope.spec.server.beans.clw.CommandException;
import com.wily.introscope.spec.server.beans.clw.CommandNotification;
import com.wily.introscope.spec.server.beans.session.IllegalSessionException;
import com.wily.isengard.messageprimitives.service.IllegalServiceAccessException;


/**
 * Query data from CA APM via the Command Line Workstation (CLW) in 
 * one thread and in a second thread either upload the metric data 
 * to the Prelert ODATA API or convert to json, gzip and write to file. 
 * </p>
 * The main method has 1 parameter which is the Service URI of the 
 * Prelert API web service this defaults to <b>http://localhost:8080/prelertApi/prelert.svc</b>.
 * Other options are defined as system properties using the '-D' syntax.<br/>
 * <b>Options:</b>
 * <ul>
 * <li>EmHost - The Enterprise Manager host. Default = localhost</li> 
 * <li>agent - The CA APM agent query as used in the CLW this can be an regular expression. Default = .*</li> 
 * <li>metric - The CA APM metric query as used in the CLW this can be an regular expression. Default = .*</li>
 * <li>from - Queries start time in <code>yyyy-MM-dd HH:mm:ss</code> format. Defaults to the time now - (queryLength + queryDelay)</li>
 * <li>to - Queries end time in <code>yyyy-MM-dd HH:mm:ss</code> format.</li>
 * <li>sleep - The time to sleep in ms between queries. Default = 2000.</li>
 * <li>queryLength - The length of the queries to make in seconds. Default = 60.</li>
 * <li>queryDelay - When running in real-time add a delay so that queries don't try to pull data for the exact
 * time now. This to allow the Enterprise Manager time to process the data and make it available to the CLW. Default = 30 seconds.</li>
 * <li>file - If set then write the output (Json, gzipped) to file instead of sending it to the web service.</li>
 * </ul>
 * If <code>from</code> isn't set then <code>from</code> defaults to the time <code>now - queryLength</code>. 
 * In this case the queries will run in real-time updating every <code>queryLength</code> seconds
 * and terminating when time <code>to</code> is reached or never if <code>to</code> is not set. <br/>
 * If both <code>from</code> and <code>to</code> are set then query all the data in a loop between these 2 dates
 * in chunks of size <code>queryLength</code> pausing for <code>sleep</code> seconds after every query.
 * <p/>
 * e.g. <i>Query between 2 times</i>:<br/>
 * java -cp './*' -DEmHost=vm-win2003r2-32-1 -Dagent=.* -Dmetric=.* -Dfrom='2013-03-07 09:00:00' -Dto='2013-03-08 09:00:00' com.prelert.api.test.clw.MetricUploader
 * <br/>
 * <i>Query data in real time from now until the time <code>to</code> updating every 60 seconds using the default service URI</i>:<br/>
 * java -cp './*' -DEmHost=vm-win2003r2-32-1 -Dto='2013-03-08 09:00:00' com.prelert.api.test.clw.MetricUploader</br>
 * <i>Query data in real time from now continuously until the program is stopped using specified URI for the web service.:</i><br/>
 * java -cp './*' -DEmHost=vm-win2003r2-32-1 com.prelert.api.test.clw.MetricUploader http://host:8080/prelertApi/prelert.svc</br>
 * 
 * <p/>
 * This test requires these Jar files
 * <ul>
 * <li><a href=https://code.google.com/p/odata4j/>Odata4J</a> (Apache Licence 2.0)</li>
 * <li>CLWorkstation.jar</li>
 * <li><a href=https://code.google.com/p/json-simple/>Json-Simple</a> (Apache Licence 2.0)</li>
 * <li><a href=http://logging.apache.org/log4j/1.2/>Apache Log4j</a> (Apache Licence 2.0)</li>
 * <li><a href=http://supercsv.sourceforge.net/>Super CSV</a> (Apache Licence 2.0)</li>
 * </ul>
 * 
 */
public class MetricUploader 
{
	/**
	 * The MetricFeed Entity set name
	 */
	public final static String METRIC_FEEDS_SET = "MetricFeeds";
	
	/**
	 * The CLW query string.
	 */
	public static final String HISTORICAL_DATA_CMD = "get historical data from agents matching %1$s " +
		"and metrics matching %2$s between %3$tY/%3$tm/%3$td %3$tH:%3$tM:%3$tS and " +
		"%4$tY/%4$tm/%4$td %4$tH:%4$tM:%4$tS with frequency of %5$d seconds";
	
	/**
	 * The interval between uploads of the metric data. This is set to 15 seconds
	 * to mirror the Enterprise Managers update interval.
	 */
	public static final long METRIC_UPLOAD_INTERVAL = 15000;
	
	/**
	 * The character set the Json data is encoded in. 
	 * The web service expects data in this encoding
	 */
	public static final String JSON_DATA_CHARSET = "UTF-8";
	
	
	private final static Logger s_Logger = Logger.getLogger(MetricUploader.class);
	
	private ISessionHandle m_EnterpriseManager;
	private ICommandLineWorkstationService m_ClwService;
	
	// Synchronisation object.
	private Queue<MetricData> m_MetricDataQueue = new LinkedList<MetricData>();
	
	volatile private boolean m_IsFinished = false;
	
	
	/**
     * Main entry point, runs the configuration tests.
     * 
     * @param args 1 optional argument is expected which is the service URI.
     * If not set the default <code>http://localhost:8080/prelertApi/prelert.svc</code> is used.
     * 
     * @throws ParseException
	 * @throws IOException 
	 */
    public static void main(String[] args) throws ParseException, IOException 
    {
		String serviceUri = "http://localhost:8080/prelertApi/prelert.svc";
		if (args.length > 0)
		{
			serviceUri = args[0];
		}
		
		String EmHost = "localhost";
		if (System.getProperties().containsKey("EmHost"))
		{
			EmHost = System.getProperty("EmHost");
		}
		int EmPort = 5001;
		String EmUser = "admin";
		String EmPassword = "";
		
		s_Logger.info(String.format("Using Enterprise Manager %s@%s:%d", EmUser, EmHost, EmPort));
	

    	LogonCredentials logon = new LogonCredentials();
    	logon.setHostName(EmHost);
    	logon.setPort(new Integer(EmPort).toString());
    	logon.setUserName(EmUser);
    	logon.setPassword(EmPassword);
    	
    	MetricUploader metricUploader = new MetricUploader();
    	if (metricUploader.connect(logon) == false)
    	{
    		s_Logger.error("Could not connect to EM " + logon);
    		return;
    	}

    	
    	String agentRegex = ".*";
		if (System.getProperties().containsKey("agent"))
		{
			agentRegex = System.getProperty("agent");
		}
		
		String metricRegex = ".*";
		if (System.getProperties().containsKey("metric"))
		{
			metricRegex = System.getProperty("metric");
		}
		
		long sleepTime = 2000;
		if (System.getProperties().containsKey("sleep"))
		{
			sleepTime = Long.parseLong(System.getProperty("sleep"));
		}
		
		long queryLength = 60;
		if (System.getProperties().containsKey("queryLength"))
		{
			queryLength = Long.parseLong(System.getProperty("queryLength"));
		}
		
		long queryDelay = 30;
		if (System.getProperties().containsKey("queryDelay"))
		{
			queryDelay = Long.parseLong(System.getProperty("queryDelay"));
		}
		
		
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	
    	boolean realtime = true;
    	Date start = new Date(new Date().getTime() - (queryLength + queryDelay) * 1000); // time now - query0length. 
    	if (System.getProperties().containsKey("from"))
		{
    		realtime = false;
			start = dateFormat.parse(System.getProperty("from"));
		}
		
		
		Date stop = dateFormat.parse("2020-01-01 00:00:00"); // some time a long way away
		if (System.getProperties().containsKey("to"))
		{
			stop = dateFormat.parse(System.getProperty("to"));
		}
		
		
		
		// Write to file or upload to OData consumer
		MetricConsumer consumer = null;
		if (System.getProperties().containsKey("file"))
		{
			String filename = System.getProperty("file");
			consumer = metricUploader.new MetricConsumer(new File(filename));
			s_Logger.info("Writing data to file '" + filename + "'");
		}
		else
		{
			ODataConsumer oDataConsumer = ODataConsumers.newBuilder(serviceUri).setFormatType(FormatType.JSON).build();
			consumer = metricUploader.new MetricConsumer(oDataConsumer);
			s_Logger.info("Using API on URI " + serviceUri);
		}
		Thread consumerThread = new Thread(consumer, "Consumer");
		consumerThread.start();
		
		
		s_Logger.info(String.format("Starting CLW queries agent=%s, metric=%s from %s to %s",
				agentRegex, metricRegex, start, stop));
    	
    	while (start.before(stop))
    	{
    		long timerStart = System.currentTimeMillis();
    		
    		Date queryEnd = new Date(start.getTime() + queryLength * 1000);
    		
    		List<MetricData> md = metricUploader.getMetricData(agentRegex, metricRegex, start, queryEnd);
    		synchronized (metricUploader.m_MetricDataQueue) 
    		{
    			metricUploader.m_MetricDataQueue.addAll(md);
    		}
    		
    		start = queryEnd;

    		long timerDuration = System.currentTimeMillis() - timerStart;
    		long sleepIntervalms = sleepTime;
    		if (realtime)
    		{
    			sleepIntervalms = (queryLength * 1000) - timerDuration;
    			if (sleepIntervalms < 0)
    			{
    				sleepIntervalms = 0;
    			}
    		}
    		
    		try 
    		{
				Thread.sleep(sleepIntervalms);
			}
    		catch (InterruptedException e) 
    		{
				e.printStackTrace();
				break;
			}
    	}
    	
    	
    	metricUploader.m_IsFinished = true;
    	
    	s_Logger.info("TEST COMPLETE");
    }
    
    
    /**
	 * Connect to the Enterprise Manager.
	 */
	private boolean connect(LogonCredentials logon)
	{
		ApplicationFeedback feedback = new ApplicationFeedback("CLW");
		feedback.setShouldBuffer(false);
		
		try
		{
			m_EnterpriseManager = logon.logon(feedback);
		}
		catch (IsengardException e)
		{
			s_Logger.error("Could not connect to an Enterprise Manager with connection params " +
					logon);
			s_Logger.error(e);
			return false;
		}

		try 
		{
			 m_ClwService = (ICommandLineWorkstationService)MessageServiceFactory.getService(
					 											m_EnterpriseManager.getPostOffice(), 
																ICommandLineWorkstationService.class);
		} 
		catch (com.wily.isengard.messageprimitives.ConnectionException e) 
		{
			s_Logger.error("Could not connect the Command Line Workstation Service: " + e);
			return false;
		}
		catch (ServiceException e) 
		{
			s_Logger.error("Could not get the Command Line Workstation Service: " + e);
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Query for metric data through the CLWorkstation.
	 * 
	 * @param agentRegex
	 * @param metricRegex
	 * @param start
	 * @param stop
	 * @return A list of metric data
	 */
	public List<MetricData> getMetricData(String agentRegex,
			String metricRegex, Date start, Date stop) 
	{
		String cmd = String.format(HISTORICAL_DATA_CMD, agentRegex, metricRegex, start, stop, 15);
		
		s_Logger.debug("getMetricData cmd = " + cmd);
		
		Date time1 = new Date();

		CLWClientService clwClientService = new CLWClientService(m_EnterpriseManager.getPostOffice());
		MetricDataCallback callback = new MetricDataCallback();
		CommandNotification commandNotification = new CommandNotification(m_EnterpriseManager.getPostOffice(),
				callback, clwClientService);  

		try 
		{
			m_ClwService.executeAsyncCommand(m_EnterpriseManager.getSessionToken(), 
					new String[]{cmd}, 
					commandNotification, 
					new Properties());					

			callback.waitForFinish();
			
			Date time2 = new Date();

			clwClientService.close();
			commandNotification.close();		

			List<MetricData> results = callback.getQueryResults();
			
			Date time3 = new Date();
			
			long time3_1 = time3.getTime() - time1.getTime();
			long time2_1 = time2.getTime() - time1.getTime();
			long time3_2 = time3.getTime() - time2.getTime();

			int metricCount = (results.size() == 0) ? 0 : results.get(0).getPoints().size();
					
			String timings = "getMetricData = %s metrics queried in %s ms, processed in %s ms. %s ms in total.";
			s_Logger.info(String.format(timings, metricCount, time2_1, time3_2, time3_1));
			
			Collections.sort(results);
			return results;
		}
		catch (com.wily.isengard.messageprimitives.ConnectionException e) 
		{
			s_Logger.error("getMetricData ConnectionException: " + e);
			return Collections.emptyList();
		}
		catch (CommandException e) 
		{
			s_Logger.error("getMetricData CommandException: " + e);
			return Collections.emptyList();
		}
		catch (IllegalServiceAccessException e) 
		{
			s_Logger.error("getMetricData IllegalServiceAccessException: " + e);
			return Collections.emptyList();
		} 
		catch (IllegalSessionException e) 
		{
			s_Logger.error("getMetricData IllegalSessionException: " + e);
			return Collections.emptyList();
		}
	}
		
	/**
	 * Return true if the CLW queries have finished.
	 * @return true if the CLW queries have finished
	 */
	public boolean isFinished()
	{
		return m_IsFinished;
	}
	
	
	/**
	 * Runnable object reads metric data from the shared queue object
	 * and either uploads it to the ODATA service or writes it
	 * out to a gzipped file.
	 */
	public class MetricConsumer implements Runnable 
	{
		private ODataConsumer m_ODataConsumer;
		private FileOutputStream m_OutputStream;
		private GZIPOutputStream m_GzipOutStream;
		
		private long m_Id = 0;
		
		
		/**
		 * If this constructor is used the data will be uploaded 
		 * to the ODataConsumer.
		 * @param consumer
		 */
		public MetricConsumer(ODataConsumer consumer)
		{
			m_ODataConsumer = consumer;
		}
		
		/**
		 * If this constructor is used the data will be written 
		 * to <code>file</code>.
		 * @param file
		 * @throws IOException 
		 */
		public MetricConsumer(File file) throws IOException
		{
			m_OutputStream = new FileOutputStream(file);
			m_GzipOutStream = new GZIPOutputStream(m_OutputStream);
		}

		@Override
		public void run() 
		{
			s_Logger.info("Starting data consumer");


			long sleepTime = METRIC_UPLOAD_INTERVAL;
			while (isFinished() == false)
			{
				try 
				{
					if (sleepTime < 0)
					{
						sleepTime = 0;
					}
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					s_Logger.info("Consumer thread interrupted...exiting.");
					break;
				}
				
				
				MetricData head = null;
				while (head == null && isFinished() != true)
				{
					synchronized (m_MetricDataQueue) 
					{
						head = m_MetricDataQueue.peek();
					}

					try 
					{
						Thread.sleep(100);
					}
					catch (InterruptedException e)
					{
						s_Logger.info("Consumer thread interrupted 2...exiting.");
						break;
					}
				}
				
				if (isFinished() && m_GzipOutStream != null)
				{
					try 
					{
						m_GzipOutStream.close();
					}
					catch (IOException e) 
					{
						e.printStackTrace();
					}
					return;
				}
				
				long uploadStart = System.currentTimeMillis();
				
				MetricData md = null;
	    		synchronized (m_MetricDataQueue) 
	    		{
	    			md = m_MetricDataQueue.remove();
	    			
	    			if (m_ODataConsumer != null)
	    			{
	    				uploadData(m_ODataConsumer, md);
	    			}
	    			else if (m_GzipOutStream != null)
	    			{
	    				try 
	    				{
							writeData(md);
						}
	    				catch (IOException e) 
	    				{
							e.printStackTrace();
							break;
						}
	    			}
	    			else
	    			{
	    				s_Logger.error("Both ODataConsumer and Output stream are null");
	    			}
	    		}
	    		
				
				long uploadDuration = System.currentTimeMillis() - uploadStart;
				
				sleepTime = METRIC_UPLOAD_INTERVAL - uploadDuration;
			}
			
		}
		
		/**
		 * Upload the data to the ODATA Metric Upload API
		 */
		@SuppressWarnings("unchecked")
		public void uploadData(ODataConsumer oDataConsumer, MetricData md)
		{
			Set<String> paths = new HashSet<String>();
			
			JSONArray jsonArrayIn = new JSONArray();

			for (MetricData.Point pt : md.getPoints())
			{
				String path = pt.getPath();
				if (paths.contains(path))
				{
					s_Logger.debug("Skipping duplicate metric path " + path);
					continue;
				}
				paths.add(path);
				
				JSONObject obj = new JSONObject();
				obj.put("m", pt.getPath());
				obj.put("d", pt.getValue());
				jsonArrayIn.add(obj);	
			}
			byte [] data = gzipCompress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET)));


			int id = 1;
			List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
			dataProps.add(OProperties.int32("Id", id));
			dataProps.add(OProperties.string("Source", "CA-APM"));
			dataProps.add(OProperties.datetimeOffset("CollectionTime", 
					new DateTime(md.getCollectionTime())));
			dataProps.add(OProperties.int32("Count", md.getPoints().size()));
			dataProps.add(OProperties.string("Compression", "gzip"));
			dataProps.add(OProperties.binary("Data", data));

			
			long uploadStart = System.currentTimeMillis();
			
			OEntity createdEnt = oDataConsumer.createEntity(METRIC_FEEDS_SET)
				.properties(dataProps).execute();
			
			long uploadDuration = System.currentTimeMillis() - uploadStart;
			s_Logger.info(String.format("Uploaded %d metrics in %d ms", md.getPoints().size(), uploadDuration));

			if (createdEnt == null)
			{
				s_Logger.error("Create entity MetricFeed failed");
			}
		}
		
		
		/**
		 * Convert MetricData to Json, gzip then write to the 
		 * output file.
		 * @param md
		 * @throws IOException
		 */
		@SuppressWarnings("unchecked")
		public void writeData(MetricData md) throws IOException
		{
			JSONArray jsonArrayIn = new JSONArray();

			for (MetricData.Point pt : md.getPoints())
			{
				JSONObject obj = new JSONObject();
				obj.put("m", pt.getPath());
				obj.put("d", pt.getValue());
				jsonArrayIn.add(obj);	
			}
			
			JSONObject obj = new JSONObject();
			obj.put("Id", ++m_Id);
			obj.put("Source", "Scale");
			obj.put("CollectionTime", md.getCollectionTime());
			obj.put("Compression", "plain");
			obj.put("Data", jsonArrayIn.toJSONString());
			
			m_GzipOutStream.write(obj.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET)));
		}
	}

	
    /**
     * Gzip compress <code>data</code>
     * 
     * @param data
     * @return
     */
    private byte[] gzipCompress(byte[] data)
    {
		ByteArrayOutputStream oBuf = new ByteArrayOutputStream();
		GZIPOutputStream gBuf;
		try 
		{
			gBuf = new GZIPOutputStream(oBuf);
			gBuf.write(data);
			gBuf.close();
			oBuf.close();
			
			return oBuf.toByteArray();
		}
		catch (IOException e) 
		{
			s_Logger.error("ERROR: gzip compression failed!");
		}

		return null;
    }
}
