/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2011     *
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

package com.prelert.web;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import com.prelert.server.CausalityPDFChart;


public class CausalityViewExportController extends MultiActionController
{
	static Logger logger = Logger.getLogger(CausalityViewExportController.class);
	
	

    public ModelAndView pdf(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
		logger.debug("CausalityViewExportController handleRequest()");	
		
		response.setContentType("application/pdf");
		response.setHeader("Content-disposition", "attachment; filename=pdfBoxChart.pdf");

		OutputStream out = response.getOutputStream(); 
		
		/*
		FileInputStream in = new FileInputStream("c:/pdfBoxChart.pdf"); 
		
		byte[] buffer = new byte[4096]; 
		int length; 
		while ((length = in.read(buffer)) > 0)
		{     
			out.write(buffer, 0, length); 
		} 
		in.close(); 
		
		*/
		
		GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(2010, 1, 10, 13, 30);
        Date minTime = calendar.getTime();
        calendar.set(2010, 1, 10, 16, 30);
        Date maxTime = calendar.getTime();
		
		CausalityPDFChart chart = new CausalityPDFChart();
		chart.loadDataFromServer(minTime, maxTime);
		chart.exportViaPDFBox(out);
		
		out.flush(); 
		
		return null;
    }
    
    
    public ModelAndView csv(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
		logger.debug("CausalityViewExportController handleRequest()");
		

		response.setContentType("text/csv");
		response.setHeader("Content-disposition", "attachment; filename=causality_data.csv");

		
		OutputStream out = response.getOutputStream(); 
		FileInputStream in = new FileInputStream("c:/work/causality_data.csv"); 
		
		byte[] buffer = new byte[4096]; 
		int length; 
		while ((length = in.read(buffer)) > 0)
		{     
			out.write(buffer, 0, length); 
		} 
		in.close(); 
		out.flush(); 
		
		return null;
    }
    

}
