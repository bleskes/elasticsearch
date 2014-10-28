/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.proxy;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

import com.prelert.proxy.dao.RemoteControlDAO;

/**
 * Simple class to cause the Proxy to shutdown.
 * Uses RMI to connect to the Proxy, invokes the shutdown command then returns. 
 */
public class ProxyController 
{
	// require a rebuild
	private static final String s_DefaultHost = "localhost";
	private static final int    s_DefaultPort = Registry.REGISTRY_PORT;
	private static final String s_StopVerb = "stop";
	private static final String s_KillVerb = "kill";
	
	private static String s_Host;
	private static int s_Port;
	private static String s_Verb;
	
	public static void main(String args[]) throws RemoteException, MalformedURLException, NotBoundException
	{
		RemoteControlDAO remoteControl = null;
		
		if (parseArguments(args))
		{
			String [] bindings = new String[0];
			try
			{
				Registry registry = LocateRegistry.getRegistry(s_Host, s_Port);
				
				bindings = registry.list();
				
				remoteControl = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");
			}
			catch (Exception e)
			{
				System.out.println("Could not locate registry with host=" + s_Host +
										", port=" + s_Port );
				System.out.println("Exception = " + e.toString());
				
				System.out.println("Registry bindings = " + Arrays.toString(bindings));
				
				return;
			}

			if (s_Verb.equals(s_StopVerb))
			{
				// stop the proxy.
				remoteControl.shutdown();					
			}
			else if (s_Verb.equals(s_KillVerb))
			{
				remoteControl.kill();
			}
		}
		else 
		{
			printUsage(args);
		}
	}
	
	
	private static void printUsage(String [] args)
	{
		System.out.println("Proxy Controller");
		System.out.print("Invalid arguments: ");
		for (String arg : args)
		{
			System.out.print(arg + " ");
		}
		
		System.out.println("");
		System.out.println("Usage: [-host] [-port] stop | kill");
		System.out.println("");
		System.out.println("The host and port options are optional and default to:");
		System.out.println("	host = " + s_DefaultHost);
		System.out.println("	port = " + s_DefaultPort);
		System.out.println("");
		System.out.println("stop is the default option, kill may be specified instead.");
		System.out.println("Example usage:");
		System.out.println("-host localhost -port 50000");
		System.out.println("-host localhost -port 50000 kill");
	}
	
	private static boolean parseArguments(String [] args)
	{
		s_Host = s_DefaultHost;
		s_Port = s_DefaultPort;
		
		if (args.length == 1 && 
				(args[0].equals(s_StopVerb) || args[0].equals(s_KillVerb)) )
		{
			s_Verb = args[0];
			return true;
		}

		try
		{
			if (args.length == 3)
			{
				if (args[0].equals("-host"))
				{
					s_Host = args[1];
				}
				else if (args[0].equals("-port"))
				{
					s_Port = Integer.parseInt(args[1]);
				}
				else 
				{
					return false;
				}
				
				if (args[2].equals(s_StopVerb) || args[2].equals(s_KillVerb))
				{
					s_Verb = args[2];
					return true;
				}
			}
			else if(args.length == 5)
			{
				if (args[0].equals("-host"))
				{
					s_Host = args[1];
				}
				else 
				{
					return false;
				}
				
				if (args[2].equals("-port"))
				{
					s_Port = Integer.parseInt(args[3]);
				}
				else 
				{
					return false;
				}
				
				if (args[4].equals(s_StopVerb) || args[4].equals(s_KillVerb))
				{
					s_Verb = args[4];
					return true;
				}
			}

		}
		catch (NumberFormatException e)
		{
			return false;
		}
		
		
		return false;
	}
	

	
}
