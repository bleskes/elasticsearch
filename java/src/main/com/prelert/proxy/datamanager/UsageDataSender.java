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

package com.prelert.proxy.datamanager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.data.BuildInfo;
import com.prelert.proxy.datamanager.DatabaseManager;


/**
 * Class to send usage data back to Prelert.
 *
 * To maximise the chance of getting through corporate firewalls, data is sent
 * via an HTTP GET to port 80 on the Prelert internet site.  If usage data
 * can't be sent for any reason, this is NOT considered an error, and the
 * product continues to operate  continues to operate.
 *
 * URL arguments are:
 *
 * id = Customer ID (taken verbatim from the license key)
 * typ = Data type - one of "download", "install", "start", "end"
 * tm = Unix epoch time of the event (measured in seconds since 1/1/1970, i.e.
 *      NOT the usual milliseconds that Java uses)
 *
 * In addition, each plugin may provide zero or more extra arguments to be sent
 * as usage data.  For example, the Introscope plugin also provides:
 *
 * plg = Name of plugin
 * agnt = Number of agents selected
 * met = Number of metrics analyzed
 *
 * Some example usage URL requests are:
 *
 * Customer "0a57f1c" downloaded:
 * http://www.prelert.com/usage/usage.php?id=0a57f1c&typ=download&tm=1327445900
 *
 * Installation:
 * http://www.prelert.com/usage/usage.php?id=0a57f1c&typ=install&tm=1327445905
 *
 * Analysis started with 2 agents selected:
 * http://www.prelert.com/usage/usage.php?id=0a57f1c&typ=start&tm=1327445906&plg=Introscope&agnt=2
 *
 * Analysis ended with 30,000 metrics processed:
 * http://www.prelert.com/usage/usage.php?id=0a57f1c&typ=end&tm=1327445907&plg=Introscope&met=30000
 *
 * Analysis started at a different time with 4 agents selected:
 * http://www.prelert.com/usage/usage.php?id=0a57f1c&typ=start&tm=1327445909&plg=Introscope&agnt=4
 *
 * Analysis ended with 60,000 metrics processed:
 * http://www.prelert.com/usage/usage.php?id=0a57f1c&typ=end&tm=1327445911&plg=Introscope&met=60000
 *
 * @author David Roberts
 */
public class UsageDataSender
{
	static Logger s_Logger = Logger.getLogger(UsageDataSender.class);

	/**
	 * Textual URL to send usage data to.
	 */
	private static final String PRELERT_URL = "http://www.prelert.com/usage/usage.php";

	/**
	 * Character set to be used to encode the URL.
	 */
	private static final String URL_CHARSET = "UTF-8";

	/**
	 * Only wait 1 second for the Prelert website to respond.
	 */
	private static final int TIMEOUT_MS = 1000;

	/**
	 * Enumeration of the different states that we report usage data for:
	 * - download
	 * - install
	 * - start
	 * - end
	 */
	public enum UsageDataType { DOWNLOAD, INSTALL, START, END };


	/**
	 * The ID of the customer that the usage data relates to.  If this is null,
	 * we don't report any usage data.
	 */
	private String m_CustomerId;


	/**
	 * Construct with a customer ID.
	 * @param customerId The ID of the customer that the usage data relates to.
     *                   If this is null, we don't report any usage data.
	 */
	public UsageDataSender(String customerId)
	{
		m_CustomerId = customerId;
	}


	/**
	 * Send an install message.
	 */
	public void sendInstallMessage()
	{
		// Don't send any data if the customer ID is null
		if (m_CustomerId == null)
		{
			return;
		}

		String urlString = buildUrl(UsageDataType.INSTALL, null);
		boolean success = connectUrl(urlString);
		if (success)
		{
			s_Logger.info("Successfully sent install message to URL " + urlString);
		}
		else
		{
			s_Logger.info("Failed to send install message to URL " + urlString);
			// TODO - write URL to plain text file so that we have a chance of
			//        getting the information by other means
		}
	}


	/**
	 * Send a start message.
	 */
	public void sendStartMessage(Map<String, String> extraArgs)
	{
		// Don't send any data if the customer ID is null
		if (m_CustomerId == null)
		{
			return;
		}

		String urlString = buildUrl(UsageDataType.START, extraArgs);
		boolean success = connectUrl(urlString);
		if (success)
		{
			s_Logger.info("Successfully sent start message to URL " + urlString);
		}
		else
		{
			s_Logger.info("Failed to send start message to URL " + urlString);
			// TODO - write URL to plain text file so that we have a chance of
			//        getting the information by other means
		}
	}


