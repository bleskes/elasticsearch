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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * Class for parsing marketing messages from HTML content.
 * @author Pete Harverson
 */
public class MarketingMessageParser
{
	static Logger s_Logger = Logger.getLogger(MarketingMessageParser.class);

	
	/**
	 * Parses the html content at the supplied URL to obtain marketing messages
	 * from within lowercase or uppercase DIV elements.
	 * @param spec String representation of the URL containing the message HTML.
	 * @return	list of messages, or an empty list if no DIV elements were found 
	 * 	at the supplied URL.
	 * @throws IOException if an error occurs trying to parse content from the
	 * 	supplied URL, for example if the URL could not be accessed.
	 */
	public List<String> parseDivsFromURL(String spec) throws IOException
	{
		s_Logger.debug("parseDivsFromURL(" + spec + ")");
		
		// Read in content of URL.
		URL url = new URL(spec);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(url.openStream()));

		StringBuilder strBuilder = new StringBuilder();
		String inputLine;

		while ((inputLine = in.readLine()) != null)
		{
			strBuilder.append(inputLine);
		}

		in.close();
	
		String html = strBuilder.toString();
		
		String startTag = "<div"; // Note may contain an id attribute.
		String endTag = "</div>";
		
		ArrayList<String> messages = new ArrayList<String>();

		// Split into a set of tokens by the start of the <div> tag.
		// Check for upper or lowercase tags.
		String[] firstSplit = StringUtils.splitByWholeSeparator(html, startTag);
		if (firstSplit.length == 1)
		{
			firstSplit = StringUtils.splitByWholeSeparator(html, startTag.toUpperCase());
		}
		
		if (firstSplit.length > 1)
		{
			String token;
			for (int i = 1; i < firstSplit.length; i++)
			{	
				// Obtain the text within the <div> element.
				token = StringUtils.substringBetween(firstSplit[i], ">", endTag);
				if (token == null)
				{
					token = StringUtils.substringBetween(firstSplit[i], ">", endTag.toUpperCase());
				}
				
				if (token != null && token.trim().length() > 0)
				{
					messages.add(token);
				}
						
			}
		}
		
		return messages;
	}
	
	
	public static void main(String[] args)
	{
    	try
    	{
    		MarketingMessageParser parser = new MarketingMessageParser();
        	List<String> messages = parser.parseDivsFromURL(
        			"http://www.prelert.com/download/marketing_messages.html");
        	s_Logger.debug("Messages: " + messages);
    	}
    	catch (IOException ioe)
    	{
    		s_Logger.error("Error reading messages from marketing URL: " , ioe);
    	}
    	
	}
	
}
