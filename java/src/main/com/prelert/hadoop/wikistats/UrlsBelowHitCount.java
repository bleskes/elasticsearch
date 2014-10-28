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
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.log4j.Logger;

/**
 * Emit all Urls below a certain hit count
 * 
 * The Data is arranged 'projectcode, pagename, pageviews, bytes' e.g<br/>
 * 	en Barack_Obama 997 123091092
 */
public class UrlsBelowHitCount 
{
	public static final Integer MIN_HIT_COUNT = 50;
	
	public static class Map
	extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text>
	{
		private static final Logger s_Logger = Logger.getLogger(Map.class);
		
		
		@Override
		public void	map(LongWritable key, Text value, OutputCollector<Text, Text>
							output, Reporter reporter) throws IOException
		{
			String line = value.toString();
			String [] tokens = line.split("\\t");
			if (tokens.length != 3)
			{
				s_Logger.error("Line must contain 3 tokens");
				return;
			}

			try
			{			
//				Integer hitcount = Integer.parseInt(tokens[2]);
//				if (hitcount < MIN_HIT_COUNT)
//				{
					output.collect(new Text(tokens[1]), new Text(tokens[1]));
//				}
			}
			catch (NumberFormatException e)
			{
				s_Logger.warn(e);					
			}
		}		
	}
	
	
    public static class Reduce 
    extends MapReduceBase implements Reducer<Text, Text, Text, Text> 
    {
    	//private static final Logger s_Logger = Logger.getLogger(Reduce.class);
    			
    	@Override
    	public void reduce(Text key, Iterator<Text> values, 
    					OutputCollector<Text, Text> output, Reporter reporter) 
    	throws IOException
    	{
    		//s_Logger.info("Value count = " + )
   			output.collect(key, null);
    	}    	
    }
	
	
    public static void main(String[] args) throws Exception 
    {
    	JobConf conf = new JobConf(ReduceHitCountData.class);
    	conf.setJobName("urls_below_hit_count");

    	conf.setOutputKeyClass(Text.class);
    	conf.setOutputValueClass(Text.class);

    	conf.setMapperClass(Map.class);
    	conf.setReducerClass(Reduce.class);
    	
    	conf.setKeepFailedTaskFiles(true);
    	
    	conf.setCompressMapOutput(true);

    	conf.setInputFormat(TextInputFormat.class);
    	conf.setOutputFormat(TextOutputFormat.class);
    	
    	TextOutputFormat.setCompressOutput(conf, true);
    	TextOutputFormat.setOutputCompressorClass(conf, GzipCodec.class);

    	FileInputFormat.setInputPaths(conf, new Path(args[0]));
    	FileOutputFormat.setOutputPath(conf, new Path(args[1]));

    	JobClient.runJob(conf);
    }

}
