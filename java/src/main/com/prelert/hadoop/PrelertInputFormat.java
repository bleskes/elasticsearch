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

package com.prelert.hadoop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.log4j.Logger;


/**
 * Prelert specific input format class. Overrides the behaviour of
 * {@link org.apache.hadoop.mapred.FileInputFormat} in the way it splits
 * files and the type of record reader it creates. 
 */
public class PrelertInputFormat extends FileInputFormat<Text, Text> 
{
	public static final String BY_FIELD = "prelert.by.index";
	public static final String SOURCETYPE_FIELD = "prelert.sourcetype.index";
	public static final String VALUE_FIELD = "prelert.value.index"; 
	public static final String FIELD_CONFIG = "prelert.fieldconfig";
	public static final String HEADER = "prelert.header";
	/**
	 * If defined in the job conf then the anomaly detector will persist
	 * its models every time it sees this many records.
	 */
	public static final String PERSISTENCE_INTERVAL = "prelert.persistence.interval";
	public static final Integer PERSISTENCE_RECORD_COUNT = 100;
	
	
	private static final Logger s_Logger = Logger.getLogger(PrelertInputFormat.class);
	
	/**
	 * Don't split input files, the full file should go to each 
	 * mapper.
	 * 
	 * @param fs the file system that the file is on
	 * @param filename the file name to check
	 * @return is this file splitable?
	 */
	@Override
	protected boolean isSplitable(FileSystem fs, Path filename) 
	{
		return false;
	}
	  
	/**
	 * Extracts the header from the CSV file and sets it in the
	 * job conf for the mapper class. Returns a {@link CsvRecordReader}.
	 */
	@Override
	public RecordReader<Text, Text> getRecordReader(
											InputSplit input, JobConf job, 
											Reporter reporter)
	throws IOException 
	{
		s_Logger.info("Get Prelert record reader");
		s_Logger.info("Create symlink: " + job.get("mapred.create.symlink"));

		
		// Cast to file split, don't check is instanceof
		// as always will be in subclass of FileInputFormat<>
		FileSplit fileSplit = (FileSplit)input;	
		
//		Iterator<Map.Entry<String,String>> iter = job.iterator();
//		while (iter.hasNext())
//		{
//			Map.Entry<String,String> entry = iter.next();
//			s_Logger.info(entry.getKey() + " = " + entry.getValue());
//		}

		job.setInt(PERSISTENCE_INTERVAL, PERSISTENCE_RECORD_COUNT);
		
		String configFileName = job.get(FIELD_CONFIG);
		if (configFileName == null)
		{
			s_Logger.error("No field config file was specified");
		}
		else
		{
			// Add the field config file contents to the job conf
			FileSystem fs = FileSystem.get(job);
			Path configFilePath = new Path(configFileName); 
			if (fs.exists(configFilePath))
			{
				BufferedReader reader = new BufferedReader(
									new InputStreamReader(fs.open(configFilePath)));
				
				StringBuilder configContents = new StringBuilder();
				String line = reader.readLine();
				while (line != null)
				{
					configContents.append(line);
					line = reader.readLine();
				}
				
				if (configContents.length() == 0)
				{
					s_Logger.error("Empty field config file '" + configFileName + "'");
				}
				
				job.set(FIELD_CONFIG, configContents.toString());
			}
		}
		
			
		reporter.setStatus(input.toString());
		
		CsvRecordReader recordReader = new CsvRecordReader(job, fileSplit);
		// read the csv header and pass through to the job. 
		job.set(HEADER, recordReader.header());
		
		return recordReader;
	}
}
