/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.server;

import java.awt.Color;
import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.prelert.dao.mysql.TimeSeriesMySQLDAO;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Class which writes a causality chart and key into a PDF file using the JFreeChart
 * charting API and the iText PDF library.
 * @author Pete Harverson
 */
public class CausalityPDFChart
{
	static Logger logger = Logger.getLogger(CausalityPDFChart.class);
	
	private TimeSeriesMySQLDAO 			m_TimeSeriesDAO;
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;
	private List<TimeSeriesData>		m_TimeSeriesData;
	
	private List<Color>					m_LineColors;
	
	//private TimeSeriesCollection 		m_TimeSeriesCollection;
	//private JFreeChart					m_Chart;
	
	
	/**
	 * Creates a new CausalityPDFChart object for writing a causality chart and 
	 * key into a PDF file.
	 */
	public CausalityPDFChart()
	{
		createDataSource();
		
		// Add some test time series.
		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
		TimeSeriesConfig config1 = new TimeSeriesConfig("system_udp", "Packets Received", "lon-app01");
		TimeSeriesConfig config2 = new TimeSeriesConfig("system_udp", "Packets Received", "lon-app03");
		TimeSeriesConfig config3 = new TimeSeriesConfig("system_udp", "Packets Sent", "lon-data01");
		m_TimeSeriesConfigs.add(config1);
		m_TimeSeriesConfigs.add(config2);
		m_TimeSeriesConfigs.add(config3);
		
		m_LineColors = new ArrayList<Color>(); 
		m_LineColors.add(Color.RED);
		m_LineColors.add(Color.BLUE);
		m_LineColors.add(Color.GREEN);
		
		m_TimeSeriesData = new ArrayList<TimeSeriesData>(); 
		
		// Create the JFreeChart.
		//m_Chart = createChart();
	}
	
	
	/**
	 * Initialises a data source to obtain some causality data.
	 */
	protected void createDataSource()
	{
		Properties connProps = new Properties();
		connProps.put("driverClassName", "com.mysql.jdbc.Driver");
		connProps.put("url", "jdbc:mysql://prelert-solaris/incident_demo");
		connProps.put("username", "root");
		connProps.put("password", "");
		connProps.put("testOnBorrow", "true");
		connProps.put("validationQuery", "select 1");
		
		try
        {
	        DataSource dataSource = BasicDataSourceFactory.createDataSource(connProps);
	        m_TimeSeriesDAO = new TimeSeriesMySQLDAO();
	        m_TimeSeriesDAO.setDataSource(dataSource);
	        
	        logger.debug("Created data source: " + dataSource);
        }
        catch (Exception e)
        {
	        logger.error("Error creating data source", e);
        }
	}
	
	
	/**
	 * Creates the JFreeChart chart component.
	 * @return the JFreeChart.
	 */
//    private JFreeChart createChart() 
//    {
//    	m_TimeSeriesCollection = new TimeSeriesCollection();
//    	TimeSeriesCollection notificationsCollection = new TimeSeriesCollection();
//
//        JFreeChart chart = ChartFactory.createTimeSeriesChart(
//            "",  				// title
//            "Time",             // x-axis label
//            "Value",   			// y-axis label
//            m_TimeSeriesCollection,            // data
//            false,				// create legend?
//            true,				// generate tooltips?
//            false				// generate URLs?
//        );
//        
//        XYPlot plot = (XYPlot) chart.getPlot();
//        plot.setDataset(1, notificationsCollection);
//
//        chart.setBackgroundPaint(Color.white);
//        
//        TextTitle m_Title = chart.getTitle();
//        m_Title.setFont(new Font("Verdana", Font.BOLD, 12));
//        m_Title.setPaint(new Color(0x15428b));
//        m_Title.setHorizontalAlignment(HorizontalAlignment.LEFT);
//        m_Title.setPadding(5, 5, 0, 0);
//        
//        TextTitle m_Subtitle = new TextTitle("Details on incident at 2010-02-10 15:01:30");
//        m_Subtitle.setFont(new Font("Verdana", Font.BOLD, 9));
//        m_Subtitle.setPaint(new Color(0x15428b));
//        m_Subtitle.setHorizontalAlignment(HorizontalAlignment.LEFT);
//        m_Subtitle.setPadding(0, 5, 5, 0);
//        chart.addSubtitle(m_Subtitle);
//        
//        plot.setBackgroundPaint(Color.white);
//        plot.setRangeGridlinesVisible(false);
//        plot.setDomainGridlinesVisible(false);
//      	plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
//        plot.setDomainCrosshairVisible(true);
//        plot.setRangeCrosshairVisible(true);
//        plot.setOutlineVisible(false);
//        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
//
//        XYItemRenderer r = plot.getRenderer();
//        if (r instanceof XYLineAndShapeRenderer) 
//        {
//            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
//            renderer.setBaseShapesVisible(false);
//            renderer.setBaseShapesFilled(false);
//            renderer.setDrawSeriesLineAsPath(true);
//        }
//
//        // Set the colour for a couple of time series.
//        r.setSeriesPaint(0, new Color(0x0000FF));
//        r.setSeriesPaint(1, new Color(0x63B8FF));
//        
//  
//        DateAxis axis = (DateAxis) plot.getDomainAxis();
//        axis.setDateFormatOverride(new SimpleDateFormat("MMM d HH:mm"));
//
//        return chart;
//
//    }
    
    
    /**
     * Loads data from the server for time series in the chart between the 
     * specified start and end times.
     * @param minTime
     * @param maxTime
     */
	public void loadDataFromServer(Date minTime, Date maxTime)
	{
		try
        {
			for (TimeSeriesConfig config : m_TimeSeriesConfigs)
			{  
				List<TimeSeriesDataPoint> dataPoints = m_TimeSeriesDAO.getDataPointsForTimeSpan(config.getDataType(), 
						config.getMetric(), 
						minTime, 
						maxTime, 
						config.getSource(), 
						config.getAttributes(), 
						false);
				logger.debug("Number of time series data points: " + dataPoints.size());
				
				m_TimeSeriesData.add(new TimeSeriesData(config, dataPoints));
				
		        // Load the data into the JFreeChart.
//				TimeSeries timeSeries = new TimeSeries(config.getDataType());
//				for (TimeSeriesDataPoint dataPoint : dataPoints)
//		    	{
//		    		timeSeries.add(new Millisecond(new Date(dataPoint.getTime())), dataPoint.getValue());
//		    	}
			}
	        
        }
		catch (Exception e)
		{
			logger.error("Error loading data from database", e);
		}
	}
    
	
    /**
     * Exports the causality chart and key to a PDF file with the specified name.
     * @param filename
     */
//	public void exportViaIText(String filename)
//	{
//		Document document = new Document(PageSize.A4.rotate());
//		try
//		{
//			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
//			document.open();
//			PdfContentByte cb = writer.getDirectContent();
//			PdfTemplate tp = cb.createTemplate(PageSize.A4.rotate().getWidth(), PageSize.A4.rotate().getHeight());
//			Graphics2D g2d= tp.createGraphics(PageSize.A4.rotate().getWidth(), PageSize.A4.rotate().getHeight(), 
//					new DefaultFontMapper());
//			Rectangle2D r2d = new Rectangle2D.Double(0, 0, PageSize.A4.rotate().getWidth(), PageSize.A4.rotate().getHeight()/2);
//			m_Chart.draw(g2d, r2d);
//			g2d.dispose();
//			cb.addTemplate(tp, 0, 0);
//			
//		//	document.newPage();
//			
//			// Test out writing the key in the form of a table.
//			PdfPTable table = new PdfPTable(3); 
//
//			// Column headings
//			table.addCell("data type");
//			table.addCell("metric");
//			table.addCell("source");
//			PdfPCell[] cells = table.getRow(0).getCells();
//			for (PdfPCell cell : cells)
//			{
//				cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//			}
//			
//			
//			// Data
//			for (TimeSeriesConfig config : m_TimeSeriesConfigs)
//			{
//				table.addCell(config.getDataType());
//				table.addCell(config.getMetric());
//				table.addCell(config.getSource());
//			}
//			
//			// Write the table to the document.
//			table.setTotalWidth(400);
//			table.writeSelectedRows(0, -1, 250, 250, cb); 
//
//			
//		}
//		catch (Exception e)
//		{
//			logger.error("Error writing chart to PDF", e);
//		}
//		
//		document.close();
//		logger.debug("Chart written to file " + filename);
//	}
	
	
	public void exportViaPDFBox(OutputStream stream) throws IOException, COSVisitorException
	{
		
		// the document
        PDDocument doc = null;
        String message = "Details on incident at 2010-02-10 15:01:30";
        try
        {
            doc = new PDDocument();

            PDFont font = PDType1Font.HELVETICA;
            PDPage page = new PDPage();
            page.setMediaBox(PDPage.PAGE_SIZE_A4);
            page.setRotation(90);
            doc.addPage(page);
            PDRectangle pageSize = page.findMediaBox();
            Dimension pageDimension = pageSize.createDimension();
            
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            
            float fontSize = 10;
            float stringWidth = font.getStringWidth( message )*fontSize/1000f;
            float startX = 50;
            float startY = pageWidth - 50;
            PDPageContentStream contentStream = new PDPageContentStream(doc, page, false, false);
            
            logger.debug("A4 page dimension: " + pageDimension);
            
            // add the rotation using the current transformation matrix
            // including a translation of pageWidth to use the lower left corner as 0,0 reference
            contentStream.concatenate2CTM(0, 1, -1, 0, pageWidth, 0);
            contentStream.setFont( font, fontSize );
            
            // Add a title.
            contentStream.beginText();
            
            /*
            contentStream.moveTextPositionByAmount(startX, startY);
            contentStream.drawString( message);
            contentStream.moveTextPositionByAmount(0, 100);
            contentStream.drawString( message);
            contentStream.moveTextPositionByAmount(100, 100);
            contentStream.drawString( message);
            
            */
            
            contentStream.moveTextPositionByAmount(startX, startY+20);
            contentStream.drawString( message);
            
            contentStream.endText();
            
            // Draw the axes.
            contentStream.setStrokingColor(Color.GRAY);
            contentStream.drawLine(startX, pageWidth/2, pageHeight-50, pageWidth/2);
            contentStream.drawLine(startX, pageWidth/2, startX, pageWidth-50);
            
            // Draw the time series.
            int index = 0;
            for (TimeSeriesData timeSeries : m_TimeSeriesData)
            {
            	contentStream.setStrokingColor(m_LineColors.get(index));
            	
            	List<TimeSeriesDataPoint> dataPoints = timeSeries.getDataPoints();
            	float startLineX = startX;
            	float startLineY = pageWidth/2;
            	
            	for (TimeSeriesDataPoint dataPoint : dataPoints)
                {
            		
            		contentStream.drawLine(startLineX, startLineY, startLineX+3, pageWidth/2 + ((float)(dataPoint.getValue()/200)));
            		
            		startLineX+=3;
            		startLineY = pageWidth/2 + ((float)(dataPoint.getValue()/200));
                }
            	
            	// Add an entry to the key.
            	contentStream.drawLine(startX+200, pageWidth/2 - ((index+1)*25), startX+220, pageWidth/2 - ((index+1)*25));
            	
            	contentStream.setFont( font, 8 );
            	contentStream.beginText();    
                contentStream.moveTextPositionByAmount(startX+230, pageWidth/2 - ((index+1)*25) - 5);
                contentStream.drawString(m_TimeSeriesConfigs.get(index).getDataType());
                contentStream.moveTextPositionByAmount(70, 0);
                contentStream.drawString(m_TimeSeriesConfigs.get(index).getMetric());
                contentStream.moveTextPositionByAmount(80, 0);
                contentStream.drawString(m_TimeSeriesConfigs.get(index).getSource());
                
                contentStream.endText();

            	index++;
            }
            
            contentStream.beginText();   
            contentStream.moveTextPositionByAmount(startX, 20);
            contentStream.drawString("Generated at " + new Date());
            contentStream.endText();
            
            
            /*
            contentStream.drawLine(startX-2, startY-2, startX-2, startY+200+fontSize);
            contentStream.drawLine(startX-2, startY+200+fontSize, startX+100+stringWidth+2, startY+200+fontSize);
            contentStream.drawLine(startX+100+stringWidth+2, startY+200+fontSize, startX+100+stringWidth+2, startY-2);
            contentStream.setStrokingColor(Color.RED);
            contentStream.drawLine(startX+100+stringWidth+2, startY-2, startX-2, startY-2);
            
            contentStream.setStrokingColor(Color.RED);
            contentStream.drawLine(startX+100+stringWidth+2, startY-2, startX-2, startY-2);
            
            contentStream.setStrokingColor(Color.GREEN);
            for (int i = 0; i < 100; i++)
            {
            	if ( (i % 2 == 0))
            	{
            		contentStream.drawLine(startX+100+(i*5), startY+(i*3), startX+((i+1)*5), startY+((i+1)*3));
            	}
            }

            
            contentStream.setNonStrokingColor(Color.YELLOW);
            contentStream.fillRect(startX-2, startY-2, 20, 20);
            
            */
            
            contentStream.close();

            doc.save(stream);
            
            logger.debug("PDFBox saved chart to output stream");
        }
        finally
        {
            if( doc != null )
            {
                doc.close();
            }
        }

	}


	public static void main(String[] args)
	{
		try
		{
			CausalityPDFChart pdfChart = new CausalityPDFChart();
			
			GregorianCalendar calendar = new GregorianCalendar();
	        calendar.set(2010, 1, 10, 14, 0);
	        Date minTime = calendar.getTime();
	        calendar.set(2010, 1, 10, 16, 0);
	        Date maxTime = calendar.getTime();
			
	        pdfChart.loadDataFromServer(minTime, maxTime);
			//pdfChart.exportViaIText("c:/causalityChart.pdf");
			
	        FileOutputStream stream = new FileOutputStream("c:/pdfBoxChart.pdf");
			pdfChart.exportViaPDFBox(stream);
			stream.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
