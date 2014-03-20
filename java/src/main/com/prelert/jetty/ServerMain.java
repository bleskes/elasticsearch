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
package com.prelert.jetty;


import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletProperties;

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
	private static final Logger s_Logger = Logger.getLogger(ServerMain.class);
	/**
	 * The web service resources
	 */
	public static final String RESOURCE_PACKAGE = "com.prelert.rs.resources";
	public static final String APPLICATION_CLASS = "com.prelert.rs.resources.PrelertWebApp";

	/**
	 * Base URI, all web service endpoints should match this path
	 */
	public static final String BASE_PATH = "/engine/v0.3/*";
	
	/**
	 * The default port the server will run on.
	 * Override the default value by setting the {@value #JETTY_PORT_PROPERTY} 
	 * system property.
	 */
	public static final int JETTY_PORT = 8080;
	
	private static final String JETTY_PORT_PROPERTY = "jetty.port";
	
	public static void main(String[] args) 
	throws Exception 
	{
		int jettyPort = JETTY_PORT;
		try
		{
			String portProp = System.getProperty(JETTY_PORT_PROPERTY);
			if (portProp == null)
			{
				s_Logger.info("Using default port " + JETTY_PORT);
			}
			else
			{
				jettyPort = Integer.parseInt(portProp);
				s_Logger.info("Using port " + jettyPort);
			}
		}
		catch (NumberFormatException e)
		{
			s_Logger.warn(String.format("Error parsing %s property value '%s' "
					+ "cannot not be parsed as an integer", 
					JETTY_PORT_PROPERTY, System.getProperty(JETTY_PORT_PROPERTY)));
			
			s_Logger.info("Using default port " + JETTY_PORT);
		}
		
		Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(jettyPort);
        server.setConnectors(new Connector[] { connector });
        
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        
        context.setErrorHandler(new ApiErrorHandler());
        

        ServletHolder jerseyServlet = context.addServlet(
        		org.glassfish.jersey.servlet.ServletContainer.class, 
        		BASE_PATH);
        jerseyServlet.setInitOrder(1);
        
        /*  Either set the application class or the resource package */        
        // Application class
        jerseyServlet.setInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS,
        		APPLICATION_CLASS);        
        // Resources
//        jerseyServlet.setInitParameter(ServerProperties.PROVIDER_PACKAGES, 
//        		RESOURCE_PACKAGE);
        
        server.setHandler(context);       
        server.start();
        server.join();
    }
}