	/**
	 * Send an end message.
	 */
	public void sendEndMessage(Map<String, String> extraArgs)
	{
		// Don't send any data if the customer ID is null
		if (m_CustomerId == null)
		{
			return;
		}

		String urlString = buildUrl(UsageDataType.END, extraArgs);
		boolean success = connectUrl(urlString);
		if (success)
		{
			s_Logger.info("Successfully sent end message to URL " + urlString);
		}
		else
		{
			s_Logger.info("Failed to send end message to URL " + urlString);
			// TODO - write URL to plain text file so that we have a chance of
			//        getting the information by other means
		}
	}


	/**
	 * Encode the URL arguments.
	 * @param map A map of name/value strings representing the URL
	 *            arguments.
	 * @return A string representation of the URL arguments.
	 */
	String urlEncodeArgsUTF8(Map<String, String> map)
	{
		StringBuilder result = new StringBuilder("?");

		try
		{
			for (Map.Entry<String, String> entry : map.entrySet())
			{
				if (result.length() > 1)
				{
					result.append('&');
				}
				result.append(URLEncoder.encode(entry.getKey(), URL_CHARSET));
				result.append('=');
				result.append(URLEncoder.encode(entry.getValue(), URL_CHARSET));
			}
		}
		catch (UnsupportedEncodingException e)
		{
			// This really shouldn't happen, as Java should always support UTF-8
			s_Logger.error(e);
		}

		// Return an empty string if there are no arguments
		if (result.length() <= 1)
		{
			return "";
		}

		return result.toString();
	}


	/**
	 * Build the full URL, by concatenating the arguments onto the end of the
	 * base URL.
	 * @param type The type of usage data we're sending.
	 * @param extraArgs Any extra arguments to be sent.
	 * @return The full URL we should try to connect to.
	 */
	private String buildUrl(UsageDataType type,
							Map<String, String> extraArgs)
	{
		Map<String, String> allArgs = (extraArgs == null) ? new HashMap<String, String>() : new HashMap<String, String>(extraArgs);

		// Add the compulsory arguments (id, typ, tm, ver, build) to the optional ones
		allArgs.put("id", m_CustomerId);

		switch (type)
		{
			case DOWNLOAD:
			{
				allArgs.put("typ", "download");
				break;
			}
			case INSTALL:
			{
				allArgs.put("typ", "install");
				break;
			}
			case START:
			{
				allArgs.put("typ", "start");
				break;
			}
			case END:
			{
				allArgs.put("typ", "end");
				break;
			}
			default:
			{
				s_Logger.error("Unhandled UsageDataType " + type);
				break;
			}
		}

		Date timeNow = new Date();
		long epochSeconds = timeNow.getTime() / 1000l;
		allArgs.put("tm", Long.toString(epochSeconds));

		BuildInfo info = new BuildInfo();
		allArgs.put("ver", info.getVersionNumber());
		allArgs.put("build", info.getBuildNumber());

		// Concatenate the arguments onto the end of the base URL
		return PRELERT_URL + urlEncodeArgsUTF8(allArgs);
	}


	/**
	 * Connect to the specified URL.  Don't worry what the response is.
	 * @param urlString The full URL, including the query string, that we are to
	 *                  connect to.
	 * @return true if we connected to the URL, and false if we didn't.
	 */
	private boolean connectUrl(String urlString)
	{
		try
		{
			URL prelertUrl = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection)prelertUrl.openConnection();
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.connect();
			int resp = connection.getResponseCode();
			s_Logger.info("HTTP status code for usage data was " + resp);
			connection.disconnect();
			return (resp == HttpURLConnection.HTTP_OK);
		}
		catch (MalformedURLException mue)
		{
			// URL is messed up
		}
		catch (ClassCastException cce)
		{
			// Not HTTP connection?
		}
		catch (IOException ioe)
		{
			// Connection failure
		}

		return false;
	}


	/**
	 * Entry point that sends a message to the Prelert website reporting
	 * successful installation of the product.  This should be called at the
	 * very end of the installation, AFTER the prelert_licinst program has
	 * been run.
	 * @param args
	 */
	public static void main(String args[])
	{
		try
		{
			// Configure logging
			BasicConfigurator.configure();

			// Log the copyright and version
			s_Logger.info(BuildInfo.fullInfo("UsageDataSender"));

			ApplicationContext applicationContext =
					new ClassPathXmlApplicationContext("usageDataContext.xml");

			DatabaseManager dbMgr =
					applicationContext.getBean("databaseManager", DatabaseManager.class);
			if (dbMgr != null)
			{
				UsageDataSender sender = new UsageDataSender(dbMgr.getCustomerId());
				sender.sendInstallMessage();
			}
			else
			{
				s_Logger.error("Database manager is null");
			}
		}
		catch (Exception e)
		{
			s_Logger.fatal("UsageDataSender Fatal Error", e);

			System.exit(1);
		}
	}

}

