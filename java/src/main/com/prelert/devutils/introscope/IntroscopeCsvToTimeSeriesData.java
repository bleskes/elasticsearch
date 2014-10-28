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
package com.prelert.devutils.introscope;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.plugin.introscope.clworkstation.IntroscopeCsvProcessor;

/**
 * See the readme and wilyCsvToPrelertPoints.sh in the git repositry at 
 * <repo_home>devutils/platform/introscope/csvparser
 * 
 * The script csvConverterStartup.sh converts a .csv file generated from Wily
 * Introscope Command Line Workstation query into to Prelert time series points format xml.
 * 
 * It writes a file with the same name as the input but with a '_points.xml' extension.
 *
 * The script expects to have a ./lib folder below it containing prelert-utils.jar, superCSV-1-52.jar and log4j-1.2.16.jar
 * Usage:
 * 	./wilyCsvToPrelertPoints inputfile.csv
 */
public class IntroscopeCsvToTimeSeriesData 
{
	private static Logger s_Logger = Logger.getLogger(IntroscopeCsvToTimeSeriesData.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		// Configure logging
		BasicConfigurator.configure();
		
		String filename = "";

		if (args.length > 0)
		{
			filename = args[0];
		}

		FileReader inputFileReader = new FileReader(filename);
		
		String ouputFileName = filename + "_points.xml";
		FileWriter outFile = new FileWriter(ouputFileName);

		s_Logger.info("Processing file '" + filename + "'");
		
		IntroscopeCsvProcessor csvProcessor = new IntroscopeCsvProcessor("Introscope");
		Collection<TimeSeriesData> tsData = csvProcessor.processCsv(inputFileReader);
		
		try
        {
			for (TimeSeriesData data : tsData)
			{
				Collections.sort(data.getDataPoints());
				
				// All on one line.
				outFile.append("<points>");
				outFile.append(data.toXmlStringInternal(false));
				outFile.append("</points>\n");
			}
        }
		catch (IOException e)
		{
			s_Logger.fatal("Error writing to file '" + ouputFileName + "'.");
			s_Logger.fatal(e);
			
			throw e;
		}
		finally
		{
			outFile.close();
		}
		
	}
}

