/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.rs.client;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

/**
 * A Http Client to test/develop the Prelert Autodetect RESTful web service against.</br>
 * Contains methods to create jobs, list jobs, upload data and query results.
 * 
 * </br>Implements closeable so it can be used in a try-with-resource statement
 */
public class AutodetectRsClient implements Closeable
{
	static final private Logger s_Logger = Logger.getLogger(AutodetectRsClient.class);
	
	private ObjectMapper m_JsonMapper;
		
	private CloseableHttpClient m_HttpClient;

	/**
	 * Creates a new http client and Json object mapper.
	 * Call {@linkplain #close()} once finished
	 */
	public AutodetectRsClient()
	{
		m_HttpClient = HttpClients.createDefault();
		m_JsonMapper = new ObjectMapper();
	}
	
	/**
	 * Close the http client
	 */
	@Override
	public void close() throws IOException
	{
		m_HttpClient.close();
	}
	
	/**
	 * Get details of all the jobs in database
	 * 
	 * @param url The URL of the API's jobs end point
	 * @return The pagination object containing a list of Jobs
	 * @throws IOException
	 */
	public Pagination<JobDetails> getJobs(String url) 
	throws IOException
	{
		HttpGet get = new HttpGet(url);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				Pagination<JobDetails> docs = m_JsonMapper.readValue(content, 
						new TypeReference<Pagination<JobDetails>>() {} );
				return docs;
			}
		}
		finally 
		{
			response.close();
		}		
		
		Pagination<JobDetails> page = new Pagination<>();
		page.setDocuments(Collections.<JobDetails>emptyList());
		return page;
	}
	
	/**
	 * Get the individual job on the provided URL
	 *  
	 * @param jobUrl Full path to job ie. <code>/api/job/{jobId}</code>
	 * @return If the job exists a {@link com.prelert.rs.data.SingleDocument SingleDocument}
	 * containing the Job is returned else the SingleDocument is empty
	 * @throws IOException
	 */
	public SingleDocument<JobDetails> getJob(String jobUrl) throws IOException
	{
		HttpGet get = new HttpGet(jobUrl);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				SingleDocument<JobDetails> doc = m_JsonMapper.readValue(content, 
						new TypeReference<SingleDocument<JobDetails>>() {} );
				return doc;
			}
			else
			{
				return new SingleDocument<>();
			}
		}
		finally 
		{
			response.close();
		}
	}
	
	/**
	 * Create a new job with the configuration in <code>createJobPayload</code>
	 * and return the full Url to the job's location 
	 * 
	 * @param apiUrl The base URL of the autodetect REST API
	 * @param createJobPayload The Json configuration for the new job
	 * @return The job Url or an empty string if there was an error
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String createJob(String apiUrl, String createJobPayload) 
	throws ClientProtocolException, IOException
	{
		HttpPost post = new HttpPost(apiUrl);
		
		StringEntity entity = new StringEntity(createJobPayload,
				ContentType.create("application/json", "UTF-8"));
		post.setEntity(entity);
		
		try (CloseableHttpResponse response = m_HttpClient.execute(post))
		{
			if (response.getStatusLine().getStatusCode() == 201)
			{
				if (response.containsHeader("Location"))
				{
					return response.getFirstHeader("Location").getValue();
				}
			}
			return "";
		}
	}
	
	/**
	 * Delete the individual job on the provided URL
	 *  
	 * @param jobUrl Full path to job ie. <code>/api/job/{jobId}</code>
	 * @return If the job exists as deleted return true else false
	 * @throws IOException, ClientProtocolException
	 */
	public boolean deleteJob(String jobUrl) 
	throws ClientProtocolException, IOException
	{
		HttpDelete delete = new HttpDelete(jobUrl);
		
		try (CloseableHttpResponse response = m_HttpClient.execute(delete))
		{
			return (response.getStatusLine().getStatusCode() == 200);
		}
	}
	
	/**
	 * Read the input stream in 4Mb chunks and upload making a new connection
	 * for each chunk.
	 * The data is not set line-by-line or broken in chunks on newline boundaries
	 * it is send in fixed size blocks. The API will manage reconstructing 
	 * the records from the chunks.
	 * 
	 * @param jobUrl Full path to job ie. <code>/api/job/{jobId}</code>
	 * @param inputStream The data to write to the web service
	 * @return
	 * @throws IOException 
	 */
	public boolean chunkedUpload(String jobUrl, FileInputStream inputStream) 
	throws IOException, InterruptedException
	{
		String postUrl = jobUrl + "/streaming/chunked_upload";	

		s_Logger.info("Uploading chunked data to " + postUrl);
		
		final int BUFF_SIZE = 4096 * 1024;
		byte [] buffer = new byte[BUFF_SIZE];
		int read = 0;
		int uploadCount = 0;
		while ((read = inputStream.read(buffer)) > -1)
		{
			ByteArrayEntity entity = new ByteArrayEntity(buffer, 0, read);
			entity.setContentType("application/octet-stream");
			
			s_Logger.info("Upload " + ++uploadCount);			
			
			HttpPost post = new HttpPost(postUrl);			
			post.setEntity(entity);
			CloseableHttpResponse response = m_HttpClient.execute(post);
			response.close(); // close connection
			
			// wait a little
			Thread.sleep(500);
		}
				
		return true;
	}
	
	/**
	 * Stream the input stream to the service
	 * 
	 * @param jobUrl Full path to job ie. <code>/api/job/{jobId}</code>
	 * @param inputStream The data to write to the web service
	 * @param compressed Is the data gzipped compressed?
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean streamingUpload(String jobUrl, InputStream inputStream,
			boolean compressed) 
	throws IOException
	{
		String postUrl = jobUrl + "/streaming";	
		s_Logger.info("Streaming data to " + postUrl);

		InputStreamEntity entity = new InputStreamEntity(inputStream);
		entity.setContentType("application/octet-stream");
		entity.setChunked(true);
	
		HttpPost post = new HttpPost(postUrl);	
		if (compressed)
		{
			post.addHeader("Content-Encoding", "gzip");
		}
		post.setEntity(entity);
		
        try (CloseableHttpResponse response = m_HttpClient.execute(post)) 
        {
            HttpEntity resEntity = response.getEntity();
            /*
            if (resEntity != null) 
            {
            	s_Logger.info("Response content length: " + resEntity.getContentLength());
            	s_Logger.info("Chunked?: " + resEntity.isChunked());
            }
            */
            
            EntityUtils.consume(resEntity);
        } 
		
		return true;
	}
	
	/**
	 * Finish the job after all the data has been uploaded
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @return
	 * @throws IOException
	 */
	public boolean closeStreamingJob(String jobUrl)
	throws IOException
	{
		// Send finish message
		String closeUrl = jobUrl + "/streaming/close";	
		HttpPost post = new HttpPost(closeUrl);		
		CloseableHttpResponse response = m_HttpClient.execute(post);
		response.close();
		
		return true;
	}
	
	
	/**
	 * Get the detectors used a in particular job.
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * 
	 * @return
	 * @throws IOException 
	 */
	public Pagination<Detector> getDetectors(String jobUrl) 
	throws IOException 
	{
		String url = jobUrl + "/detectors";
		
		HttpGet get = new HttpGet(url);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				Pagination<Detector> docs = m_JsonMapper.readValue(content, 
						new TypeReference<Pagination<Detector>>() {} );
				return docs;
			}
		}
		finally 
		{
			response.close();
		}	
		
		// else return empty page
		Pagination<Detector> page = new Pagination<>();
		page.setDocuments(Collections.<Detector>emptyList());
		return page;
	}
	
	
	/**
	 * Get the bucket results for a particular job.
	 * Calls {@link #getBuckets(String, boolean, Integer)} with the take
	 * parameter set to <code>null</code>
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @param expand If true return the anomaly records for the bucket
	 * 
	 * @return
	 * @throws IOException 
	 */
	public Pagination<Bucket> getBuckets(String jobUrl, boolean expand) 
	throws IOException 
	{
		return getBuckets(jobUrl, expand, null, null);
	}
			
	/**
	 * Get the bucket results for a particular job with paging parameters.
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @param expand If true return the anomaly records for the bucket
	 * @param skip The number of buckets to skip 
	 * @param take The max number of buckets to request. 
	 * 
	 * @return
	 * @throws IOException 
	 */
	public Pagination<Bucket> getBuckets(String jobUrl, boolean expand,
			Long skip, Long take) 
	throws IOException
	{
		return this.<String>getBuckets(jobUrl, expand, skip, take, null, null);
	}
	
	/**
	 * Get the bucket results filtered between the start and end dates.</br>
	 * The arguments are optional only one of start/end needs be set
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @param expand If true return the anomaly records for the bucket
	 * @param skip The number of buckets to skip. If <code>null</code> then ignored 
	 * @param take The max number of buckets to request. If <code>null</code> then ignored 
	 * @param start The start date filter as either a Long (seconds from epoch)
	 * or an ISO 8601 date String. If <code>null</code> then ignored
	 * @param end The end date filter as either a Long (seconds from epoch)
	 * or an ISO 8601 date String. If <code>null</code> then ignored
	 * @return
	 * @throws IOException
	 */
	public <T> Pagination<Bucket> getBuckets(String jobUrl, boolean expand,
				Long skip, Long take, T start, T end) 
	throws IOException
	{
		String url = jobUrl + "/results";
		char queryChar = '?';
		if (expand)
		{
			url += queryChar + "expand=true";
			queryChar = '&';
		}		
		if (skip != null)
		{
			url += queryChar + "skip=" + skip;
			queryChar = '&';
		}
		if (take != null)
		{
			url += queryChar + "take=" + take;
			queryChar = '&';
		}
		if (start != null)
		{
			url += queryChar + "start=" + URLEncoder.encode(start.toString(), "UTF-8");
			queryChar = '&';
		}
		if (end != null)
		{
			url += queryChar + "end=" + URLEncoder.encode(end.toString(), "UTF-8");
			queryChar = '&';
		}
		
		
		HttpGet get = new HttpGet(url);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				Pagination<Bucket> docs = m_JsonMapper.readValue(content, 
						new TypeReference<Pagination<Bucket>>() {} );
				return docs;
			}
		}
		finally 
		{
			response.close();
		}	
		
		// else return empty page
		Pagination<Bucket> page = new Pagination<>();
		page.setDocuments(Collections.<Bucket>emptyList());
		return page;
	}
		
	/**
	 * Get the bucket results for a particular job.
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @param bucketId The bucket to get
	 * @param expand If true return the anomaly records for the bucket
	 * @return
	 * @throws IOException 
	 */
	public SingleDocument<Bucket> getBucket(String jobUrl, String bucketId,
			boolean expand) 
	throws IOException
	{
		String url = jobUrl + "/results/" + bucketId;
		if (expand)
		{
			url += "?expand=true";
		}
		
		HttpGet get = new HttpGet(url);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				SingleDocument<Bucket> docs = m_JsonMapper.readValue(content, 
						new TypeReference<SingleDocument<Bucket>>() {} );
				return docs;
			}
		}
		finally 
		{
			response.close();
		}	
		
		// else return empty doc
		SingleDocument<Bucket> doc = new SingleDocument<>();
		return doc;
	}
	
	/**
	 * Get the anomaly records for the bucket. Records from all detectors are
	 * returned.
	 * This is similar to calling {@linkplain #getBucket(String, String, boolean)}
	 * with <code>expand=true</code> except the bucket isn't returned only the
	 * anomaly records.
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @param bucketId The bucket to get the anomaly records from
	 * @return
	 * @throws IOException 
	 */
	public Pagination<Map<String,Object>> getRecords(String jobUrl, String bucketId)
	throws IOException
	{
		String url = jobUrl + "/results/" + bucketId + "/records";
		
		HttpGet get = new HttpGet(url);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				Pagination<Map<String,Object>> docs = m_JsonMapper.readValue(content, 
						new TypeReference<Pagination<Map<String,Object>>>() {} );
				return docs;
			}
		}
		finally 
		{
			response.close();
		}	
		
		// else return empty page
		Pagination<Map<String,Object>> page = new Pagination<>();
		return page;
	}
	
	/**
	 * Get the anomaly records from a particular anomlay detector 
	 * in the given bucket.
	 * This is similar to {@linkplain #getRecords(String, String)} but only 
	 * the records produced by the detetor <code>detectorId</code> are returned.
	 * 
	 * @param jobUrl The URL for the job i.e <code>api/jobs/{jobid}</code>
	 * @param bucketId The bucket to get the anomaly records from
	 * @param detetorId Only get anomaly records from this detector
	 * @return
	 * @throws IOException 
	 */
	public Pagination<Map<String,Object>> getRecordByDetector(String jobUrl, 
			String bucketId, String detetorId)
	throws IOException
	{
		String url = jobUrl + "/results/" + bucketId + "/records/" + detetorId;
		
		HttpGet get = new HttpGet(url);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				Pagination<Map<String,Object>> docs = m_JsonMapper.readValue(content, 
						new TypeReference<Pagination<Map<String,Object>>>() {} );
				return docs;
			}
		}
		finally 
		{
			response.close();
		}	
		
		// else return empty page
		Pagination<Map<String,Object>> page = new Pagination<>();
		return page;
	}
	
	/**
	 * Generic get to the Url. The result is converted to the type
	 * referenced in <code>typeRef</code>. A <code>TypeReference</code> 
	 * has to be used to preserve the generic that is usually lost in
	 * erasure.
	 *  
	 * @param fullUrl
	 * @param typeRef
	 * @return A new T or <code>null</code>
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public <T> T get(String fullUrl, TypeReference<T> typeRef) 
	throws JsonParseException, JsonMappingException, IOException
	{
		HttpGet get = new HttpGet(fullUrl);
		get.addHeader("Content-Type", "application/json");
		
		CloseableHttpResponse response = m_HttpClient.execute(get);
		try
		{
			if (response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();				
				String content = EntityUtils.toString(entity);
				
				T docs = m_JsonMapper.readValue(content, typeRef);
				return docs;
			}
		}
		finally 
		{
			response.close();
		}	
		
		return null;		
	}
}