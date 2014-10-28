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

package com.prelert.devutils.dynatrace;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.prelert.data.Attribute;
import com.prelert.data.XmlStringEscaper;

public class DynatraceCsvToTimeSeriesData 
{
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		String filename = "";

		if (args.length > 0)
		{
			filename = args[0];
		}

		FileReader file = new FileReader(filename);

		FileWriter outFile = new FileWriter(filename + "_points.xml");

		System.out.println("Processing file...");

		CsvListReader csvReader = new CsvListReader(file, CsvPreference.EXCEL_PREFERENCE);		

        // Read the header
		List<String> line = csvReader.read();

		while ((line = csvReader.read()) != null)
		{
			try
			{
/*
"dynamic_measure_name",
"dynamic_measuretype",
"dynamic_is_dynamic",
"source_name",
"source_host",
"systemprofile_name",
"countvalue",
"sumvalue",
"maxvalue",
"minvalue",
"formatted_timestamp",
"metricgroup_name",
"metric_name"
*/

                // Example line
/*
"LogicalDisk Free Percentage",19,1,"Windows Performance Monitor","wppwa02a0003","OPICS-PROD",1,61.4539985656738,61.4539985656738,61.4539985656738,"2011-07-13 11:54:00-04","Windows Performance Monitor","LogicalDisk Free Percentage"
"Web Service Cache: File Cache Flushes",19,1,"Windows Performance Monitor","wppwa02a0003","OPICS-PROD",1,22801,22801,22801,"2011-07-13 11:47:00-04","IIS Performance Monitor","Web Service Cache: File Cache Flushes"
*/

				String dynamic_measure_name = line.get(0);
//				String dynamic_measuretype = line.get(1);
//				String dynamic_is_dynamic = line.get(2);
				String source_name = line.get(3);	
				String source_host = line.get(4);
				String systemprofile_name = line.get(5);
				String countvalue = line.get(6);
				String sumvalue = line.get(7);
				String maxvalue = line.get(8);
				String minvalue = line.get(9);
				String formatted_timestamp = line.get(10);
				String metricgroup_name = line.get(11);
				String metric_name = line.get(12);

                Date    date;

                // Date is 2011-07-13 11:29:00-04
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
				try
				{
					date = dateFormat.parse(formatted_timestamp);
				}
				catch (Exception pe)
				{
					System.out.println(csvReader.getLineNumber());
					System.out.println(pe);
					csvReader.read();
					continue;
				}

                // Convert values

                // countvalue must be an integer
                int countValueInt = Integer.parseInt(countvalue);

                // Other values could be ints or doubles, but as we're using doubles in TimeSeriesDataPoint
                // we convert all to doubles here and live with the minor loss of precision
				double sumValueDouble = Double.parseDouble(sumvalue);
				double minValueDouble = Double.parseDouble(minvalue);
				double maxValueDouble = Double.parseDouble(maxvalue);

                // Create a mean value
                double meanValueDouble = sumValueDouble / (double)countValueInt;

                // Write points
                writePoint(outFile, 
                            date,
                            dynamic_measure_name, 
                            source_name, 
                            systemprofile_name, 
                            metricgroup_name,
                            source_host,
                            metric_name,
                            "average", // Aggregator
                            meanValueDouble);

                writePoint(outFile, 
                            date,
                            dynamic_measure_name, 
                            source_name, 
                            systemprofile_name, 
                            metricgroup_name,
                            source_host,
                            metric_name,
                            "minimum", // Aggregator
                            minValueDouble);

                writePoint(outFile, 
                            date,
                            dynamic_measure_name, 
                            source_name, 
                            systemprofile_name, 
                            metricgroup_name,
                            source_host,
                            metric_name,
                            "maximum", // Aggregator
                            maxValueDouble);
            }
			catch (Exception e)
			{
				System.out.println(e);
				csvReader.read();
				continue;
			}

		}

		outFile.flush();
		outFile.close();
	}

    public static void writePoint(FileWriter outFile,
                                    Date date,
                                    String dynamic_measure_name,
                                    String source_name,
                                    String systemprofile_name,
                                    String metricgroup_name,
                                    String source_host,
                                    String metric_name,
                                    String aggregator,
                                    double value) throws Exception
    {
        // Create attributes
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("dynamic_measure_name", dynamic_measure_name));
        attributes.add(new Attribute("source_name", source_name));
        attributes.add(new Attribute("systemprofile_name", systemprofile_name));
        attributes.add(new Attribute("aggregator", aggregator));

        // type is 'metricgroup_name'
        // source is 'source_host'
        // metric_name is 'metric_name'
        String type = "<type>" + XmlStringEscaper.escapeXmlString(metricgroup_name) + "</type>";
        String source = "<source>" + XmlStringEscaper.escapeXmlString(source_host) + "</source>";
        String metric = "<metric>" + XmlStringEscaper.escapeXmlString(metric_name) + "</metric>";
        String time = "<time>" + date.getTime() / 1000l + "</time>";    // seconds since epoch

        StringBuilder builder = new StringBuilder();

        builder.append("<points>");
        builder.append("<point>");
        builder.append(type);
        builder.append(source);
        builder.append(metric);
        builder.append(time);
        builder.append("<value>" + value + "</value>");
        for (Attribute attribute : attributes)
        {
            builder.append(attribute.toXmlTagInternal());
        }
        builder.append("</point>");
        builder.append("</points>\n");

        outFile.write(builder.toString());
    }
}
