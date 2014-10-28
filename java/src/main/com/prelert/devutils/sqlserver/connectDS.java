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

package com.prelert.devutils.sqlserver;

import java.sql.*;
//import com.microsoft.sqlserver.jdbc.*;

public class connectDS {

   public static void main(String[] args) 
   {
      // Declare the JDBC objects.
      Connection con = null;
      CallableStatement cstmt = null;
      ResultSet rs = null;

      try {
         // Establish the connection. 
//         SQLServerDataSource ds = new SQLServerDataSource();
//         ds.setUser("dbUser");
//         ds.setPassword("pa55w0rd");
//         ds.setServerName("vm-win2008r2-64-1");
//         ds.setPortNumber(1433); 
//        // ds.setDatabaseName("master");
//         con = ds.getConnection();
         
    	  // vm-win2008r2-64-1
         con = DriverManager.getConnection("jdbc:sqlserver://192.168.62.233:1433;databaseName=NimbusSLMsdp", "dbUser", "pa55w0rd");
         
         System.out.println("Got connection");
         
         boolean result = con.isValid(3);
         System.out.println(result);
         
         con.isValid(3);

         // Execute a stored procedure that returns some data.
         cstmt = con.prepareCall("select TOP(10) sampletime, tz_offset, source, target, origin, robot, probe, samplevalue, compressed from V_QOS_APACHE_BUSYWORKERS");
         //cstmt = con.prepareCall("select 1");
         rs = cstmt.executeQuery();

         // Iterate through the data in the result set and display it.
         while (rs.next()) {
            System.out.println("Result =  " + rs.getString(1));
            System.out.println();
         }
      }

      // Handle any errors that may have occurred.
      catch (Exception e) {
    	  System.out.print(e);
         e.printStackTrace();
      }
      finally {
         if (rs != null) try { rs.close(); } catch(Exception e) {}
         if (cstmt != null) try { cstmt.close(); } catch(Exception e) {}
         if (con != null) try { con.close(); } catch(Exception e) {}
         System.exit(1);
      }
   }
}
