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

package com.prelert.devutils.introscope;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Simple wrapper around the Introscope JDBC driver. 
 * The query should be passed as a parameter and the host, username
 * and option password should be passed as -D VM arguments.
 * 
 * Hard coded to connect on port 5001.
 * 
 */
public class IntroscopeJDBC 
{
	
	public static void main(String[] args) throws SQLException
	{
		if (args.length == 0)
		{
			System.out.println("ERROR: A query string must be provided as an argument.");
			return;
		}
		
		final String query = args[0];

		
		String host = System.getProperty("host");
		String port = System.getProperty("port");
		String user = System.getProperty("user");
		String password = System.getProperty("password", "");
		if (host == null || port == null || user == null)
		{
			System.out.println("ERROR: A host, port and username must be provided.");
			System.out.println("Pass -Dhost=host -Dport=port -Duser=user as the Java VM arguments");
			return;
		}
		
		
		final String connectionString = String.format("jdbc:introscope:net//%s:%s@%s:%s", user, password, host, port);

		
		// Make the connection
		DriverManager.registerDriver(new com.wily.introscope.jdbc.IntroscopeDriver());
		Connection con = DriverManager.getConnection(connectionString);
		
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		System.out.println(rs.getMetaData().getColumnCount());
		
		int i = 1;
		for(; i <= rs.getMetaData().getColumnCount() -1; i++)
		{
			String colName = rs.getMetaData().getColumnName(i);
			System.out.print(colName + ", ");
		}
		System.out.print(rs.getMetaData().getColumnName(i) + "\n");
		
		while (rs.next())
		{
			int col = 1;
			for(; col <= rs.getMetaData().getColumnCount() -1; col++)
			{
				String colValue = rs.getString(col);
				System.out.print("\"" + colValue + "\"" + ",");
			}
			System.out.print("\"" + rs.getString(col) + "\"" + "\n");
		}

	}

}
