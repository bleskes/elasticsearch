package com.prelert.rs.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.JobInUseException;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.provider.RestApiException;
import com.prelert.rs.streaminginterceptor.StreamingInterceptor;


/**
 * Streaming data endpoint
 * 
 * <pre>curl -X POST 'http://localhost:8080/api/data/<jobid>/' --data @<filename></pre>
 * <br/>
 * Binary gzipped files must be POSTed with the --data-binary option 
 * <pre>curl -X POST 'http://localhost:8080/api/data/<jobid>/' --data-binary @<filename.gz></pre>
 *
 */
@Path("/data")
public class Data extends ResourceWithJobManager
{   
	static final private Logger s_Logger = Logger.getLogger(Data.class);
	
	/**
	 * Persisted data files are named with this date format
	 * e.g. Tue_22_Apr_2014_091033
	 */
	static final private SimpleDateFormat s_PersistedFileNameDateFormat = 
			new SimpleDateFormat("EEE_d_MMM_yyyy_HHmmss");
	
	/**
	 * The name of this endpoint
	 */
	static final public String ENDPOINT = "data";
	
	private boolean m_isPersistData;
	private String m_BaseDirectory;  
	
	public Data()
	{
		// should we save uploaded data and where
		m_BaseDirectory = System.getProperty("persistbasedir");
		m_isPersistData = m_BaseDirectory != null;	
	}


	/**
	 * Data upload endpoint.
	 * 
	 * @param headers
	 * @param jobId
	 * @param input
	 * @return
	 * @throws IOException
	 * @throws UnknownJobException
	 * @throws NativeProcessRunException
	 * @throws MissingFieldException
     * @throws JobInUseException if the data cannot be written to because 
	 * the job is already handling data
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 * @throws TooManyJobsException If the license is violated
	 */
    @POST
    @Path("/{jobId}")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON,
    	MediaType.APPLICATION_OCTET_STREAM})
    public Response streamData(@Context HttpHeaders headers,
    		@PathParam("jobId") String jobId, InputStream input)  
    throws IOException, UnknownJobException, NativeProcessRunException,
    	MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
    	OutOfOrderRecordsException, TooManyJobsException
    {   	   	
    	s_Logger.debug("Handle Post data to job = " + jobId);
    	
    	String contentEncoding = headers.getHeaderString(HttpHeaders.CONTENT_ENCODING);
    	if (contentEncoding!= null && contentEncoding.equals("gzip"))
    	{
    		s_Logger.info("Decompressing post data in job = " + jobId);
    		try
    		{
    			input = new GZIPInputStream(input);
    		}
    		catch (ZipException ze)
    		{
    			throw new RestApiException("Content-Encoding = gzip "
    					+ "but the data is not in gzip format",
    					ErrorCode.UNCOMPRESSED_DATA,
    					Response.Status.BAD_REQUEST);
    		}
    	}
    	
    	
    	if (m_isPersistData)
    	{   	
    		try
    		{
    			Files.createDirectory(FileSystems.getDefault().getPath(
    					m_BaseDirectory, jobId));
    		}
    		catch (FileAlreadyExistsException e)
    		{
    			// continue
    		}
    		
    		java.nio.file.Path filePath = FileSystems.getDefault().getPath(
    				m_BaseDirectory, jobId, s_PersistedFileNameDateFormat.format(new Date()) + ".gz"); 
    		
    		s_Logger.info("Data will be persisted to: " + filePath);
    		
    		// Create the interceptor for writing data to disk 
    		// and start running in a new thread.
    		final StreamingInterceptor si = new StreamingInterceptor(filePath);
    		final InputStream uploadStream = input;
    		
    		input = si.createStream();
    		
    		new Thread() {
    			@Override
    			public void run()
    			{
    				si.pump(uploadStream);
    			}
    		}.start();
    	}
    	
   		handleStream(jobId, input);    
    	
    	s_Logger.debug("File uploaded to job " + jobId);
    	return Response.accepted().build();
    }
     
    
    /**
     * Calling this endpoint indicates that data transfer is complete.
     * The job is retired and cleaned up after this
     * @param jobId
     * @return
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException 
     */
    @Path("/{jobId}/close")
    @POST
    public Response commitUpload(@PathParam("jobId") String jobId) 
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {   	
    	s_Logger.debug("Post to close data upload for job " + jobId);

    	JobManager manager = jobManager();
    	manager.finishJob(jobId);
    	
   		s_Logger.debug("Process finished successfully, Job Id = '" + jobId + "'");    		
   		return Response.accepted().build();
    }
    
    /**
     * Pass the data stream to the native process. 
     *    
     * @param jobId
     * @param input
     * @return 
     * 
	 * @throws NativeProcessRunException If there is an error starting the native 
	 * process
	 * @throws UnknownJobException If the jobId is not recognised
	 * @throws MissingFieldException If a configured field is missing from 
	 * the CSV header
     * @throws JsonParseException 
     * @throws JobInUseException if the data cannot be written to because 
	 * the job is already handling data
     * @throws HighProportionOfBadTimestampsException 
     * @throws OutOfOrderRecordsException 
	 * @throws TooManyJobsException If the license is violated
	 */
    private boolean handleStream(String jobId, InputStream input)
    throws NativeProcessRunException, UnknownJobException, MissingFieldException, 
    JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
    OutOfOrderRecordsException, TooManyJobsException
    {
    	JobManager manager = jobManager();
		return manager.dataToJob(jobId, input);
    }
}
