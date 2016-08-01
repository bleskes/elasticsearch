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
package com.prelert.master;


import java.net.BindException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.glassfish.jersey.servlet.ServletProperties;

import com.prelert.settings.PrelertSettings;

/**
 * Instantiate and configure an embedded Jetty Server in Java.
 *
 * This is the heart of the Engine API.  It is the entry point
 * on all platforms, and also the trigger for shutting down when
 * running as a Windows service.  (On *nix we shut down by
 * sending a SIGTERM.)
 *
 * The String RESOURCE_PACKAGE is the Java package containing the
 * web resources or set APPLICATION_CLASS to a class extending
 * javax.ws.rs.core.Application and set the servlet to use that.
 */
public class MasterMain
{
    private static final Logger LOGGER = Logger.getLogger(MasterMain.class);
    /**
     * The web service resources
     */
    public static final String RESOURCE_PACKAGE = "com.prelert.master.rs.resources";
    public static final String APPLICATION_CLASS = "com.prelert.master.rs.resources.MasterApiWebApp";

    /**
     * Base URI, all web service endpoints should match this path
     */
    public static final String BASE_PATH = "/engine-master/v2/*";

    /**
     * The default port the server will run on.
     * Override the default value by setting the {@value #JETTY_PORT_PROPERTY}
     * system property.
     */
    public static final int JETTY_PORT = 8080;

    private static final String JETTY_PORT_PROPERTY = "jetty.port";

    /**
     * The server - held as a static member variable to allow close() to
     * access it
     */
    private static Server ms_Server;


    private MasterMain()
    {
    }

    public static void main(String[] args)
    throws Exception
    {
        // Tell Jetty to log to our log4j framework, which it does via slf4j.
        // (The bit that tells slf4j that it's a proxy for log4j is the Maven
        // dependency on slf4j-log4j12.)
        Log.setLog(new Slf4jLog());

        int jettyPort = PrelertSettings.getSettingOrDefault(JETTY_PORT_PROPERTY, JETTY_PORT);
        LOGGER.info("Using port " + jettyPort);

        ms_Server = new Server(jettyPort);

        // Set up an access log for Jetty
        String logdir = PrelertSettings.getSettingOrDefault(PrelertSettings.PRELERT_LOGS_PROPERTY, "");
        NCSARequestLog requestLogger = new NCSARequestLog(logdir + "/master_api/jetty_access.log.yyyy_mm_dd");
        requestLogger.setExtended(true);
        requestLogger.setLogLatency(true);
        ms_Server.setRequestLog(requestLogger);


        // The true argument here lets us add more handlers to the running
        // server
        HandlerCollection handlerCollection = new HandlerCollection(true);
        ms_Server.setHandler(handlerCollection);

        // We start the server before adding the API handler so that the static
        // content is available immediately
        try
        {
            ms_Server.start();
        }
        catch (BindException e)
        {
            LOGGER.error("Error binding to port " + jettyPort, e);
            ms_Server.stop();
            System.exit(1);
        }

        if (ms_Server.isFailed())
        {
            LOGGER.error("Failed to start server");
            ms_Server.stop();
            System.exit(2);
        }


        // This serves the Master Engine API
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
//        contextHandler.setErrorHandler(new ApiErrorHandler());

        // Add cross origin accept filter, using wildcard '*' for origins
        // and explicitly allowing GET,POST,DELETE,PUT and HEAD http methods.
        CrossOriginFilter crossOrigin = new CrossOriginFilter();
        FilterHolder filterHolder = new FilterHolder(crossOrigin);
        filterHolder.setInitParameter("allowedMethods", "GET,POST,DELETE,PUT,HEAD");
        filterHolder.setInitParameter("allowedOrigins", "*");
        contextHandler.addFilter(filterHolder, "/*",
                EnumSet.of(DispatcherType.REQUEST));

        ServletHolder jerseyServlet = contextHandler.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class,
                BASE_PATH);
        jerseyServlet.setInitOrder(1);
        jerseyServlet.setAsyncSupported(true);

        // Set the application class
        jerseyServlet.setInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS,
                APPLICATION_CLASS);

        // Add the context handler to the collection already being served by the
        // running server
        handlerCollection.addHandler(contextHandler);

        // Handlers added after server start have to be explicitly started.  For
        // some reason we used to get away without doing this in Jetty 9.1, but
        // it's essential in Jetty 9.3.
        try
        {
            // Stop the server if an exception here
            contextHandler.start();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to start web application", e);
            ms_Server.stop();
            ms_Server.join();
            System.exit(3);
        }

        LOGGER.info("Master Engine API now serving REST endpoints");

        // Block until the server stops (otherwise the whole JVM would shut down
        // prematurely when main() exited)
        ms_Server.join();

        // Although it's generally considered bad practice to call System.exit(),
        // we need the shutdown hooks to execute here otherwise running jobs
        // won't be closed and the Elasticsearch connection won't be gracefully
        // shut down, and this could lead to data loss.  (Simply exiting from
        // this main() function doesn't cut the mustard, as the Elasticsearch
        // client has non-daemon threads that will keep the JVM alive and prevent
        // shutdown hooks from running.)
        LOGGER.info("Master Engine API about to exit");
        System.exit(0);
    }


    /**
     * Used to stop the Windows service.  (On Unix we just kill the process.)
     * @param args Not used, but required for methods called by the native
     * Apache Commons Daemon service manager.
     */
    public static void close(String[] args)
    throws Exception
    {
        if (ms_Server != null)
        {
            LOGGER.info("Stopping Jetty");
            ms_Server.stop();
            ms_Server = null;
            LOGGER.info("Jetty stopped");
        }
        else
        {
            LOGGER.warn("Received request to stop Jetty when it was not started");
        }
    }
}
