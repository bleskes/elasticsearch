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
package com.prelert.jetty;


import java.io.File;
import java.util.EnumSet;
import java.util.ResourceBundle;

import javax.servlet.DispatcherType;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.servlet.ServletProperties;

import com.prelert.job.messages.Messages;

/**
 * Instantiate and configure an embedded Jetty Server in Java.
 * Useful for running & debugging Jetty applications in an IDE
 *
 * The String RESOURCE_PACKAGE is the Java package containing the
 * web resources or set APPLICATION_CLASS to a class extending
 * javax.ws.rs.core.Application and set the servlet to use that.
 */
public class ServerMain
{
	private static final Logger LOGGER = Logger.getLogger(ServerMain.class);
	/**
	 * The web service resources
	 */
	public static final String RESOURCE_PACKAGE = "com.prelert.rs.resources";
	public static final String APPLICATION_CLASS = "com.prelert.rs.resources.PrelertWebApp";

	/**
	 * Base URI, all web service endpoints should match this path
	 */
	public static final String BASE_PATH = "/engine/v1/*";

	/**
	 * The default port the server will run on.
	 * Override the default value by setting the {@value #JETTY_PORT_PROPERTY}
	 * system property.
	 */
	public static final int JETTY_PORT = 8080;

	private static final String JETTY_PORT_PROPERTY = "jetty.port";
	private static final String JETTY_HOME_PROPERTY = "jetty.home";

	/**
	 * The server - held as a static member variable to allow close() to
	 * access it
	 */
	private static Server ms_Server;


	private ServerMain()
	{
	}

	public static void main(String[] args)
	throws Exception
	{


		int jettyPort = JETTY_PORT;
		try
		{
			String portProp = System.getProperty(JETTY_PORT_PROPERTY);
			if (portProp == null)
			{
				LOGGER.info("Using default port " + JETTY_PORT);
			}
			else
			{
				jettyPort = Integer.parseInt(portProp);
				LOGGER.info("Using port " + jettyPort);
			}
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn(String.format("Error parsing %s property value '%s' "
					+ "cannot not be parsed as an integer",
					JETTY_PORT_PROPERTY, System.getProperty(JETTY_PORT_PROPERTY)));

			LOGGER.info("Using default port " + JETTY_PORT);
		}

		String jettyHome = System.getProperty(JETTY_HOME_PROPERTY);
		if (jettyHome == null)
		{
			LOGGER.info("Using default " + JETTY_HOME_PROPERTY +
							" of current directory");
			jettyHome = ".";
		}

		// load the resources here so they are cached
		ResourceBundle.getBundle(Messages.BUNDLE_NAME);

		ms_Server = new Server(jettyPort);

		// This serves the Kibana-based dashboard.
		ResourceHandler dashboardHandler = new ResourceHandler();
		dashboardHandler.setResourceBase(jettyHome + File.separator + "webapps");

		// The true argument here lets us add more handlers to the running
		// server
		HandlerCollection handlerCollection = new HandlerCollection(true);
		handlerCollection.setHandlers(new Handler[] { dashboardHandler });

		ms_Server.setHandler(handlerCollection);

		// We start the server before adding the API handler so that the static
		// content is available immediately
		ms_Server.start();

		// This serves the Engine API
		ServletContextHandler contextHandler = new ServletContextHandler();
		contextHandler.setContextPath("/");
		contextHandler.setErrorHandler(new ApiErrorHandler());

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

		// Block until the server stops (otherwise the whole JVM would shut down
		// prematurely when main() exited)
		ms_Server.join();
	}


	/**
	 * Used to stop the Windows service.  (On Unix we just kill the
	 * process.)
	 */
	public static void close(String[] args)
	throws Exception
	{
		if (ms_Server != null)
		{
			ms_Server.stop();
			ms_Server = null;
		}
	}
}
