/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
package com.prelert.rs.client.integrationtests;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;


/**
 * Download the support bundle from the API and check it contains
 * certain files and those files are not empty.
 */
public class SupportBundleTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(SupportBundleTest.class);

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final HttpClient m_HttpClient;

    private SupportBundleTest()
    {
        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        }
        catch (Exception e)
        {
            LOGGER.fatal("Failed to start the HTTP client", e);
        }
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

    public ZipInputStream downloadSupportBundle(String baseUrl) throws IOException
    {
        String url = String.format("%s/support", baseUrl);

        LOGGER.info("GET support bundle: " + url);

        InputStreamResponseListener responseListener = new InputStreamResponseListener();
        Request request = m_HttpClient.newRequest(url).method(HttpMethod.GET);
        request.send(responseListener);
        return new ZipInputStream(responseListener.getInputStream());
    }

    private void checkZipContents(ZipInputStream zip) throws IOException
    {
        class NameSize
        {
            int m_Size;
            String m_Name;

            NameSize(String name, int size)
            {
                m_Name = name;
                m_Size = size;
            }
        }

        List<NameSize> filePaths = new ArrayList<>();
        byte buffer[] = new byte[2048];

        ZipEntry entry = zip.getNextEntry();
        test(entry != null);
        do
        {
            filePaths.add(new NameSize(entry.getName(), zip.read(buffer)));
            entry = zip.getNextEntry();
        }
        while (entry != null);


        final String [] requiredFiles = {"elasticsearch_info.log",
                                        "basic_info.log",
                                        "engine_api_info.log"};

        for (String file : requiredFiles)
        {
            boolean found = false;

            for (NameSize ns : filePaths)
            {
                if (ns.m_Name.contains(file))
                {
                    found = true;
                    LOGGER.info(ns.m_Name + " : " + ns.m_Size);

                    if (ns.m_Size <= 0)
                    {
                        LOGGER.error(file + " is empty");
                        test(false);
                    }
                }
            }

            if (found == false)
            {
                LOGGER.error(file + " missing from the support download");
                test(found);
            }
        }
    }

    /**
     * Throws an exception if <code>condition</code> is false.
     *
     * @param condition
     * @throws IllegalStateException
     */
    public static void test(boolean condition)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
    }

    private static void configureLogging()
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    public static void main(String[] args) throws IOException
    {
        configureLogging();

        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        SupportBundleTest test = new SupportBundleTest();
        try (ZipInputStream zip = test.downloadSupportBundle(baseUrl))
        {
            test.checkZipContents(zip);
        }
        test.close();

        LOGGER.info("Download support bundle test passed");
    }

}
