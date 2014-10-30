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

package com.prelert.hadoop.wikistats;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.log4j.Logger;


/**
 *  Extract the total <b>hit</b> count for each project code in the Wikipedia 
 *  traffic stats V3 data. The output of the map reduce job will be the total 
 *  hits by project for each hour.<br/>
 *  
 *  The timestamp of the data is extracted from the input file name which is formatted:
 *  <br/>pagecounts-YYYYMMDD-HHMMSS.gz
 *  <br/> e.g pagecounts-20110101-010000.gz<br/>
 *  
 *  The Data is arranged 'projectcode, pagename, pageviews, bytes' e.g<br/>
 *  en Barack_Obama 997 123091092
 */
public class HitCountByProject
{
	public static class Map
	extends MapReduceBase implements Mapper<LongWritable, Text, LongWritable, Text>
	{
		private static final Logger s_Logger = Logger.getLogger(Map.class);
		private SimpleDateFormat m_DateFormat;
		private boolean m_GotBucketTime;
		private LongWritable m_BucketTime;
		
		public Map()
		{
			m_DateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
			
			m_GotBucketTime = false;
		}
		
		@Override
		public void	map(LongWritable key, Text value, OutputCollector<LongWritable, 
							Text> output, Reporter reporter) 
		throws IOException
		{
			String line = value.toString();
			String [] tokens = line.split("\\s");
			if (tokens.length != 4)
			{
				s_Logger.error("Line must contain 4 tokens");
				return;
			}
			
			if (m_GotBucketTime == false)
			{
				m_BucketTime = new LongWritable(getBucketTime(reporter));
				m_GotBucketTime = true;
			}
		
			// bytes
			try
			{
				// output projectcode,bytes
				Text code_hits = new Text(tokens[0] + "\t" + tokens[2]);
				output.collect(m_BucketTime, code_hits);
			}
			catch (NumberFormatException nfe)
			{
				s_Logger.error(String.format("Cannot parse %s as a long", tokens[2]));
			}

		}		

		private long getBucketTime(Reporter reporter)
		{
			InputSplit split = reporter.getInputSplit();
			if (split instanceof FileSplit == false)
			{
				s_Logger.error("Input split is not a file, cannot get filename");
				return 0;
			}
			
			FileSplit fileSplit = (FileSplit)split;
			String filename= fileSplit.getPath().getName();			
			Date time = getDateFromFilename(filename);
			return time.getTime();
		}
		
		
		/**
		 * Filenames are formatted pagecounts-YYYYMMDD-HHMMSS.gz
		 * @param filename
		 * @return
		 * @throws ParseException
		 */
		public Date getDateFromFilename(String filename)
		{
			Pattern pattern = Pattern.compile("pagecounts-([0-9]{8}\\-[0-9]{6}).*");
			Matcher matcher = pattern.matcher(filename);
			if (matcher.matches())
			{
				try
				{
					return m_DateFormat.parse(matcher.group(1));
				} 
				catch (ParseException e) 
				{
					s_Logger.error("Cannot parse date from string: " + filename);
				}
			}
			else 
			{
				s_Logger.info("Cannot extract datetime from " + filename);
			}
			
			return new Date(0);					
		}
	}
	
	
    public static class Reduce 
    extends MapReduceBase implements Reducer<LongWritable, Text, LongWritable, Text> 
    {
    	private static final Logger s_Logger = Logger.getLogger(Reduce.class);

    	private static java.util.Map<String,Long> m_HitsByProject;
    	
    	@Override
    	public void reduce(LongWritable key, Iterator<Text> values, 
    					OutputCollector<LongWritable, Text> output, Reporter reporter) 
    	throws IOException 
    	{
    		m_HitsByProject = new HashMap<String, Long>();
    		
    		
    		while (values.hasNext()) 
    		{
    			String value = values.next().toString();
    			String [] tokens = value.split("\t");
    			if (tokens.length < 2)
    			{
    				s_Logger.error("Cannot split string '" + value + "'"); 
    				continue;
    			}
    			
    			Long sum = m_HitsByProject.get(tokens[0]);
    			if (sum == null)
    			{
    				sum = new Long(0);
    			}
    			
    			try 
    			{
    				long count = Long.parseLong(tokens[1]);
    				sum = sum + count;
    				m_HitsByProject.put(tokens[0], sum);
    			}
    			catch (NumberFormatException e) 
    			{
    				s_Logger.error("Cannot parse long from string: " + tokens[1]);
    			}

    			m_HitsByProject.put(tokens[0], sum);    				
    		}
    		
    		
    		Iterator<java.util.Map.Entry<String,Long>> iter = m_HitsByProject.entrySet().iterator();
    		while (iter.hasNext())
    		{
    			java.util.Map.Entry<String,Long> e = iter.next();
    			
    			Text val = new Text(String.format("%s\t%d", e.getKey(), e.getValue()));
    			output.collect(key, val);
    		}
    	}
    }
    
    
    public static void main(String[] args) throws Exception 
    {
    	JobConf conf = new JobConf(HitCount.class);
    	conf.setJobName("hit_count_by_project");

    	conf.setOutputKeyClass(LongWritable.class);
    	conf.setOutputValueClass(Text.class);

    	conf.setMapperClass(Map.class);
    	conf.setReducerClass(Reduce.class);

    	conf.setInputFormat(TextInputFormat.class);
    	conf.setOutputFormat(TextOutputFormat.class);

    	FileInputFormat.setInputPaths(conf, new Path(args[0]));
    	FileOutputFormat.setOutputPath(conf, new Path(args[1]));

    	JobClient.runJob(conf);
    }
}
