package com.prelert.rs.resources;

import java.io.IOException;
import java.io.InputStream;
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

import com.prelert.job.JobManager;
import com.prelert.job.NativeProcessRunException;
import com.prelert.job.UnknownJobException;
import com.prelert.rs.provider.RestApiException;


/**
 * Streaming data endpoint
 * 
 * <pre>curl -X POST 'http://localhost:8080/api/jobs/<jobid>/streaming/' --data @<filename></pre>
 * <br/>
 * Binary gzipped files must be POSTed with the --data-binary option 
 * <pre>curl -X POST 'http://localhost:8080/api/jobs/<jobid>/streaming/' --data-binary @<filename.gz></pre>
 *
 */
@Path("/jobs/{jobId}/streaming")
public class Streaming extends ResourceWithJobManager
{   
	static final private Logger s_Logger = Logger.getLogger(Streaming.class);
	
	/**
	 * The name of this endpoint
	 */
	static public final String ENDPOINT = "streaming";


    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON,
    	MediaType.APPLICATION_OCTET_STREAM})
    public Response streamData(@Context HttpHeaders headers,
    		@PathParam("jobId") String jobId, InputStream input)  
    throws IOException, UnknownJobException, NativeProcessRunException
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
    					Response.Status.BAD_REQUEST);
    		}
    	}
    	
    	try
    	{
    		handleStream(jobId, input);    
    	} 
    	catch (NativeProcessRunException e) 
    	{
    		s_Logger.error("Error sending data to job " + jobId, e);
    		throw e;
    	}	 
    	
    	s_Logger.debug("File uploaded to job " + jobId);
    	return Response.accepted().build();
    }
     
    @Path("/chunked_upload")
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    public Response chunkedUpload(@PathParam("jobId") String jobId, InputStream input)
    throws UnknownJobException, NativeProcessRunException
    {
    	s_Logger.debug("Handle Post of chunked data to job = " + jobId);
    	
    	try
    	{
    		handleStream(jobId, input);    
    	} 
    	catch (NativeProcessRunException e) 
    	{
    		s_Logger.error("Error sending data to job " + jobId, e);
    		throw e;
    	}
    	
    	s_Logger.debug("Uploaded chunk to job " + jobId);    	    	
    	return Response.accepted().build();
    }
    
    
    @Path("/close")
    @POST
    public Response commitChunkedUpload(@PathParam("jobId") String jobId) 
    throws UnknownJobException, NativeProcessRunException
    {   	
    	s_Logger.debug("Post to close data upload for job " + jobId);

    	JobManager manager = jobManager();
    	boolean fin = manager.finishJob(jobId);
    	
    	if (fin)
    	{
    		s_Logger.debug("Process finished successfully, Job Id = '" + jobId + "'");    		
    		return Response.accepted().build();
    	}
    	else
    	{
    		s_Logger.error("Error closing job '" + jobId + "'");
    		return Response.serverError().build();
    	}
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
	 */
    private boolean handleStream(String jobId, InputStream input)
    throws NativeProcessRunException, UnknownJobException	
    {
    	JobManager manager = jobManager();
		return manager.dataToJob(jobId, input);
    }

}
