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

package com.prelert.hadoop.jobcontrol;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

//import org.apache.hadoop.mapred.pipes.Submitter; // Pipes
//import org.apache.hadoop.streaming.HadoopStreaming; // Streaming


import org.apache.hadoop.streaming.PipeMapRunner;
import org.apache.hadoop.streaming.PipeMapper;
import org.apache.hadoop.streaming.PipeReducer;
import org.apache.hadoop.streaming.StreamJob;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import com.prelert.hadoop.example.wordcount.Map;
import com.prelert.hadoop.example.wordcount.Reduce;

public class JobControl 
{
	private static final Logger s_Logger = Logger.getLogger(JobControl.class);
	
	private static final String FILE = "file";
	private static final String LIBJARS = "libjars";
	private static final String INPUT_FORMAT = "inputformat";
	private static final String INPUT = "input";
	private static final String OUTPUT = "output";
	private static final String CMD_ENV = "cmdenv";
	private static final String MAPPER = "mapper";
	private static final String REDUCER = "reducer";
	private static final String JOB_CONF = "jobconf";
	
	/**
	 * The file containing the configuration options.
	 */
	private static final String RESOURCE_FILE = "prelert_resources.xml";
	
	
	public static void main(String[] args) throws Exception 
	{
		Configuration config = new Configuration(false);
		InputStream istream = JobControl.class.getClassLoader().getResourceAsStream(RESOURCE_FILE);
		if (istream == null)
		{
			s_Logger.fatal("Cannot open resource file '" + RESOURCE_FILE + "'");
			return;
		}
		
		config.addResource(istream);

		JobConf conf = runStreamingJob(config);
		
		FileSystem fs = FileSystem.get(conf);
		String outputDir = config.get(OUTPUT);
		String localDir = "/tmp/output/";
		fs.copyToLocalFile(new Path(outputDir), new Path(localDir));
	}
	
	
	/**
	 * Create a Hadoop streaming job from the config file and run
	 * 
	 * @param fileConf
	 * @throws Exception
	 */
	static private JobConf runStreamingJob(Configuration fileConf) throws Exception
	{	
		JobConf jobConf = new JobConf();
		jobConf.setJobName("RemoteJob");
		
		String jobtracker = jobConf.get("jobtracker");
		if (jobtracker == null)
		{
			jobtracker = "localhost:9001";
			s_Logger.info("No job tracker set using default" + jobtracker);
		}
		String filesystem = jobConf.get("filesystem");
		if (filesystem == null)
		{
			filesystem = "hdfs://localhost:9000";
			s_Logger.info("No filesystem set using default " + filesystem);
		}
		
		jobConf.set("mapred.job.tracker", jobtracker); 
		jobConf.set("fs.default.name", filesystem);

		jobConf.setKeepFailedTaskFiles(true); // debugging	

		List<String> streamingArgs = new ArrayList<String>();
		
		// generic options must be set first
		java.util.Map<String,String> confs = fileConf.getValByRegex(JOB_CONF + "\\d{0,2}");
		for (String val : confs.values())
		{
			addArgToList("D", val, streamingArgs);
		}
		addArgToList(LIBJARS, fileConf.get(LIBJARS), streamingArgs);
		
		// Get all the starting 'fileX' where X is an integer 
		int count = 1;
		
		StringBuilder files = new StringBuilder();
		
		// use get as iterating on the Map.Entry does not do variable expansion
		String filename = fileConf.get(String.format("%s%d", FILE, count));
		if (filename != null)
		{
			files.append(filename);
		}
		filename = fileConf.get(String.format("%s%d", FILE, ++count));
		while (filename != null)
		{
			files.append("," + filename);
			filename = fileConf.get(String.format("%s%d", FILE, ++count));
		}
		
		addArgToList("files", files.toString(), streamingArgs);
		addArgToList(INPUT_FORMAT, fileConf.get(INPUT_FORMAT), streamingArgs);
		addArgToList(INPUT, fileConf.get(INPUT), streamingArgs);
		addArgToList(OUTPUT, fileConf.get(OUTPUT), streamingArgs);
		addArgToList(MAPPER, fileConf.get(MAPPER), streamingArgs);
		addArgToList(REDUCER, fileConf.get(REDUCER), streamingArgs);
		java.util.Map<String,String> envVars = fileConf.getValByRegex(CMD_ENV + "\\d{0,2}");
		for (String val : envVars.values())
		{
			addArgToList(CMD_ENV, val, streamingArgs);
		}
	
		String [] jobArgs = streamingArgs.toArray(new String[streamingArgs.size()]);
		
		StreamJob streamJob = new StreamJob();
		streamJob.setConf(jobConf);
		ToolRunner.run(streamJob, jobArgs);
		
		return jobConf;
	}
	
	
	/**
	 * Run a plain Java Hadoop job.
	 * 
	 * @param args
	 * @throws IOException
	 */
	static private JobConf runJob(String [] args) throws IOException
	{
		JobConf conf = new JobConf();
		conf.setJobName("RemoteJob");
		
		conf.set("mapred.job.tracker", "192.168.62.251:9001"); 
		conf.set("fs.default.name", "hdfs://vm-centos-62-64-1:9000");
		
		conf.setMapperClass(Map.class);
		conf.setReducerClass(Reduce.class);
	
		//conf.setJar("wordcount.jar");
		//conf.setJarByClass(Map.class);
		conf.setJar("/Source/local/gui/apps/hadoop/jar/wordcount.jar");  // TODO
		
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);
		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);	
		
		FileInputFormat.setInputPaths(conf, new Path("/user/dkyle/input/"));
		FileOutputFormat.setOutputPath(conf, new Path("/user/dkyle/output/"));

		conf.setKeepFailedTaskFiles(true); // debuggin

		RunningJob running = JobClient.runJob(conf);
		
		return conf;
	}

	
	/**
	 * Adds the argument name and value to a list of args. The character '-' is
	 * prepended to the argument name. 
	 * 
	 * @param argname The argument name without '-' prepended, the dash is 
	 * added to the argument by this function.
	 * @param value If null then no action is taken.
	 * @param argsList If value is not null then the argname and value are added
	 * to this list.
	 */
	static private void addArgToList(String argname, String value, List<String> argsList)
	{
		if (value != null)
		{
			argsList.add("-" + argname);
			argsList.add(value);
		}
	}
}
