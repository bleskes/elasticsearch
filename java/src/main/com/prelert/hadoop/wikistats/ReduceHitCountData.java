

package com.prelert.hadoop.wikistats;
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

import java.io.IOException;

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
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.log4j.Logger;

/**
 * Filter the output of the HitCountByUrl job by hitcount. 
 * HitCountByUrl outputs all URls hit more than once during the hour this
 * job will reduce the data further by setting the minimum hit count to a higher
 * value. Edit <code>MIN_HIT_COUNT</code> to change this the default value is
 * 100 which will remove ~ 99% of the Urls.</br>
 * 
 * There is no reduce step in this job.</br>
 * 
 * Data is tab separated formatted as
 * 	epochtime	url	hitcount 
 *
 */
public class ReduceHitCountData 
{
	public static final Integer MIN_HIT_COUNT = 100;
	
	public static class Map
	extends MapReduceBase implements Mapper<LongWritable, Text, LongWritable, Text>
	{
		private static final Logger s_Logger = Logger.getLogger(Map.class);
		
		
		@Override
		public void	map(LongWritable key, Text value, OutputCollector<LongWritable, Text>
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
				Integer hitcount = Integer.parseInt(tokens[2]);
				if (hitcount >= MIN_HIT_COUNT)
				{
					Long epoch = Long.parseLong(tokens[0]);
					output.collect(new LongWritable(epoch), new Text(tokens[1] + "\t" + tokens[2]));
				}
			}
			catch (NumberFormatException e)
			{
				s_Logger.warn(e);					
			}
		}		
	}
	
	
    public static void main(String[] args) throws Exception 
    {
    	JobConf conf = new JobConf(ReduceHitCountData.class);
    	conf.setJobName("reduce_hit_count_by_url");

    	conf.setOutputKeyClass(LongWritable.class);
    	conf.setOutputValueClass(Text.class);

    	conf.setMapperClass(Map.class);
    	conf.setNumReduceTasks(0); // no reducer
    	//conf.setReducerClass(Reduce.class);
    	
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
