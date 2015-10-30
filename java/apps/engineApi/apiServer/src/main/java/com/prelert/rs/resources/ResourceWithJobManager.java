/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.rs.resources;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.QueryPage;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.provider.RestApiException;
import com.prelert.server.info.ServerInfoFactory;

/**
 * Abstract resource class that knows how to access a
 * {@linkplain com.prelert.job.manager.JobManager}
 */
public abstract class ResourceWithJobManager
{
    // TODO This field is hidden in subclasses
    private static final Logger LOGGER = Logger.getLogger(ResourceWithJobManager.class);

    /**
     * The filter 'start' query parameter
     */
    public static final String START_QUERY_PARAM = "start";

    /**
     * The filter 'end' query parameter
     */
    public static final String END_QUERY_PARAM = "end";

    /**
     * Format string for the un-parseable date error message
     */
    public static final String BAD_DATE_FORMAT_MSG = "Error: Query param '%s' with value"
            + " '%s' cannot be parsed as a date or converted to a number (epoch)";

    /**
     * Application context injected by the framework
     */
    @Context
    private Application m_RestApplication;

    private JobManager m_JobManager;
    private AlertManager m_AlertManager;
    private ServerInfoFactory m_ServerInfo;

    /**
     *
     */
    @Context
    protected UriInfo m_UriInfo;

    /**
     * Get the job manager object from the application's set of singletons
     *
     * @return
     */
    protected JobManager jobManager()
    {
        if (m_JobManager != null)
        {
            return m_JobManager;
        }

        if (m_RestApplication == null)
        {
            LOGGER.error("Application context has not been set in "
                    + "the jobs resource");

            throw new IllegalStateException("Application context has not been"
                    + " set in the jobs resource");
        }

        Set<Object> singletons = m_RestApplication.getSingletons();
        for (Object obj : singletons)
        {
            if (obj instanceof JobManager)
            {
                m_JobManager = (JobManager)obj;
                break;
            }
        }

        if (m_JobManager == null)
        {
            LOGGER.error("Application singleton set doesn't contain an " +
                    "instance of JobManager");

            throw new IllegalStateException("Application singleton set doesn't "
                    + "contain an instance of JobManager");
        }

        return m_JobManager;
    }

    /**
     * Get the Alert manager object from the application's set of singletons
     *
     * @return
     */
    protected AlertManager alertManager()
    {
        if (m_AlertManager != null)
        {
            return m_AlertManager;
        }

        if (m_RestApplication == null)
        {
            LOGGER.error("Application context has not been set in "
                    + "the jobs resource");

            throw new IllegalStateException("Application context has not been"
                    + " set in the jobs resource");
        }

        Set<Object> singletons = m_RestApplication.getSingletons();
        for (Object obj : singletons)
        {
            if (obj instanceof AlertManager)
            {
                m_AlertManager = (AlertManager)obj;
                break;
            }
        }

        if (m_AlertManager == null)
        {
            String msg = "Application singleton set doesn't contain an " +
                    "instance of AlertManager";

            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }

        return m_AlertManager;
    }

    /**
     * Get the server info factory class
     * @return
     */
    protected ServerInfoFactory serverInfo()
    {
        if (m_ServerInfo != null)
        {
            return m_ServerInfo;
        }

        if (m_RestApplication == null)
        {
            LOGGER.error("Application context has not been set in "
                    + "the jobs resource");

            throw new IllegalStateException("Application context has not been"
                    + " set in the jobs resource");
        }

        Set<Object> singletons = m_RestApplication.getSingletons();
        for (Object obj : singletons)
        {
            if (obj instanceof ServerInfoFactory)
            {
                m_ServerInfo = (ServerInfoFactory)obj;
                break;
            }
        }

        if (m_ServerInfo == null)
        {
            String msg = "Application singleton set doesn't contain an " +
                    "instance of ServerInfoFactory";

            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }

        return m_ServerInfo;
    }


    /**
     * Set the previous and next page URLs if appropriate.
     * If there are more hits than the take value is set to the results
     * will be paged else the next and previous page URLs will be
     * <code>null</code>
     *
     * @param path
     * @param page
     */
    protected void setPagingUrls(String path, Pagination<?> page)
    {
        setPagingUrls(path, page, Collections.<KeyValue>emptyList());
    }

