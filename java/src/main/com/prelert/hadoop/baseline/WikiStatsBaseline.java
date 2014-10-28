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

package com.prelert.hadoop.baseline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 *  Extract the total hit count in the Wikipedia traffic 
 *  stats V3 data using plain java <em>not</em> Hadoop<br/>
 *  
 *  Use the Amazon S3 API to read the files one at a time summing the hit count 
 *  in each. Use the variable <code>startFromThisFile</code> to start the 
 *  processing from a particular file this is useful if program fails as you can 
 *  restart it from the last file it processed.
 */
public class WikiStatsBaseline 
{
	private static final Logger s_Logger = Logger.getLogger(WikiStatsBaseline.class);
	private static SimpleDateFormat s_DateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	
	private static final String BUCKET_NAME = "prelert-hadoop-data";
		
	
	
	public static void main(String[] args) throws Exception 
	{
		s_Logger.info("Starting Wiki Stats hit count");
		
		System.setProperty("aws.accessKeyId", "AKIAIQVPCQWTJS2QUW6Q");
		System.setProperty("aws.secretKey", "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");
		
        AmazonS3 s3 = new AmazonS3Client(new SystemPropertiesCredentialsProvider());
		s3.setRegion(Region.getRegion(Regions.US_EAST_1));
		
		// this is actually the file before the one we are interested in
		// listing starts after this file
		String startFromThisFile = "wiki_stats_v3/pagecounts-20110302-040000.gz";
		
		ListObjectsRequest listObjs = new ListObjectsRequest().
				withBucketName(BUCKET_NAME).withPrefix("wiki_stats_v3/pagecounts-").
				withMaxKeys(2000). /* BUG this setting isn't respected it always returns 1000 keys */
				withMarker(startFromThisFile);
		listObjs.setMaxKeys(2000);
		
		ObjectListing objs = s3.listObjects(listObjs);
		
		
		//s_Logger.info(objs.getObjectSummaries().size());

		s_Logger.info(String.format("Processing %d files starting with %s", 
						objs.getObjectSummaries().size(), 
						objs.getObjectSummaries().get(0).getKey()));
		
		
		Map<Long,Long> hitCountByBuckettime = new HashMap<Long,Long>();
		
		for (S3ObjectSummary summary : objs.getObjectSummaries())
		{
			s_Logger.info("Processing " + summary.getKey());
			
			long buckettime = getDateFromFilename(summary.getKey());
			
			S3Object object = s3.getObject(new GetObjectRequest(BUCKET_NAME, summary.getKey()));
			
			
			BufferedReader reader = new BufferedReader(
										new InputStreamReader(
												new GZIPInputStream(							
														object.getObjectContent())));
			
			String line;
			long sum = 0;
			while ((line = reader.readLine()) != null)
			{
				String [] tokens = line.split("\\s");
				if (tokens.length != 4)
				{
					s_Logger.error("Line must contain 4 tokens");
					continue;
				}
				
				try
				{
					long hitCount = Long.parseLong(tokens[2]);
					sum = sum + hitCount;
				}
				catch (NumberFormatException e)
				{
					s_Logger.error("Cannot parse '"+tokens[2] + "'", e);					
				}
			}
			
			hitCountByBuckettime.put(buckettime, sum);
			
			s_Logger.info(String.format("Hit count for bucket %d = %d", buckettime, sum));
			
			reader.close();
		}
		
		for (Map.Entry<Long,Long> e: hitCountByBuckettime.entrySet())
		{
			s_Logger.info(String.format("%d\t%d", e.getKey(), e.getValue()));
		}
	
		
		s_Logger.info("Finished Wiki Stats hit count");
	}
	
	
	
	
	/**
	 * Filenames are formatted pagecounts-YYYYMMDD-HHMMSS.gz
	 * @param filename
	 * @return
	 * @throws ParseException
	 */
	private static long getDateFromFilename(String filename)
	{
		Pattern pattern = Pattern.compile("wiki_stats_v3/pagecounts-([0-9]{8}\\-[0-9]{6}).*");
		Matcher matcher = pattern.matcher(filename);
		if (matcher.matches())
		{
			try
			{
				return s_DateFormat.parse(matcher.group(1)).getTime();
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
		
		return new Date(0).getTime();					
	}
}
