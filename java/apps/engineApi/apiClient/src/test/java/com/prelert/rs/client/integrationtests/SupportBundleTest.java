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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;


/**
 * Download the support bundle from the API and check it contains
 * certain files and those files are not empty.
 */
public class SupportBundleTest extends BaseIntegrationTest
{
    private final HttpClient m_HttpClient;

    private SupportBundleTest(String baseUrl)
    {
        super(baseUrl);

        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        }
        catch (Exception e)
        {
            m_Logger.fatal("Failed to start the HTTP client", e);
        }
    }

    @Override
    protected void runTest() throws IOException
    {
        try (ZipInputStream zip = downloadSupportBundle(m_BaseUrl))
        {
            checkZipContents(zip);
        }
    }

    /**
     * Close the http client
     */
    @Override
    public void close() throws IOException
    {
        super.close();

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

        m_Logger.info("GET support bundle: " + url);

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
                    m_Logger.info(ns.m_Name + " : " + ns.m_Size);

                    if (ns.m_Size <= 0)
                    {
                        m_Logger.error(file + " is empty");
                        test(false);
                    }
                }
            }

            if (found == false)
            {
                m_Logger.error(file + " missing from the support download");
                test(found);
            }
        }
    }


    public static void main(String[] args) throws IOException
    {
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        try (SupportBundleTest test = new SupportBundleTest(baseUrl))
        {
            test.runTest();
            test.m_Logger.info("Download support bundle test passed");
        }

    }

}
