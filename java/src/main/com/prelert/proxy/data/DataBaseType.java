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

package com.prelert.proxy.data;


/**
 * Enum type that represents all the database types available for the OpenAPI 
 */
public enum DataBaseType 
{
	MYSQL
	{
		@Override
		public String toString()
		{
			return "MySQL";
		}
		
		@Override
		public String buildConnectionUrl(String host, int port, String databaseName)
		{
			String connectionUrl = String.format("jdbc:mysql://%s:%d/%s",
									host, port, databaseName);
			
			return connectionUrl;
		}
	},
	
	POSTGRESQL
	{
		@Override
		public String toString()
		{
			return "PostgreSQL";
		}
		
		@Override
		public String buildConnectionUrl(String host, int port, String databaseName)
		{
			String connectionUrl = String.format("jdbc:postgresql://%s:%d/%s",
											host, port, databaseName);

			return connectionUrl;
		}
	},
	
	SQL_SERVER
	{
		@Override
		public String toString()
		{
			return "SQL Server";
		}

		@Override
		public String buildConnectionUrl(String host, int port, String databaseName)
		{
			String connectionUrl = String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
									host, port, databaseName);

			return connectionUrl;
		}
	};
	
	
	/**
	 * For each specific database type this returns the connection string
	 * of the form jdbc:subprotocol:subname built from the parameters.
	 * 
	 * @param host
	 * @param port
	 * @param databaseName
	 * @return The Connection Url string.
	 */
	abstract public String buildConnectionUrl(String host, int port, String databaseName); 
	
	
	/**
	 * Return the DataBaseType from the string name.
	 * 
	 * @param The name parameter which must be from one of the
	 * enum values toString() function.
	 * @return  
	 */
	static public DataBaseType enumValue(String name)
	{
		return Enum.valueOf(DataBaseType.class, name.toUpperCase().replace(' ', '_'));
	}
}