    /**
     * Set the previous and next page URLs if appropriate.
     * If there are more hits than the take value is set to the results
     * will be paged else the next and previous page URLs will be
     * <code>null</code>. The list of extra query parameters will be
     * added to the paging Urls.
     *
     * @param path
     * @param page
     * @param queryParams List of extra query parameters
     */
    protected void setPagingUrls(String path, Pagination<?> page,
            List<KeyValue> queryParams)
    {
        if (page.isAllResults() == false)
        {
            // Is there a next page of results
            int remaining = (int)page.getHitCount() -
                    (page.getSkip() + page.getTake());
            if (remaining > 0)
            {
                int nextPageStart = page.getSkip() + page.getTake();
                UriBuilder uriBuilder = m_UriInfo.getBaseUriBuilder()
                        .path(path)
                        .queryParam("skip", nextPageStart)
                        .queryParam("take", page.getTake());
                for (KeyValue pair : queryParams)
                {
                    uriBuilder.queryParam(pair.getKey(), pair.getValue());
                }

                URI nextUri = uriBuilder.build();

                page.setNextPage(nextUri);
            }

            // previous page
            if (page.getSkip() > 0)
            {
                int prevPageStart = Math.max(0, page.getSkip() - page.getTake());

                UriBuilder uriBuilder = m_UriInfo.getBaseUriBuilder()
                        .path(path)
                        .queryParam("skip", prevPageStart)
                        .queryParam("take", page.getTake());

                for (KeyValue pair : queryParams)
                {
                    uriBuilder.queryParam(pair.getKey(), pair.getValue());
                }

                 URI prevUri = uriBuilder.build();

                page.setPreviousPage(prevUri);
            }
        }
    }

    /**
     * If the date is an empty string, it returns 0. Otherwise, it
     * first tries to parse the date first as a Long and convert that
     * to an epoch time. If the long number has more than 10 digits
     * it is considered a time in milliseconds else if 10 or less digits
     * it is in seconds. If that fails it tries to parse the string
     * using one of the DateFormats in {@link #DATE_FORMATS}.
     *
     * If the date string cannot be parsed a {@link RestApiException} is thrown.
     *
     * @param paramName The name of the parameter being parsed
     * @param date The value to be parsed
     * @return The epoch time in milliseconds or 0 if the date is empty
     * @throws RestApiException if the date cannot be parsed
     */
    protected long paramToEpochIfValidOrThrow(String paramName, String date, Logger logger)
    {
        long epochStart = 0;
        if (date.isEmpty() == false)
        {
            epochStart = paramToEpoch(date);
            if (epochStart == 0) // could not be parsed
            {
                String msg = String.format(BAD_DATE_FORMAT_MSG, paramName, date);
                logger.info(msg);
                throw new RestApiException(msg, ErrorCodes.UNPARSEABLE_DATE_ARGUMENT,
                        Response.Status.BAD_REQUEST);
            }
        }
        return epochStart;
    }

    /**
     * First tries to parse the date first as a Long and convert that
     * to an epoch time. If the long number has more than 10 digits
     * it is considered a time in milliseconds else if 10 or less digits
     * it is in seconds. If that fails it tries to parse the string
     * using one of the DateFormats in {@link #DATE_FORMATS}.
     *
     * If the date string cannot be parsed 0 is returned.
     *
     * @param date
     * @return The epoch time in milliseconds or 0 if the date cannot be parsed.
     */
    private long paramToEpoch(String date)
    {
        try
        {
            long epoch = Long.parseLong(date);
            if (date.trim().length() <= 10) // seconds
            {
                return epoch * 1000;
            }
            else
            {
                return epoch;
            }
        }
        catch (NumberFormatException nfe)
        {
            // not a number
        }

        // try parsing as a date string
        try
        {
            TemporalAccessor parsed = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date);
            return Instant.from(parsed).toEpochMilli();
        }
        catch (DateTimeParseException pe)
        {
            // not a date
        }

        // Could not do the conversion
        return 0;
    }

    @VisibleForTesting
    protected void setApplication(Application application)
    {
        m_RestApplication = application;
    }

    @VisibleForTesting
    protected void setUriInfo(UriInfo uriInfo)
    {
        m_UriInfo = uriInfo;
    }

    /**
     * Simple class to pair key, value strings
     */
    protected class KeyValue
    {
        private String m_Key;
        private String m_Value;

        public KeyValue(String key, String value)
        {
            m_Key = key;
            m_Value = value;
        }

        public String getKey()
        {
            return m_Key;
        }

        public String getValue()
        {
            return m_Value;
        }
    }

    /**
     * Utility method for creating generic pagination objects
     * @param page
     * @param skip
     * @param take
     * @return
     */
    protected <T> Pagination<T> paginationFromQueryPage(QueryPage<T> page, int skip, int take)
    {
        Pagination<T> pagination = new Pagination<T>();
        pagination.setDocuments(page.queryResults());
        pagination.setHitCount(page.hitCount());
        pagination.setSkip(skip);
        pagination.setTake(take);

        return pagination;
    }

    /**
     * Utility method for creating generic single document objects
     * @param opt
     * @param docId
     * @param docType
     * @return
     */
    protected <T> SingleDocument<T> singleDocFromOptional(Optional<T> opt, String docId, String docType)
    {
        SingleDocument<T> doc = new SingleDocument<T>();
        doc.setExists(opt.isPresent());
        if (opt.isPresent())
        {
            doc.setDocument(opt.get());
        }

        doc.setDocumentId(docId);
        doc.setType(docType);

        return doc;
    }
}
