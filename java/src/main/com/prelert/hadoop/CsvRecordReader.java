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
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.LineRecordReader;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.log4j.Logger;

/**
 * CSV specific implementation of the record reader. 
 * The CSV header is parsed on creation so it will not
 * be returned by the first call to <code>next()</code>. 
 */
public class CsvRecordReader implements RecordReader<Text, Text> 
{
	private static final Logger s_Logger = Logger.getLogger(CsvRecordReader.class);
			
	private LineRecordReader m_LineReader;
	private LongWritable m_LineKey;
	private Text m_LineValue;
	
	private String m_Header;
	
	/**
	 * Create the line reader and get the CSV header. 
	 * The line reader starting point is incremented to beyond the header
	 * so a call to <code>next()</code> will never return the header. 
	 * 
	 * @param job
	 * @param split
	 * @throws IOException
	 */
	public CsvRecordReader(JobConf job, FileSplit split) throws IOException  
	{
		m_Header = readCsvHeader(job, split);	
		m_LineReader = new LineRecordReader(job, split);

		m_LineKey = m_LineReader.createKey();
		m_LineValue = m_LineReader.createValue();		
		
		if (split.getStart() == 0)
		{
			// throw away the header
			m_LineReader.next(m_LineKey, m_LineValue);
		}
	}

	
	/**
	 * Reads a line of the csv file, the input key is the first 
	 * field in the file (time) and the input value is the remaining
	 * fields.
	 */
	@Override
	public boolean next(Text key, Text value) throws IOException 
	{	
		// get the next line
		if (m_LineReader.next(m_LineKey, m_LineValue) == false) 
		{
			return false;
		}
		
		String [] pieces = m_LineValue.toString().split(",", 2);
		key.set(pieces[0]);
		value.set(pieces[1]);
		
		return true;
	}

	@Override
	public Text createKey() 
	{
		return new Text("");
	}
	
	@Override
	public Text createValue() 
	{
		return new Text("");
	}

	@Override
	public long getPos() throws IOException 
	{
		return m_LineReader.getPos();
	}

	@Override
	public float getProgress() throws IOException 
	{
		return m_LineReader.getProgress();
	}

	@Override
	public void close() throws IOException 
	{
		m_LineReader.close();
	}
	
	
	/**
	 * Return the CSV file header or <code>null</code> if the header 
	 * can't be read.
	 * 
	 * @return
	 */
	public String header()
	{
		return m_Header;
	}
	
	
	/**
	 * Returns the first non-empty line of the csv file specified 
	 * in <code>split</code>.
	 * 
	 * @param job
	 * @param split
	 * @return
	 * @throws IOException
	 */
	private String readCsvHeader(JobConf job, FileSplit split) throws IOException
	{
		String header = null;
		
		FileSystem fs = FileSystem.get(job);
		
		if (fs.exists(split.getPath()))
		{
			BufferedReader reader = new BufferedReader(
								new InputStreamReader(fs.open(split.getPath())));
			
			// skip empty lines
			header = reader.readLine();
			while (header != null && header.isEmpty())
			{
				header = reader.readLine();
			}
		}
		
		s_Logger.info("CSV Header = " + header);
		return header;
	}
}
