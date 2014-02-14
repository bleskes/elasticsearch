package com.prelert.hadoop.wikistats;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
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
 *  Create a histogram of the count of URLs then were hit N times i.e. if 10 ULRs
 *  were hit once the output would be 1	10 (tab delimited).<br/>
 *  
 *  The wiki stats data is arranged 'projectcode, pagename, pageviews, bytes' e.g<br/>
 *  en Barack_Obama 997 123091092
 */
public class HistogramHits 
{
	public static class Map
	extends MapReduceBase implements Mapper<LongWritable, Text, LongWritable, LongWritable>
	{
		private static final Logger s_Logger = Logger.getLogger(Map.class);
		
		private static final LongWritable ONE = new LongWritable(1); 

		@Override
		public void	map(LongWritable key, Text value, OutputCollector<LongWritable, LongWritable>
							output, Reporter reporter) throws IOException
		{
			String line = value.toString();
			String [] tokens = line.split("\\s");
			if (tokens.length != 4)
			{
				s_Logger.error("Line must contain 4 tokens");
				return;
			}
			
			try
			{
				LongWritable hits = new LongWritable(Long.parseLong(tokens[2]));
				output.collect(hits, ONE);
			}
			catch (NumberFormatException nfe)
			{
				s_Logger.error(String.format("Cannont parse '%s' as long", tokens[2]));
			}
		}
	}
	
    public static class Reduce 
    extends MapReduceBase implements Reducer<LongWritable, LongWritable, LongWritable, LongWritable> 
    {
    	@Override
    	public void reduce(LongWritable key, Iterator<LongWritable> values, 
    					OutputCollector<LongWritable, LongWritable> output, Reporter reporter) 
    	throws IOException
    	{   		
    		long sum = 0;
    		while (values.hasNext()) 
    		{
    			// sum += values.next().get();
    			values.next();
    			++sum; // always 1
    		}
    		
    		reporter.incrCounter("HITCOUNT", "HISTOGRAM", 1);
    		output.collect(key, new LongWritable(sum));
    	}
    }
    
	
    public static void main(String[] args) throws Exception 
    {
    	JobConf conf = new JobConf(HitCountByUrl.class);
    	conf.setJobName("histogram_hits");

    	conf.setOutputKeyClass(LongWritable.class);
    	conf.setOutputValueClass(LongWritable.class);

    	conf.setMapperClass(Map.class);
    	conf.setReducerClass(Reduce.class);

    	conf.setInputFormat(TextInputFormat.class);
    	conf.setOutputFormat(TextOutputFormat.class);

    	FileInputFormat.setInputPaths(conf, new Path(args[0]));
    	FileOutputFormat.setOutputPath(conf, new Path(args[1]));

    	JobClient.runJob(conf);
    }

}
