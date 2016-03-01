/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package com.prelert.rs.client;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.DataPostResponse;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

/**
 * A HTTP Client for the Prelert Engine RESTful API.
 *
 * <br>
 * Contains methods to create jobs, list jobs, upload data and query results.
 * <br>
 * Implements closeable so it can be used in a try-with-resource statement
 */
public class EngineApiClient implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(EngineApiClient.class);
    private static final int BUFFER_SIZE = 4096 * 1024;

    private final String m_BaseUrl;
    private final ObjectMapper m_JsonMapper;
    private final HttpClient m_HttpClient;
    private ApiError m_LastError;

    /**
     * Creates a new http client and Json object mapper.
     * Call {@linkplain #close()} once finished
     *
     * @param baseUrl The base URL for the REST API including version number
     * e.g <code>http://localhost:8080/engine/v2/</code>
     */
    public EngineApiClient(String baseUrl)
    {
        m_BaseUrl = baseUrl;
        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        }
        catch (Exception e)
        {
            LOGGER.fatal("Failed to start the HTTP client", e);
        }
        m_JsonMapper = new ObjectMapper();
        m_JsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Close the http client
     */
    @Override
    public void close() throws IOException
    {
        try
        {
            m_HttpClient.stop();
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Get details of all the jobs in database
     *
     * @return The {@link Pagination} object containing a list of {@link JobDetails jobs}
     * @throws IOException
     */
    public Pagination<JobDetails> getJobs()
    throws IOException
    {
        String url = m_BaseUrl + "/jobs";
        LOGGER.debug("GET jobs: " + url);

        Pagination<JobDetails> page = get(url, new TypeReference<Pagination<JobDetails>>() {});

        if (page == null)
        {
            page = new Pagination<>();
            page.setDocuments(Collections.<JobDetails>emptyList());
        }

        return page;
    }

    /**
     * Get the individual job on the provided URL
     *
     * @param jobId The Job's unique Id
     *
     * @return If the job exists a {@link com.prelert.rs.data.SingleDocument SingleDocument}
     * containing the {@link JobDetails job} is returned else the SingleDocument is empty
     * @throws IOException
     */
    public SingleDocument<JobDetails> getJob(String jobId)
    throws IOException
    {
        String url = m_BaseUrl + "/jobs/" + jobId;
        LOGGER.debug("GET job: " + url);

        SingleDocument<JobDetails> doc = get(url,
                new TypeReference<SingleDocument<JobDetails>>() {});

        if (doc == null)
        {
            doc = new SingleDocument<>();
        }
        return doc;
    }

    /**
     * Create a new Job from the <code>JobConfiguration</code> object.
     * <br>
     * Internally this function converts <code>jobConfig</code> to a JSON
     * string and calls {@link #createJob(String)}
     *
     * @param jobConfig the job configuration
     * @return The new job's Id or an empty string if there was an error
     * @throws IOException
     */
    public String createJob(JobConfiguration jobConfig) throws IOException
    {
        String payLoad = m_JsonMapper.writeValueAsString(jobConfig);
        return createJob(payLoad);
    }

    /**
     * Create a new job with the configuration in <code>createJobPayload</code>
     * and return the newly created job's Id
     *
     * @param createJobPayload The Json configuration for the new job
     * @return The new job's Id or an empty string if there was an error
     * @throws JsonMappingException
     * @throws JsonParseException
     * @throws IOException
     */
    public String createJob(String createJobPayload) throws JsonParseException,
            JsonMappingException, IOException

    {
        String url = m_BaseUrl + "/jobs";
        LOGGER.debug("Create job: " + url);

        Request request = m_HttpClient.POST(url)
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .header(HttpHeader.CONTENT_ENCODING, "UTF-8")
                .content(new StringContentProvider(createJobPayload));

        ContentResponse response = executeRequest(request);
        String content = response.getContentAsString();

        if (response.getStatus() == HttpStatus.CREATED_201)
        {
            Map<String, String> msg = m_JsonMapper.readValue(content,
                    new TypeReference<Map<String, String>>() {} );

            m_LastError = null;

            if (msg.containsKey("id"))
            {
                return msg.get("id");
            }
            else
            {
                LOGGER.error("Job created but no 'id' field in returned content");
                LOGGER.error("Response Content = " + content);
            }
        }
        else
        {
            String msg = String.format("Error creating job status code = %d. "
                    + "Returned content: %s", response.getStatus(), content);
            LOGGER.error(msg);
            m_LastError = m_JsonMapper.readValue(content, new TypeReference<ApiError>() {} );
        }

        return "";
    }


    /**
     * PUTS the description parameter to the job and sets it as
     * the job's new description field
     *
     * @param jobId The job's unique ID
     * @param description New description field
     *
     * @return True if the job description added successfully
     * @throws IOException
     */
    public boolean setJobDescription(String jobId, String description)
    throws IOException
    {
        String json = "{\"description\":\"" + description + "\"}";
        return updateJob(jobId, json);
    }

    /**
     * Submits a request to update a job
     *
     * @param jobId the id of the job to update
     * @param updateJson the JSON containing the fields to update and their new values
     * @return {@code true} if the update was successful, or {@code false} otherwise
     * @throws IOException
     */
    public boolean updateJob(String jobId, String updateJson) throws IOException
    {
        String url = m_BaseUrl + "/jobs/" + jobId + "/update";
        LOGGER.debug("PUT update job: " + url);

        Request request = m_HttpClient.newRequest(url)
                .method(HttpMethod.PUT)
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .header(HttpHeader.CONTENT_ENCODING, "UTF-8")
                .content(new StringContentProvider(updateJson));

        return executeRequest(request, "updating job");
    }

    private ContentResponse executeRequest(Request request) throws IOException
    {
        try
        {
            return request.send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("An error occurred while executing an HTTP request", e);
            throw new IOException(e);
        }
    }

    /**
     * Executes an HTTP request and checks if the response was OK. If not, it logs the error.
     *
     * @return True if response was OK, otherwise false.
     * @throws ExecutionException
     * @throws TimeoutException
     * @throws InterruptedException
     * @throws JsonMappingException
     * @throws JsonParseException
     * @throws IOException
     */
    private boolean executeRequest(Request request, String activityDescription)
            throws JsonParseException, JsonMappingException, IOException
    {
        ContentResponse response = executeRequest(request);

        if (response.getStatus() == HttpStatus.OK_200)
        {
            m_LastError = null;
            return true;
        }
        String content = response.getContentAsString();
        String msg = String.format("Error %s. Status code = %d, Returned content: %s",
                activityDescription, response.getStatus(), content);
        LOGGER.error(msg);
        m_LastError = m_JsonMapper.readValue(content, new TypeReference<ApiError>() {} );
        return false;
    }

    /**
     * Delete an individual job
     *
     * @param jobId The Job's unique Id
     * @return If the job existed and was deleted return true else false
     * @throws IOException
     */
    public boolean deleteJob(String jobId) throws IOException
    {
        String url = m_BaseUrl + "/jobs/" + jobId;
        LOGGER.debug("DELETE job: " + url);

        return executeRequest(m_HttpClient.newRequest(url).method(HttpMethod.DELETE),
                "deleting job");
    }

    /**
     * Read the input stream in 4Mb chunks and upload making a new connection
     * for each chunk.
     * The data is not set line-by-line or broken in chunks on newline
     * boundaries; it is sent in fixed size blocks. The API will manage
     * reconstructing the records from the chunks.
     *
     * @param jobId The Job's unique Id
     * @param inputStream The data to write to the web service
     * @return
     * @throws IOException
     * @see #streamingUpload(String, InputStream, boolean)
     */
    public MultiDataPostResult chunkedUpload(String jobId, InputStream inputStream)
            throws IOException
    {
        String postUrl = m_BaseUrl + "/data/" + jobId;
        LOGGER.debug("Uploading chunked data to " + postUrl);

        byte [] buffer = new byte[BUFFER_SIZE];
        int uploadCount = 0;
        MultiDataPostResult uploadSummary = new MultiDataPostResult();

        while (inputStream.read(buffer) > -1)
        {
            LOGGER.info("Upload " + ++uploadCount);

            Request request = m_HttpClient.POST(postUrl)
                    .header(HttpHeader.CONTENT_TYPE, "application/octet-stream")
                    .content(new BytesContentProvider(buffer));
            ContentResponse response = executeRequest(request);

            String content = response.getContentAsString();

            if (response.getStatus() != HttpStatus.ACCEPTED_202)
            {
                String msg = String.format(
                        "Upload of chunk %d failed, status code = %d. Returned content: %s",
                        uploadCount, response.getStatus(), content);

                LOGGER.error(msg);

                uploadSummary = m_JsonMapper.readValue(content, new TypeReference<MultiDataPostResult>() {});

                m_LastError = null;
                for (DataPostResponse dpr : uploadSummary.getResponses())
                {
                    if (dpr.getError() != null)
                    {
                        m_LastError = dpr.getError();
                        break;
                    }
                }
            }
            else
            {
                m_LastError = null;
                uploadSummary = m_JsonMapper.readValue(content, new TypeReference<MultiDataPostResult>() {});
            }
        }

        return uploadSummary;
    }

    /**
     * Stream data from <code>inputStream</code> to the service.
     * This is different to {@link #chunkedUpload(String, InputStream)}
     * in that the entire stream is read and uploading at once without breaking
     * the connection.
     *
     * @param jobId The Job's unique Id
     * @param inputStream The data to write to the web service
     * @param compressed Is the data gzipped compressed?
     * @return the multiple data upload results in {@linkplain MultiDataPostResult}
     * @throws IOException
     * @see #chunkedUpload(String, InputStream)
     */
    public MultiDataPostResult streamingUpload(String jobId, InputStream inputStream, boolean compressed)
    throws IOException
    {
        return streamingUpload(jobId, inputStream, compressed, "", "");
    }

    /**
     * Stream data from <code>inputStream</code> to the service.
     * This is different to {@link #chunkedUpload(String, InputStream)}
     * in that the entire stream is read and uploading at once without breaking
     * the connection.
     *
     * @param jobId The Job's unique Id
     * @param inputStream The data to write to the web service
     * @param compressed Is the data gzipped compressed?
     * @param resetStart The start of the time range to reset buckets for (inclusive)
     * @param resetEnd The end of the time range to reset buckets for (inclusive)
     * @return the multiple data upload results in {@linkplain MultiDataPostResult}
     * @throws IOException
     * @see #chunkedUpload(String, InputStream)
     */
    public MultiDataPostResult streamingUpload(String jobId, InputStream inputStream, boolean compressed,
            String resetStart, String resetEnd)
    throws IOException
    {
        String postUrl = String.format("%s/data/%s", m_BaseUrl, jobId);
        if (!isNullOrEmpty(resetStart) || !isNullOrEmpty(resetEnd))
        {
            postUrl += String.format("?resetStart=%s&resetEnd=%s",
                    nullToEmpty(resetStart), nullToEmpty(resetEnd));
        }
        return uploadStream(inputStream, postUrl, compressed, new MultiDataPostResult(), true,
                content -> m_JsonMapper.readValue(content, new TypeReference<MultiDataPostResult>() {}));
    }


    /**
     * Read data from <code>inputStream</code> and upload to multiple jobs
     * simultaneously. The response is a list of processed data counts/errors.
     *
     * @param jobIds The list of jobs to send the data to
     * @param inputStream The data to write to the web service
     * @param compressed Is the data gzipped compressed?
     * @return the multiple data upload results in {@linkplain MultiDataPostResult}
     * @throws IOException
     */
    public MultiDataPostResult streamingUpload(List<String> jobIds, InputStream inputStream,
                                            boolean compressed)
    throws IOException
    {
        StringJoiner joiner = new StringJoiner(",");
        for (String id : jobIds)
        {
            joiner.add(id);
        }

        String postUrl = String.format("%s/data/%s", m_BaseUrl, joiner.toString());

        return uploadStream(inputStream, postUrl, compressed, new MultiDataPostResult(), true,
                content -> m_JsonMapper.readValue(content, new TypeReference<MultiDataPostResult>() {}));
    }


    @FunctionalInterface
    private interface FunctionThatThrowsIoException<T, R>
    {
        R apply(T input) throws IOException;
    }

    private <T, E> T uploadStream(InputStream inputStream, String postUrl, boolean compressed,
            T defaultReturnValue, boolean convertResponseOnError,
            FunctionThatThrowsIoException<String, T> convertContentFunction)
    throws IOException
    {
        LOGGER.debug("Uploading data to " + postUrl);

        // It is possible that the server replies with an error and closes the stream.
        // In that case, there could be a case where a thread that writes into the inputStream
        // tries to write after the stream is closed. To avoid that, we make an asynchronous call
        // and we offer the inputStream in a deferred manner.

        CountDownLatch waitUntilRequestCompletesLatch = new CountDownLatch(1);
        AtomicInteger statusHolder = new AtomicInteger();
        DeferredContentProvider contentProvider = new DeferredContentProvider();
        Request request = m_HttpClient.POST(postUrl)
                .header(HttpHeader.CONTENT_TYPE, "application/octet-stream")
                .content(contentProvider);
        if (compressed)
        {
            request.header(HttpHeader.CONTENT_ENCODING, "gzip");
        }
        BufferingResponseListener responseListener = new BufferingResponseListener()
        {
            @Override
            public void onComplete(Result result)
            {
                statusHolder.getAndSet(result.getResponse().getStatus());
                waitUntilRequestCompletesLatch.countDown();
            }
        };
        request.send(responseListener);

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;
        while ((bytesRead = inputStream.read(buffer)) > -1)
        {
            contentProvider.offer(ByteBuffer.wrap(buffer, 0, bytesRead));
            buffer = new byte[BUFFER_SIZE];
        }
        contentProvider.close();

        try
        {
            waitUntilRequestCompletesLatch.await();
        }
        catch (InterruptedException e)
        {
            LOGGER.error(e);
            return defaultReturnValue;
        }

        String content = responseListener.getContentAsString();

        if (statusHolder.get() != HttpStatus.ACCEPTED_202)
        {
            String msg = String.format(
                    "Streaming upload failed, status code = %d. Returned content: %s",
                    statusHolder.get(), content);

            LOGGER.error(msg);

            if (convertResponseOnError)
            {
                return convertContentFunction.apply(content);
            }

            m_LastError = content.isEmpty() ? null : m_JsonMapper.readValue(content,
                    new TypeReference<ApiError>() {});
            return defaultReturnValue;
        }
        return convertContentFunction.apply(content);
    }

    /**
     * Upload the contents of <code>dataFile</code> to the server.
     *
     * @param jobId The Job's Id
     * @param dataFile Should match the data configuration format of the job
     * @param compressed Is the data gzipped compressed?
     * @return the multiple data upload results in {@linkplain MultiDataPostResult}
     * @throws IOException
     */
    public MultiDataPostResult fileUpload(String jobId, File dataFile, boolean compressed)
    throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);

        return streamingUpload(jobId, stream, compressed);
    }

    /**
     * Upload the contents of <code>dataFile</code> to the server.
     *
     * @param jobId The Job's Id
     * @param dataFile Should match the data configuration format of the job
     * @param compressed Is the data gzipped compressed?
     * @param resetStart The start of the time range to reset buckets for (inclusive)
     * @param resetEnd The end of the time range to reset buckets for (inclusive)
     * @return the multiple data upload results in {@linkplain MultiDataPostResult}
     * @throws IOException
     */
    public MultiDataPostResult fileUpload(String jobId, File dataFile, boolean compressed,
            String resetStart, String resetEnd) throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);

        return streamingUpload(jobId, stream, compressed, resetStart, resetEnd);
    }

    /**
     * Flush the job, ensuring that no previously uploaded data is waiting in
     * buffers.
     *
     * @param jobId The Job's unique Id
     * @param calcInterim Should interim results for the selected buckets be calculated
     * based on the partial data uploaded for it so far? Interim results will be calculated for
     * all available buckets (most recent bucket plus latency buckets if latency was specified).
     * @return True if successful
     * @throws IOException
     */
    public boolean flushJob(String jobId, boolean calcInterim) throws IOException
    {
        return flushJob(jobId, calcInterim, "", "");
    }

    /**
     * Flush the job, ensuring that no previously uploaded data is waiting in
     * buffers.
     *
     * @param jobId The Job's unique Id
     * @param calcInterim Should interim results for the selected buckets be calculated
     * based on the partial data uploaded for it so far? If both {@code start} and {@code end} are
     * empty, the default behaviour of calculating interim results for all available buckets
     * (most recent bucket plus latency buckets if latency was specified) will be assumed.
     * @param start The start of the time range to calculate interim results for (inclusive)
     * @param end The end of the time range to calculate interim results for (exclusive)
     * @return True if successful
     * @throws IOException
     */
    public boolean flushJob(String jobId, boolean calcInterim, String start, String end)
            throws IOException
    {
        // Send flush message
        String flushUrl = String.format(m_BaseUrl + "/data/%s/flush?calcInterim=%s&start=%s&end=%s",
                jobId, calcInterim ? "true" : "false", start, end);
        LOGGER.debug("Flushing job " + flushUrl);

        ContentResponse response = executeRequest(m_HttpClient.POST(flushUrl));
        String content = response.getContentAsString();

        if (response.getStatus() != HttpStatus.OK_200)
        {
            String msg = String.format(
                    "Error flushing job %s, status code = %d. Returned content: %s",
                    jobId, response.getStatus(), content);

            LOGGER.error(msg);
            m_LastError = m_JsonMapper.readValue(content,
                    new TypeReference<ApiError>() {} );
            return false;
        }
        else
        {
            m_LastError = null;
        }

        return true;
    }


    /**
     * Finish the job after all the data has been uploaded
     *
     * @param jobId The Job's unique Id
     * @return True if successful
     * @throws IOException
     */
    public boolean closeJob(String jobId)
    throws IOException
    {
        // Send finish message
        String closeUrl = m_BaseUrl + "/data/" + jobId + "/close";
        LOGGER.debug("Closing job " + closeUrl);

        ContentResponse response = executeRequest(m_HttpClient.POST(closeUrl));
        String content = response.getContentAsString();

        if (response.getStatus() != HttpStatus.ACCEPTED_202
                && response.getStatus() != HttpStatus.OK_200)
        {
            String msg = String.format(
                    "Error closing job %s, status code = %d. Returned content: %s",
                    jobId, response.getStatus(), content);
            LOGGER.error(msg);
            m_LastError = m_JsonMapper.readValue(content, new TypeReference<ApiError>() {});
            return false;
        }
        else
        {
            m_LastError = null;
        }

        return true;
    }

    /**
     * Returns a {@link BucketsRequestBuilder} for the given job through which
     * the request can be configured and executed
     *
     * @param jobId The jobId for which buckets are requested
     *
     * @return A {@link BucketsRequestBuilder}
     */
    public BucketsRequestBuilder prepareGetBuckets(String jobId)
    {
        return new BucketsRequestBuilder(this, jobId);
    }

    /**
     * Returns a {@link BucketRequestBuilder} for the given job through which
     * the request can be configured and executed
     *
     * @param jobId The jobId for which a bucket is requested
     *
     * @return A {@link BucketRequestBuilder}
     */
    public BucketRequestBuilder prepareGetBucket(String jobId, String bucketId)
    {
        return new BucketRequestBuilder(this, jobId, bucketId);
    }

    /**
     * Returns a {@link CategoryDefinitionsRequestBuilder} for the given job through which
     * the request can be configured and executed
     *
     * @param jobId The jobId for which category definitions are requested
     *
     * @return A {@link CategoryDefinitionsRequestBuilder}
     */
    public CategoryDefinitionsRequestBuilder prepareGetCategoryDefinitions(String jobId)
    {
        return new CategoryDefinitionsRequestBuilder(this, jobId);
    }

    /**
     * Returns a single document with the category definition that was requested
     *
     * @param jobId the job id
     * @param categoryId the job's category id
     *
     * @return A {@link SingleDocument} object containing the requested {@link CategoryDefinition}
     * object
     * @throws IOException
     */
    public SingleDocument<CategoryDefinition> getCategoryDefinition(String jobId, String categoryId)
            throws JsonMappingException, IOException
    {
        return new CategoryDefinitionRequestBuilder(this, jobId, categoryId).get();
    }

    /**
     * Returns a {@link InfluencersRequestBuilder} for the given job through which
     * the request can be configured and executed
     *
     * @param jobId The jobId for which influencers are requested
     *
     * @return A {@link InfluencersRequestBuilder}
     */
    public InfluencersRequestBuilder prepareGetInfluencers(String jobId)
    {
        return new InfluencersRequestBuilder(this, jobId);
    }

    /**
     * Returns a {@link RecordsRequestBuilder} for the given job through which
     * the request can be configured and executed
     *
     * @param jobId The jobId for which records are requested
     *
     * @return A {@link RecordsRequestBuilder}
     */
    public RecordsRequestBuilder prepareGetRecords(String jobId)
    {
        return new RecordsRequestBuilder(this, jobId);
    }

    /**
     * Stream data from <code>inputStream</code> to the preview service.
     *
     * @param jobId The Job's unique Id
     * @param inputStream The data to write to the web service
     * @return String The preview result
     * @throws IOException
     */
    public String previewUpload(String jobId, InputStream inputStream)
    throws IOException
    {
        String postUrl = String.format("%s/preview/%s", m_BaseUrl, jobId);
        return uploadStream(inputStream, postUrl, false, "", false, content -> content);
    }

    /**
     * Get the last 10 lines of the job's latest log file
     *
     * @param jobId The Job's unique Id
     * @return The last 10 lines of the last log file
     * @throws IOException
     */
    public String tailLog(String jobId) throws IOException
    {
        return tailLog(jobId, 10);
    }

    /**
     * Tails the last <code>lineCount</code> lines from the job's
     * last log file. This tails the autodetect process log file.
     *
     * @param jobId The Job's unique Id
     * @param lineCount The number of lines to return
     * @return The last <code>lineCount</code> lines of the log file
     * @throws IOException
     */
    public String tailLog(String jobId, int lineCount) throws IOException
    {
        String url = String.format("%s/logs/%s/tail?lines=%d",
                m_BaseUrl, jobId, lineCount);

        LOGGER.debug("GET tail log " + url);

        return getStringContent(url);
    }

    /**
     * Tails the last <code>lineCount</code> lines from the named log file.
     *
     * @param jobId The Job's unique Id
     * @param logfileName the name of the log file without the '.log' suffix.
     * @param lineCount The number of lines to return
     * @return The last <code>lineCount</code> lines of the log file
     * @throws IOException
     */
    public String tailLog(String jobId, String logfileName, int lineCount) throws IOException
    {
        String url = String.format("%s/logs/%s/%s/tail?lines=%d",
                m_BaseUrl, jobId, logfileName, lineCount);

        LOGGER.debug("GET tail log " + url);

        return getStringContent(url);
    }

    /**
     * Get content from Url and return as a string
     *
     * @param url
     * @return If status code == 200 return the HTTP response content
     * else return an empty string.
     * @throws IOException
     */
    private String getStringContent(String url) throws IOException
    {
        ContentResponse response = executeRequest(m_HttpClient.newRequest(url).method(HttpMethod.GET));
        String content = response.getContentAsString();

        if (response.getStatus() == HttpStatus.OK_200)
        {
            m_LastError = null;
            return content;
        }
        else
        {
            String msg = String.format(
                    "Error reading string content. Status code = %d. Returned content: %s",
                    response.getStatus(), content);
            LOGGER.error(msg);
            m_LastError = m_JsonMapper.readValue(content, new TypeReference<ApiError>() {});
            return "";
        }
    }

    /**
     * Download the specified log file for the job.
     * The autodetect process writes a log file named after the job id (&lt;job_id&gt;.log)
     * while the Java component logs to engine_api.log.
     *
     * @param jobId The Job's unique Id
     * @param logfileName the name of the log file without the '.log' suffix.
     * @return
     * @throws IOException
     */
    public String downloadLog(String jobId, String logfileName) throws IOException
    {
        String url = String.format("%s/logs/%s/%s",
                m_BaseUrl, jobId, logfileName);

        LOGGER.debug("GET log file " + url);

        return getStringContent(url);
    }


    /**
     * Download all the log files for the given job.
     *
     * <b>Important: the caller MUST close the ZipInputStream returned by
     * this method, otherwise all subsequent client/server communications
     * will be blocked.</b>
     *
     * @param jobId The Job's unique Id
     * @return A ZipInputStream for the log files. If an error occurred, the inputstream
     * may by empty or contain the server response. The caller MUST close this
     * ZipInputStream when they have finished with it.
     * @throws IOException
     */
    public ZipInputStream downloadAllLogs(String jobId) throws IOException
    {
        String url = String.format("%s/logs/%s", m_BaseUrl, jobId);

        LOGGER.debug("GET download logs " + url);

        m_LastError = null;
        InputStreamResponseListener responseListener = new InputStreamResponseListener();
        Request request = m_HttpClient.newRequest(url).method(HttpMethod.GET);
        request.send(responseListener);
        return new ZipInputStream(responseListener.getInputStream());
    }


    /**
     * A generic HTTP GET to any Url. The result is converted from Json to
     * the type referenced in <code>typeRef</code>. A <code>TypeReference</code>
     * has to be used to preserve the generic type information that is usually
     * lost in due to erasure.
     * <br>
     * If the response code is 200 or 404 try to parse the returned content
     * into an object of the generic parameter type <code>T</code>.
     * The 404 status code is not considered an error it simply means an
     * empty document was returned by the API.
     * <br>
     * This method is useful for paging through a set of results via the
     * next or previous page links in a {@link Pagination} object.
     *
     * @param fullUrl
     * @param typeRef
     * @return A new T or <code>null</code>
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @see get(URI, TypeReference)
     */
    public <T> T get(String fullUrl, TypeReference<T> typeRef)
    throws JsonParseException, JsonMappingException, IOException
    {
        return get(fullUrl, typeRef, false);
    }

    /**
     * A generic HTTP GET to any Url. The result is converted from Json to
     * the type referenced in <code>typeRef</code>. A <code>TypeReference</code>
     * has to be used to preserve the generic type information that is usually
     * lost in due to erasure.
     * <br>
     * If the response code is 200 or 404 try to parse the returned content
     * into an object of the generic parameter type <code>T</code>.
     * The 404 status code is not considered an error it simply means an
     * empty document was returned by the API.
     * <br>
     * This method is useful for paging through a set of results via the
     * next or previous page links in a {@link Pagination} object.
     *
     * @param uri
     * @param typeRef
     * @return A new T or <code>null</code>
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @see get(String, TypeReference)
     */
    public <T> T get(URI uri, TypeReference<T> typeRef)
    throws JsonParseException, JsonMappingException, IOException
    {
        return get(m_HttpClient.newRequest(uri).method(HttpMethod.GET), typeRef, false);
    }

    /**
     * A generic HTTP GET to any Url. The result is converted from Json to
     * the type referenced in <code>typeRef</code>. A <code>TypeReference</code>
     * has to be used to preserve the generic type information that is usually
     * lost in due to erasure.
     * <br>
     * If the response code is 200 or 404 try to parse the returned content
     * into an object of the generic parameter type <code>T</code>.
     * The 404 status code is not considered an error it simply means an
     * empty document was returned by the API.
     * <br>
     * This method is useful for paging through a set of results via the
     * next or previous page links in a {@link Pagination} object.
     *
     * @param url
     * @param typeRef
     * @return A new T or <code>null</code>
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @see get(String, TypeReference)
     */
    public <T> T get(String url, TypeReference<T> typeRef, boolean errorOn404)
    throws JsonParseException, JsonMappingException, IOException
    {
        return get(m_HttpClient.newRequest(url).method(HttpMethod.GET), typeRef, errorOn404);
    }

    private <T> T get(Request request, TypeReference<T> typeRef, boolean errorOn404)
    throws JsonParseException, JsonMappingException, IOException
    {
        ContentResponse response = executeRequest(request);
        String content = response.getContentAsString();

        // 404 errors return empty paging docs so still read them
        if (response.getStatus() == HttpStatus.OK_200
                || (response.getStatus() == HttpStatus.NOT_FOUND_404 && !errorOn404))
        {
            T docs = m_JsonMapper.readValue(content, typeRef);
            m_LastError = null;
            return docs;
        }
        else
        {
            String msg = String.format(
                    "GET returned status code %d for url %s. Returned content = %s",
                    response.getStatus(), request.getURI(), content);
            LOGGER.error(msg);
            m_LastError = m_JsonMapper.readValue(content, new TypeReference<ApiError>() {} );
        }

        return null;
    }

    /**
     * Get the last error message
     * @return The error or null if no errors have occurred
     */
    public ApiError getLastError()
    {
        return m_LastError;
    }

    public String getBaseUrl()
    {
        return m_BaseUrl;
    }

    private static boolean isNullOrEmpty(String string)
    {
        return string == null || string.isEmpty();
    }

    private static String nullToEmpty(String string)
    {
        return string == null ? "" : string;
    }
}
