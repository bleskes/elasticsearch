package com.prelert.rs.resources;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.JobInUseException;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.resources.data.DataStreamer;


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
     * The name of this endpoint
     */
	static final public String ENDPOINT = "data";

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
        DataStreamer dataStreamer = new DataStreamer(jobManager());
        dataStreamer.streamData(headers, jobId, input);
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
}
