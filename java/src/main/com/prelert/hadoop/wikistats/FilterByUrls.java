package com.prelert.hadoop.wikistats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.fs.FileSystem;
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
 * Filter the output of the HitCountByUrl by a list of URLs.</br>
 * Only URLs in the list will be emitted.
 * 
 * There is no reduce step in this job.</br>
 * 
 * Data is tab separated formatted as
 * 	epochtime	url	hitcount 
 *
 */
public class FilterByUrls 
{
	public static final String URL_FILE = "url_file";
	
	public static class Map
	extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text>
	{	
		private final Set<String> s_RequiredUrls = new HashSet<String>();
		
		private static final Logger s_Logger = Logger.getLogger(Map.class);
		
		@Override
		public void configure(JobConf conf)
		{	    	
			try
			{
				FileSystem fs = FileSystem.get(conf);
				if (fs == null)
				{
					s_Logger.error("Cannot open filesystem");
					return;
				}

				Path url_file = new Path(conf.get(URL_FILE));	    	
		
				if (fs.exists(url_file))
				{
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(fs.open(url_file)));

					String line = reader.readLine();
					while (line != null)
					{
						s_RequiredUrls.add(line.trim());
						line = reader.readLine();				
					}
				}


				if (s_RequiredUrls.size() == 0)
				{
					s_Logger.error("Empty url file '" + conf.get(URL_FILE) + "'");
				}
				else
				{
					s_Logger.info("Loaded " + s_RequiredUrls.size() + " urls");
				}
			}
			catch (IOException io)
			{
				s_Logger.error("Configure error", io);
			}
			
			
		}
		
		
		@Override
		public void	map(LongWritable key, Text value, OutputCollector<Text, Text>
							output, Reporter reporter) throws IOException
		{		
			if (s_RequiredUrls.size() == 0)
			{
				throw new IOException("Urls not loaded");
			}
			
			String line = value.toString();
			String [] tokens = line.split("\\t");
			if (tokens.length != 3)
			{
				s_Logger.error("Line must contain 3 tokens");
				return;
			}
			
			
			
			if (s_RequiredUrls.contains(tokens[1]))
			{
				output.collect(new Text(tokens[0]), new Text(tokens[1] + "\t" + tokens[2]));
			}
		}		
	}
	
	
    public static void main(String[] args) throws Exception 
    {
    	if (args.length < 3)
    	{
    		return;    		
    	}
    	   	
    	JobConf conf = new JobConf(ReduceHitCountData.class);
    	conf.setJobName("filter_by_url");

    	conf.setOutputKeyClass(Text.class);
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
    	
    	conf.set(URL_FILE, args[2]);
    		

    	JobClient.runJob(conf);
    }
}
