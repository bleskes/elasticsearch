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

package com.prelert.server;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;

import com.prelert.client.CSSColorChart;
import com.prelert.client.CSSSeverityColors;
import com.prelert.client.CSSSymbolChart;
import com.prelert.client.CSSSymbolChart.Shape;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityData;
import com.prelert.data.Severity;
import com.prelert.data.ProbableCause;
import com.prelert.data.ProbableCauseCollection;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Class for writing a causality chart to portable document format (PDF) file.
 * @author Pete Harverson
 */
public class CausalityPDFChartWriter
{
	static Logger logger = Logger.getLogger(CausalityPDFChartWriter.class);
	
	private PDDocument 				m_Document;
	private Locale					m_Locale;
	private TimeZone				m_TimeZone;
	
	private PDRectangle		m_PageMediaBox;
	private float			m_PageWidth;
	private float 			m_PageHeight;
	private float 			m_Margin = 50;
	private PDPage			m_TopPage;
	private PDPage			m_LastPage;
	
	private float			m_XOrigin;
	private float			m_XMax;
	private float 			m_YOrigin;
	private float			m_YMax;
	
	private long			m_MinTimeMs;
	private long			m_MaxTimeMs;
	
	private float			m_XScaling;
	private float			m_YScaling;
	
	private float 			m_KeyXOrigin;
	private float 			m_KeyY;
	private float 			m_KeyWidth;
	private float 			m_KeyCellMargin;
	
	private PDFont 			m_Font;
	private float			m_TitleFontSize = 10;
	private float			m_AxisLabelFontSize = 8;
	private float			m_KeyFontSize = 8;
	private float 			m_SymbolSize = 3;
	
	private float[]			m_KeyColumnWidths;
	
	private int				m_TimeSeriesCount;
	private int				m_NotificationCount;
	private HashMap<Severity, Integer> m_NotificationsBySeverity;
	private List<Rectangle2D.Float>	m_NotificationBands; 
	
	
	/**
	 * Creates a new PDF causality chart writer.
	 * @param title the title to display at the top of the document.
	 * @param locale locale to use for outputting locale-sensitive text.
	 * @param timeZone time zone for the calendar of the writer's date formatter.
	 * @throws IOException an an I/O error occurs creating the PDF document.
	 */
	public CausalityPDFChartWriter(String title, Locale locale, TimeZone timeZone) 
		throws IOException
	{
		m_Locale = locale;
		m_TimeZone = timeZone;
		
		performSetup();
		
		if (title != null)
		{
			drawTitle(title);
		}
		
		drawAxes();
		drawKeyHeader();
	}
	
	
	/**
	 * Performs initial setup for the writer, creating the PDF document and
	 * initialising the page for writing the chart to.
	 * @throws IOException if an I/O error occurs setting up the writer.
	 */
	protected void performSetup() throws IOException
	{
		// Create the document.
		m_Document = new PDDocument();
		
		// Read in the page size.
		m_PageMediaBox = PDPage.PAGE_SIZE_LETTER;
        try
        {
	        ResourceBundle messages = ResourceBundle.getBundle("prelert_messages", m_Locale);
	        String pageSize = messages.getString("page.size");
	        if (pageSize.equalsIgnoreCase("A4"))
	        {
	        	m_PageMediaBox = PDPage.PAGE_SIZE_A4;
	        }
        }
        catch (MissingResourceException mre)
        {
        	logger.error("Missing page size key in prelert_messages.properties: " + mre.getKey());
        }
        
        m_PageWidth = m_PageMediaBox.getWidth();
        m_PageHeight = m_PageMediaBox.getHeight();
		
		// Create the first page.
        m_TopPage = startNewPage();
        
        // Set the font.
        m_Font = PDType1Font.HELVETICA;
        
        // Initialise basic chart coordinates.
        // (0,0) is bottom left of page.
        m_XOrigin = m_Margin;
        m_XMax = m_PageWidth-m_Margin;
        m_YOrigin = m_PageHeight/2;
        setYAxisMax(m_PageHeight-m_Margin);
        
        // Initialise basic key coordinates.
        m_KeyXOrigin = m_Margin*1.25f;
        m_KeyY = m_YOrigin - 50;
        m_KeyWidth = m_PageWidth-(2.5f*m_Margin);
        m_KeyCellMargin = 3;
        m_KeyColumnWidths = new float[5];
        m_KeyColumnWidths[0] = m_KeyWidth * 0.08f;	// Symbol
        m_KeyColumnWidths[1] = m_KeyWidth * 0.15f;	// Type
        m_KeyColumnWidths[2] = m_KeyWidth * 0.32f;	// Description
        m_KeyColumnWidths[3] = m_KeyWidth * 0.15f;	// Sources
        m_KeyColumnWidths[4] = m_KeyWidth * 0.3f;	// Attributes
	}
	
	
	/**
	 * Starts a new page in the document.
	 * @return the page that has been created.
	 */
	protected PDPage startNewPage() throws IOException
	{
		m_LastPage = new PDPage();
		m_LastPage.setMediaBox(m_PageMediaBox);
    	m_Document.addPage(m_LastPage);
    	
    	drawLogo();
    	
    	return m_LastPage;
	}
	
	
	/**
	 * Returns a page content stream for writing data to the chart.
	 * @return PDPageContentStream for the page containing the chart.
	 * @throws IOException if there is an I/O error creating the stream.
	 */
	protected PDPageContentStream getChartContentStream() throws IOException
	{
		return new PDPageContentStream(m_Document, m_TopPage, true, false);
	}
	
	
	/**
	 * Returns a page content stream for writing the next element in the key.
	 * @return PDPageContentStream for the page containing the bottom of the key.
	 * @throws IOException if there is an I/O error creating the stream.
	 */
	protected PDPageContentStream getKeyContentStream() throws IOException
	{
		return new PDPageContentStream(m_Document, m_LastPage, true, false);
	}
	
	
	/**
	 * Sets the date range to use for the start and end points of the chart x-axis,
	 * and draws the x (time) axis tick labels.
	 * @param minTime start time for the x-axis.
	 * @param maxTime end time for the x-axis.
	 * @throws IOException if there is an I/O error drawing the x axis tick labels.
	 */
	public void setDateRange(Date minTime, Date maxTime) throws IOException
	{
		m_MinTimeMs = minTime.getTime();
		m_MaxTimeMs = maxTime.getTime();
		
		m_XScaling = (m_XMax - m_XOrigin)/(m_MaxTimeMs - m_MinTimeMs);
		
		drawXAxisTickLabels();
	}
	
	
	/**
	 * Sets the position of the y axis maximum to the specified value.
	 * @param yMax coordinate of the y axis maximum, in the PDF page space, 
	 * 	where (0,0) is the bottom left hand corner of the page.
	 */
	protected void setYAxisMax(float yMax)
	{
		m_YMax = yMax;
		m_YScaling = (m_YMax - m_YOrigin);
	}
	
	
	/**
	 * Sets the time zone for the calendar used when formatting dates for 
	 * display on the chart.
	 * @param zone the time zone for the calendar of the chart's 
	 * 	date formatter.
	 */
	public void setTimeZone(TimeZone zone)
	{
		m_TimeZone = zone;
	}
	
	
	/**
	 * Sets the time zone for the calendar used when formatting dates for 
	 * display on the chart.
	 * @param zone the time zone associated with the calendar of the chart's 
	 * 	date formatter.
	 */
	public TimeZone getTimeZone()
	{
		return m_TimeZone;
	}
	
	
	/**
	 * Draws the Prelert logo at the top of the last page in the document.
	 * @throws IOException an an I/O error occurs drawing the logo on the document.
	 */
	protected void drawLogo() throws IOException
	{
		try
		{
			InputStream imageStream = getClass().getResourceAsStream("logo_white.jpg");
			PDXObjectImage image = new PDJpeg(m_Document, imageStream);
			float y = m_PageHeight - (image.getHeight() + 8);
	
			PDPageContentStream contentStream = getKeyContentStream();
			contentStream.drawImage(image, 8, y );
	
			imageStream.close();
			contentStream.close();
		}
		catch (NullPointerException npe)
		{
			logger.error("Image for logo could not be found", npe);
		}
	}
	
	
	/**
	 * Draws the specified title on the chart.
	 * @param title the title to use for the chart.
	 * @throws IOException if an I/O error occurs drawing the title to the document.
	 */
	protected void drawTitle(String title) throws IOException
	{
		PDPageContentStream contentStream = getChartContentStream();
		
		// Wrap text at end of each line.
		float paragraphWidth = m_PageWidth - (2*m_Margin);
		String[] wrappedText = getWrappedText(title, m_TitleFontSize, paragraphWidth);
		
		// Drop the position of the y axis according to the number of lines.
		int numLines = wrappedText.length;
		setYAxisMax(m_YMax - ((numLines + 1) * m_TitleFontSize));
		
		contentStream.setFont(m_Font, m_TitleFontSize);
		contentStream.setNonStrokingColor(new Color(0x15428b));
		
		contentStream.beginText();  
		contentStream.moveTextPositionByAmount(m_XOrigin, m_PageHeight - m_Margin - m_TitleFontSize + 1);
		
		for (String line : wrappedText)
		{
			contentStream.drawString(line);
			contentStream.moveTextPositionByAmount(0, -(m_TitleFontSize + 1));
		}

		contentStream.endText();
		contentStream.close();
	}
	
	
	/**
	 * Draws the x and y axes on the causality chart.
	 * @throws IOException if an I/O error occurs drawing the axes on the document.
	 */
	protected void drawAxes() throws IOException
	{
		PDPageContentStream contentStream = getChartContentStream();
		
		// Draw the axes.
		contentStream.setStrokingColor(Color.GRAY);
		contentStream.drawLine(m_XOrigin, m_YOrigin, m_XMax, m_YOrigin);
		contentStream.drawLine(m_XOrigin, m_YOrigin, m_XOrigin, m_YMax + 0.5f);
		
		// Draw the x-axis ticks.
		float xCoord = m_XOrigin;
		float stepSize = (m_XMax - m_XOrigin)/7f;
		for (int i = 0; i <= 7; i++)
		{
			contentStream.drawLine(xCoord, m_YOrigin, xCoord, m_YOrigin-3);
			xCoord += stepSize;
		}
		
		contentStream.setFont(m_Font, m_AxisLabelFontSize);
		contentStream.setNonStrokingColor(Color.BLACK);
		ResourceBundle messages = ResourceBundle.getBundle("prelert_messages", m_Locale);
		contentStream.beginText();    
		contentStream.moveTextPositionByAmount(m_PageWidth/2 - 10, m_YOrigin - 20);
		contentStream.drawString(messages.getString("label.time"));
		contentStream.endText();
		
		// Draw the y-axis ticks.
		float yCoord = m_YOrigin;
		stepSize = (m_YMax - m_YOrigin)/4f;
		for (int i = 0; i <= 4; i++)
		{
			contentStream.drawLine(m_XOrigin, yCoord, m_XOrigin-3, yCoord);
			yCoord += stepSize;
		}
		
		contentStream.close();
	}
	
	
	/**
	 * Draws the tick labels on the x (time) axis. Should only be called once the date
	 * range for the chart has been set.
	 * @see #setDateRange(Date, Date)
	 * @throws IOException if an I/O error occurs drawing the tick labels on the document.
	 */
	protected void drawXAxisTickLabels() throws IOException
	{
		PDPageContentStream contentStream = getChartContentStream();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d HH:mm", m_Locale);
		dateFormat.setTimeZone(m_TimeZone);
		
		contentStream.setFont(m_Font, m_AxisLabelFontSize);
		contentStream.setNonStrokingColor(Color.BLACK);
		contentStream.beginText(); 
		
		// Draw the x-axis ticks, every two tick markers.
		float stepSize = (m_XMax - m_XOrigin)/7f;
		long timeStepSize = (m_MaxTimeMs - m_MinTimeMs)/7;
		long timeStart = m_MinTimeMs;
		Date tickTime;
		contentStream.moveTextPositionByAmount(m_XOrigin - 25, m_YOrigin - 10);
		for (int i = 0; i <= 7; i++)
		{
			if (i % 2 == 0)
			{
				tickTime = new Date(timeStart);
				tickTime = DateUtils.round(tickTime, Calendar.MINUTE);

				contentStream.drawString(dateFormat.format(tickTime));
			}
			
			contentStream.moveTextPositionByAmount(stepSize, 0);
			timeStart+=timeStepSize;
		}
		
		contentStream.endText();
		contentStream.close();
	}
	
	
	/**
	 * Adds a dotted line marker to the chart to indicate a specific time.
	 * @param time date/time for the time marker.
	 */
	public void drawTimeMarker(Date time) throws IOException
	{
		if (time != null)
		{
			PDPageContentStream contentStream = getChartContentStream();
			
			contentStream.setStrokingColor(new Color(0x000080));
			
			float dotRadius = 0.25f;
			float gap = 6f;
			Symbol symbol = Symbol.CIRCLE;
			
			long timeMs = time.getTime();
			float xCoord = m_XOrigin + ((timeMs-m_MinTimeMs) * m_XScaling);
			float[] x = symbol.getChartXCoordinates(xCoord, dotRadius);
			float[] y;
			for (float yCoord = m_YOrigin + gap; yCoord < m_YMax-gap; yCoord+=gap)
			{
				y = symbol.getChartYCoordinates(yCoord, dotRadius);
				contentStream.drawPolygon(x, y);
			}
			
			contentStream.close();
		}
	}
	
	
	/**
	 * Draws the header for the chart key.
	 * @throws IOException an an I/O error occurs drawing to the document.
	 */
	protected void drawKeyHeader() throws IOException
	{
		ResourceBundle messages = ResourceBundle.getBundle("prelert_messages", m_Locale);
		String[] columnLabels = new String[]{
				messages.getString("label.symbol"), 
				messages.getString("label.type"),
				messages.getString("label.description"),
				messages.getString("label.source"),
				messages.getString("label.attributes"),};
		
		float rowHeight = m_KeyFontSize*2 - 1;
		
		PDPageContentStream keyContentStream = getKeyContentStream();
		
		keyContentStream.setNonStrokingColor(new Color(0x15428b));
		keyContentStream.fillRect(m_KeyXOrigin, m_KeyY-rowHeight, m_KeyWidth, rowHeight);
        
		keyContentStream.setFont(m_Font, m_KeyFontSize);
		keyContentStream.setNonStrokingColor(Color.WHITE);
        
		keyContentStream.beginText();  
		keyContentStream.moveTextPositionByAmount(m_KeyXOrigin + m_KeyCellMargin, m_KeyY - 0.66f*rowHeight);
        
        for (int i = 0; i < columnLabels.length; i++)
        {
        	keyContentStream.drawString(columnLabels[i]);
        	keyContentStream.moveTextPositionByAmount(m_KeyColumnWidths[i], 0);
        }
        
        keyContentStream.endText();  
        keyContentStream.close();
        
        m_KeyY -= rowHeight;
	}
	
	
	/**
	 * Adds the specified collection of notification type probable causes to
	 * the chart.
	 * @param collection notification type ProbableCauseCollection to add.
	 */
	public void addNotifications(ProbableCauseCollection collection) throws IOException
	{
		logger.debug("addNotifications(): " + collection.getProbableCause(0).getDescription());
		
		if (m_NotificationBands == null)
		{
			m_NotificationBands = new ArrayList<Rectangle2D.Float>();
			m_NotificationsBySeverity = new HashMap<Severity, Integer>();
		}
		
		Rectangle2D.Float bounds = calculateNotificationBounds(
				collection.getStartTime(), collection.getEndTime());
		m_NotificationBands.add(bounds);
		
		ProbableCause first = collection.getProbableCause(0);
		Severity severity = first.getSeverity();
		
		// Increase the counter for this severity so that we can pick the right symbol.
		Integer countBySev = m_NotificationsBySeverity.get(severity);
		if (countBySev == null)
		{
			countBySev = new Integer(0);
		}
		int count = (countBySev.intValue());
		Symbol symbol = getNotificationSymbol(count);
		
		// If counter greater than the number of symbol shapes, don't fill, 
		// so as to double the number of different symbols.
		boolean fill = ((count / CSSSymbolChart.getInstance().getNumberOfSymbols()) % 2) == 0;
		
		// Draw the first notification in the collection.
		float[] x = symbol.getChartXCoordinates(bounds.x + m_SymbolSize, m_SymbolSize);
		float[] y = symbol.getChartYCoordinates(bounds.y + m_SymbolSize, m_SymbolSize);
		
		// Get the symbol colour to match the severity.
		String hexColor = CSSSeverityColors.getColor(severity);
		Color color = Color.decode(hexColor);
		
		PDPageContentStream contentStream = getChartContentStream();
		
		if (fill == true)
		{
			contentStream.setNonStrokingColor(color);
			contentStream.fillPolygon(x, y);
		}
		else
		{
			contentStream.setStrokingColor(color);
			contentStream.drawPolygon(x, y);
		}
		
		// If more than one, draw the last notification with a line between.
		int num = collection.getSize();
		if (num > 1)
		{
			x = symbol.getChartXCoordinates(bounds.x + bounds.width - m_SymbolSize, m_SymbolSize);
			y = symbol.getChartYCoordinates(bounds.y + m_SymbolSize, m_SymbolSize);
			
			if (fill == true)
			{
				contentStream.fillPolygon(x, y);
			}
			else
			{
				contentStream.drawPolygon(x, y);
			}
			
			contentStream.setLineWidth(1f);
			contentStream.setStrokingColor(color);
			contentStream.drawLine(bounds.x + m_SymbolSize, bounds.y + m_SymbolSize, 
					bounds.x + bounds.width - m_SymbolSize, bounds.y + m_SymbolSize);
			
		}
		
		contentStream.close();
		
		// Add entry to key.
		ArrayList<String> keyValues = new ArrayList<String>();
		keyValues.add(first.getDataSourceType().getName());
		keyValues.add(first.getDescription());
		int sourcesCount = collection.getSourceCount();
		if (sourcesCount == 1)
		{
			keyValues.add(first.getSource());
		}
		else
		{
			keyValues.add(String.valueOf(sourcesCount));
		}
		
		addEntryToKey(symbol, color, fill, keyValues);
		
		m_NotificationsBySeverity.put(severity, ++count);
		m_NotificationCount++;
	}
	
	
	/**
	 * Adds the specified notification type causality data to the chart.
	 * @param notificationData notification type causality data to add.
	 */
	public void addNotifications(CausalityData notificationData) throws IOException
	{
		logger.debug("addNotifications(): " + notificationData.getDescription());
		
		if (m_NotificationBands == null)
		{
			m_NotificationBands = new ArrayList<Rectangle2D.Float>();
		}
		
		Date startTime = notificationData.getStartTime();
		Date endTime = notificationData.getEndTime();
		Rectangle2D.Float bounds = calculateNotificationBounds(startTime, endTime);
		m_NotificationBands.add(bounds);

		// Used fill diamond symbols for the end points.
		Symbol symbol = Symbol.DIAMOND;
		
		// Draw the first notification in the collection.
		float[] x = symbol.getChartXCoordinates(bounds.x + m_SymbolSize, m_SymbolSize);
		float[] y = symbol.getChartYCoordinates(bounds.y + m_SymbolSize, m_SymbolSize);
		
		// Get the symbol colour to match the severity.
		// Get a line colour from the CSSColorChart.
		String hexColor = CSSColorChart.getInstance().getColor(m_NotificationCount);
		Color color = Color.decode(hexColor);
		
		PDPageContentStream contentStream = getChartContentStream();
		contentStream.setNonStrokingColor(color);
		contentStream.fillPolygon(x, y);
		
		// If more than one, draw the last notification with a line between.
		if (startTime.equals(endTime) == false)
		{
			x = symbol.getChartXCoordinates(bounds.x + bounds.width - m_SymbolSize, m_SymbolSize);
			y = symbol.getChartYCoordinates(bounds.y + m_SymbolSize, m_SymbolSize);
			contentStream.fillPolygon(x, y);
			
			contentStream.setLineWidth(m_SymbolSize*2f);
			contentStream.setStrokingColor(color);
			contentStream.drawLine(bounds.x + m_SymbolSize, bounds.y + m_SymbolSize, 
					bounds.x + bounds.width - m_SymbolSize, bounds.y + m_SymbolSize);
			
		}
		
		contentStream.close();
		
		// Add entry to key.
		ArrayList<String> keyValues = new ArrayList<String>();
		keyValues.add(notificationData.getDataSourceType().getName());
		keyValues.add(notificationData.getDescription());
		keyValues.add(notificationData.getSource());
		List<Attribute> attributes = notificationData.getAttributes();
		if (attributes != null)
		{
			StringBuilder strBuilder = new StringBuilder();
			for (Attribute attribute : attributes)
			{
				// Space and newline delimiter so that text can be wrapped in a key cell.
				strBuilder.append(attribute.getAttributeName());
				strBuilder.append(" = ");
				strBuilder.append(attribute.getAttributeValue());
				strBuilder.append('\n');
			}
			
			keyValues.add(strBuilder.toString());
		}
		
		addEntryToKey(symbol, color, true, keyValues);
		
		m_NotificationCount++;
	}
	
	
	/**
	 * Adds a time series type probable cause to the chart.
	 * @param probCause time series type ProbableCause to add.
	 * @param dataPoints data points for the time series.
	 */
	public void addTimeSeries(ProbableCause probCause, List<TimeSeriesDataPoint> dataPoints) 
		throws IOException
	{
		logger.debug("addTimeSeries(): " + probCause);
		
		PDPageContentStream contentStream = getChartContentStream();
		
		float scalingFactor = ((float)probCause.getScalingFactor());
		
		// Get a line colour from the CSSColorChart.
		String hexColor = CSSColorChart.getInstance().getColor(m_TimeSeriesCount);
		Color lineColor = Color.decode(hexColor);
		contentStream.setStrokingColor(lineColor);
		contentStream.setLineWidth(1f);
		
		Point2D.Float start = null;
		Point2D.Float end = null;
		for (TimeSeriesDataPoint point : dataPoints)
		{
			end = getTimeSeriesDataPointCoord(point, scalingFactor);
			
			if (start != null)
			{			
				// If the causality chart is zoomed out from the metrics time
				// span of 15 mins, then may get some clipping of off-chart points.
				start.y = Math.min(start.y, m_YMax);
				end.y = Math.min(end.y, m_YMax);
				
				contentStream.drawLine(start.x, start.y, end.x, end.y);
			}
			
			start = end;
		}
		
		contentStream.close();
		
		// Add time series to key.
		ArrayList<String> keyValues = new ArrayList<String>();
		keyValues.add(probCause.getDataSourceType().getName());
		keyValues.add(probCause.getDescription());
		keyValues.add(probCause.getSource());
		List<Attribute> attributes = probCause.getAttributes();
		if (attributes != null)
		{
			StringBuilder strBuilder = new StringBuilder();
			for (Attribute attribute : attributes)
			{
				// Space and newline delimiter so that text can be wrapped in a key cell.
				strBuilder.append(attribute.getAttributeName());
				strBuilder.append(" = ");
				strBuilder.append(attribute.getAttributeValue());
				strBuilder.append('\n');
			}
			
			keyValues.add(strBuilder.toString());
		}
		
		addEntryToKey(Symbol.LINE, lineColor, true, keyValues);
		
		m_TimeSeriesCount++;
	}
	
	
	/**
	 * Adds an entry to the key.
	 * @param symbol 		the chart symbol.
	 * @param symbolColor	the symbol colour.
	 * @param fillSymbol	flag indicated whether the symbol is filled with colour.
	 * @param cellValues	the text values to write to the key entry.
	 * @throws IOException	if an I/O error occurs writing to the document.
	 */
	protected void addEntryToKey(Symbol symbol, Color symbolColor, boolean fillSymbol,
			List<String> cellValues) throws IOException
	{
		float rowHeight = m_KeyFontSize*2;
		
		List<String[]> cellContents = new ArrayList<String[]>();
		String[] wrappedText;
        int numRows = 1;
        for (int i = 0; i < cellValues.size(); i++)
        {
        	wrappedText = getWrappedText(cellValues.get(i), m_KeyFontSize, 
        			m_KeyColumnWidths[i+1]-m_KeyCellMargin);
        	cellContents.add(wrappedText);
        	
        	numRows = Math.max(numRows, wrappedText.length);
        }
        
        if (m_KeyY - (rowHeight + ((numRows-1) * m_KeyFontSize)) < m_Margin)
        {
        	// Start a new page.
        	startNewPage();
        	
        	m_KeyY = m_PageHeight - m_Margin;
        	drawKeyHeader();
        }
		
		// Draw the symbol (left justified)
		float[] x = symbol.getChartXCoordinates(m_KeyXOrigin + m_KeyCellMargin + m_SymbolSize, m_SymbolSize);
		float[] y = symbol.getChartYCoordinates(m_KeyY - 0.5f*rowHeight, m_SymbolSize);
		
		PDPageContentStream contentStream = getKeyContentStream();
		
		if (fillSymbol == true)
		{
			contentStream.setNonStrokingColor(symbolColor);
			contentStream.fillPolygon(x, y);
		}
		else
		{
			contentStream.setStrokingColor(symbolColor);
			contentStream.drawPolygon(x, y);
		}
		
		// Draw the text	
		contentStream.setFont(m_Font, m_KeyFontSize);
		contentStream.setNonStrokingColor(Color.BLACK);
		contentStream.beginText();  
        
		contentStream.moveTextPositionByAmount(m_KeyXOrigin + m_KeyCellMargin, 
        		m_KeyY - 0.66f*rowHeight); 
        
        for (int i = 0; i < cellContents.size(); i++)
        {
        	contentStream.moveTextPositionByAmount(m_KeyColumnWidths[i], 0);
        	
        	wrappedText = cellContents.get(i);
        	for (int j = 0; j < wrappedText.length; j++)
        	{
        		if (j > 0)
        		{
        			contentStream.moveTextPositionByAmount(0, -m_KeyFontSize);
        		}
        		contentStream.drawString(wrappedText[j]);
        	}
        	
        	contentStream.moveTextPositionByAmount(0, (wrappedText.length - 1) * m_KeyFontSize);
        }
        
        rowHeight += ((numRows-1) * m_KeyFontSize);
        
        contentStream.endText(); 
        
        // Draw a border around the row.
        contentStream.setLineWidth(0.5f);
        contentStream.setStrokingColor(new Color(0x15428b));
        contentStream.drawLine(m_KeyXOrigin+0.25f, m_KeyY, m_KeyXOrigin+0.25f, m_KeyY-rowHeight);
        contentStream.drawLine(m_KeyXOrigin+0.25f, m_KeyY-rowHeight, 
        		m_KeyXOrigin+m_KeyWidth-0.25f, m_KeyY-rowHeight);
        contentStream.drawLine(m_KeyXOrigin+m_KeyWidth-0.25f, m_KeyY, 
        		m_KeyXOrigin+m_KeyWidth-0.25f, m_KeyY-rowHeight);
        
        contentStream.close();
        
        m_KeyY -= rowHeight;
	}

	
	/**
	 * Writes the PDF document to the supplied output stream. Must only be called 
	 * once all the elements have been added to the chart
	 * @param stream the stream to write to.
	 * @throws IOException if an I/O error occurs writing the chart to the output stream.
	 */
	public void write(OutputStream stream) throws IOException
	{
		try
		{
			m_Document.save(stream);
			logger.debug("PDF document saved to " + stream);
		}  
		catch (COSVisitorException cve)
		{
			throw new IOException("Error writing PDF document to output stream");
		}
	}
	
	
	/**
	 * Closes the underlying document object. Should be called after the chart has 
	 * been written to the output stream.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		if(m_Document != null)
        {
			m_Document.close();
        }
	}
	
	
	/**
	 * Returns the coordinates for the specified time series data point
	 * @param point time series data point.
	 * @param probCauseScalingFactor the scaling factor by which the data
	 * 		value should be scaled in relation to other time series probable
	 * 		causes, a value between 0 and 1.
	 * @return the coordinates, in the PDF page space, where (0,0) is the bottom
	 * 		left hand corner of the page.
	 */
	protected Point2D.Float getTimeSeriesDataPointCoord(
			TimeSeriesDataPoint point, float probCauseScalingFactor)
	{
		long timeMs = point.getTime();
		float value = (float)(point.getValue());
		
		float xCoord = m_XOrigin + ((timeMs-m_MinTimeMs) * m_XScaling);
		float yCoord = m_YOrigin + (value*probCauseScalingFactor*m_YScaling);
		
		return new Point2D.Float(xCoord, yCoord);
	}
	
	
	/**
	 * Returns the symbol for the notification type probable cause which is 
	 * currently being added to the chart.
	 * @param countForSeverity number of aggregated notifications, with the same
	 *  	severity as the current probable cause which have already
	 * 		been added to the chart.
	 * @return the chart symbol to use.
	 */
	protected Symbol getNotificationSymbol(int countForSeverity)
	{
		Shape symbolShape = CSSSymbolChart.getInstance().getSymbolShape(countForSeverity);
		Symbol symbol;
		switch (symbolShape)
		{
			case SQUARE:
				symbol = Symbol.SQUARE;
				break;
				
			case DIAMOND:
				symbol = Symbol.DIAMOND;
				break;
				
			case ARROW_UP:
				symbol = Symbol.ARROW_UP;
				break;
				
			case ARROW_DOWN:
				symbol = Symbol.ARROW_DOWN;
				break;
				
			case DIABOLO_HORIZONTAL:
				symbol = Symbol.DIABOLO_HORIZONTAL;
				break;
				
			case DIABOLO_VERTICAL:
				symbol = Symbol.DIABOLO_VERTICAL;
				break;
				
			case CIRCLE:
				symbol = Symbol.CIRCLE;
				break;
				
			case CROSS:
				symbol = Symbol.CROSS;
				break;
				
			case CROSS_DIAGONAL:
				symbol = Symbol.CROSS_DIAGONAL;
				break;
				
			case STAR:
				symbol = Symbol.STAR;
				break;
				
			default:
				symbol = Symbol.SQUARE;
				break;
		}
		
		return symbol;
	}
	
	
	/**
	 * Calculates the bounds for a notification type probable cause collection.
	 * The calculation takes into account other collections with similar time values, 
	 * so that they do not overlap on the chart.
	 * @param startTime start time of notification set.
	 * @param endTime end time of notification set.
	 * @return the bounds of the notification 'band'.
	 */
	protected Rectangle2D.Float calculateNotificationBounds(Date startTime, Date endTime)
	{
		float yPos = m_YOrigin + 1;

		long msStart1 = startTime.getTime();
		long msEnd1 = endTime.getTime();
		
		// Get the coordinates of the left hand side of the starting notification.
		float leftX = m_XOrigin + ((msStart1-m_MinTimeMs) * m_XScaling) - m_SymbolSize;
		
		// Get the width of the bounding rectangle.
		float width = ((msEnd1 - msStart1) * m_XScaling) + (m_SymbolSize * 2);			
		
		Rectangle2D.Float bounds = new Rectangle2D.Float(leftX, yPos, width, m_SymbolSize*2);
		
		if (m_NotificationBands.size() > 0)
		{
			boolean foundFreePos = false;
			
			while (foundFreePos == false)
			{
				for (Rectangle2D.Float rect : m_NotificationBands)
				{		
					if (rect.intersects(bounds) == true)
					{
						bounds.y += ((m_SymbolSize * 2) + 1);
						if ( (bounds.y + bounds.height) <= m_YMax)
						{
							foundFreePos = false;
						}
						else
						{
							// If we have so many notifications that they go off the
							// top of the chart, display at the top of chart.
							bounds.y = m_YMax - (m_SymbolSize *2);
							foundFreePos = true;
						}
						break;
					}
					else
					{
						foundFreePos = true;
					}
				}
			}
		}
		
		return bounds;
	}
	
	
	/**
	 * Returns an array of lines for the supplied text, wrapped so as to fit within
	 * the given available width.
	 * @param text String of text to wrap.
	 * @param fontSize font size to be used to output text to the document.
	 * @param availableWidth width, in page units, available to text.
	 * @return array of text lines.
	 * @throws IOException if an I/O error occurs calculating the wrapped text.
	 */
	protected String[] getWrappedText(String text, float fontSize, float availableWidth)
		throws IOException
	{
		ArrayList<String> lines = new ArrayList<String>();
		
		float stringWidth = (m_Font.getStringWidth(text) * fontSize)/1000;
		if (stringWidth > availableWidth)
		{
			String token;
			float wordWidth;
			StringBuilder line = new StringBuilder();
			
			StringTokenizer tokenizer = new StringTokenizer(text, " \n", true);
			while (tokenizer.hasMoreTokens())
			{
				token = tokenizer.nextToken();
				
				wordWidth = (m_Font.getStringWidth(token) * fontSize)/1000;
				stringWidth = (m_Font.getStringWidth(line + token) * fontSize)/1000;
				
				if (token.equals(" "))
				{
					if (stringWidth > availableWidth)
					{
						// Start a new line.
						if (line.length() > 0)
						{
							lines.add(line.toString());
							line.delete(0, line.length());
						}
					}
					else
					{
						line.append(token);
					}
				}
				else if (token.equals("\n"))
				{
					// Forced line break - start a new line.
					lines.add(line.toString());
					line.delete(0, line.length());
				}
				else
				{
					// A word.
					
					if ( (wordWidth > availableWidth) || (stringWidth > availableWidth) )
					{
						// Start a new line.
						if (line.length() > 0)
						{
							lines.add(line.toString());
							line.delete(0, line.length());
						}
					}
					
					if (wordWidth <= availableWidth)
					{
						line.append(token);
					}
					else
					{
						// The word is longer than the available width.
						// No option but to break in the middle of a word.
						char[] wordChars = token.toCharArray();
						char wordChar;
						for (int i = 0; i < wordChars.length; i++)
						{
							wordChar = wordChars[i];
							stringWidth = (m_Font.getStringWidth(new String(line) + wordChar) * fontSize)/1000;
							if (stringWidth > availableWidth)
							{
								lines.add(line.toString());
								line.delete(0, line.length());
							}
							line.append(wordChars[i]);
						}
					}
				}
			}
			
			if (line.length() > 0)
			{
				lines.add(line.toString());
			}
		}
		else
		{
			lines.add(text);
		}
		
		return lines.toArray(new String[0]);
	}
	
	
	/**
	 * Class defining the different symbol shapes that can be used to plot notifications.
	 */
	static class Symbol
	{			
		public static Symbol SQUARE = new Symbol(
				new float[]{-1f, 1f, 1f, -1f}, new float[]{-1f, -1f, 1f, 1f});
		
		public static Symbol DIAMOND = new Symbol(
				new float[]{0f, 1f, 0f, -1f}, new float[]{-1f, 0f, 1f, 0f});
		
		public static Symbol ARROW_UP = new Symbol(
				new float[]{-1f, 1f, 0f}, new float[]{-1f, -1f, 1f});
		
		public static Symbol ARROW_DOWN = new Symbol(
				new float[]{0f, 1f, -1f}, new float[]{-1f, 1f, 1f});
		
		public static Symbol DIABOLO_HORIZONTAL = new Symbol(
				new float[]{-1f, 0f, 1f, 1f, 0f, -1f}, new float[]{-1f, 0f, -1f, 1f, 0f, 1f});
		
		public static Symbol DIABOLO_VERTICAL = new Symbol(
				new float[]{-1f, 1f, 0f, 1f, -1f, 0f}, new float[]{-1f, -1f, 0f, 1f, 1f, 0f});
		
		public static Symbol CIRCLE = new Symbol(
				new float[]{-0.33f, 0.33f, 1f, 1f, 0.33f, -0.33f, -1f, -1f}, 
				new float[]{-1f, -1f, -0.33f, 0.33f, 1f, 1f, 0.33f, -0.33f});
		
		public static Symbol CROSS = new Symbol(
				new float[]{-0.33f, 0.33f, 0.33f, 1f, 1f, 0.33f, 0.33f, -0.33f, -0.33f, -1f, -1f, -0.33f}, 
				new float[]{-1f, -1f, -0.33f, -0.33f, 0.33f, 0.33f, 1f, 1f, 0.33f, 0.33f, -0.33f, -0.33f});
		
		public static Symbol CROSS_DIAGONAL = new Symbol(
				new float[]{-0.66f, 0f, 0.66f, 1f, 0.33f, 1f, 0.66f, 0f, -0.66f, -1f, -0.33f, -1f}, 
				new float[]{-1f, -0.33f, -1f, -0.66f, 0f, 0.66f, 1f, 0.33f, 1f, 0.66f, 0f, -0.66f});
		
		public static Symbol STAR = new Symbol(
				new float[]{0f, 0.33f, 1f, 0.33f, 1f, 0.33f, 0f, -0.33f, -1f, -0.33f, -1f, -0.33f}, 
				new float[]{-1f, -0.33f, -0.33f, 0f, 0.33f, 0.33f, 1f, 0.33f, 0.33f, 0f, -0.33f, -0.33f});
		
		public static Symbol LINE = new Symbol(
				new float[]{-1f, 5f, 5f, -1f}, new float[]{-0.25f, -0.25f, 0.25f, 0.25f});
		
		
		private float[] m_XCoords;
		private float[] m_YCoords;
		
		
		/**
		 * Creates a new symbol with points at the specified coordinates. Coordinates
		 * are relative to the centre of the symbol being at (0,0), with the symbol
		 * shape constrained within a square of 2 unit sides.
		 * @param xCoords the x coordinates of the symbol points.
		 * @param yCoords the y coordinates of the symbol points.
		 */
		private Symbol(float[] xCoords, float[] yCoords)
		{
			m_XCoords = xCoords;
			m_YCoords = yCoords;
		}
		
		
		/**
		 * Returns the x coordinates of the symbol when plotting on the chart at the
		 * specified position, and where the symbol size is multiplied by the
		 * given factor.
		 * @param xCenter x coordinate of the symbol's centre point.
		 * @param sizeMultiplier multiplier by which default symbol size is scaled.
		 * @return the x coordinates of the polygon points, in the PDF page space, 
		 * 		where (0,0) is the bottom left hand corner of the page.
		 */
		public float[] getChartXCoordinates(float xCenter, float sizeMultiplier)
		{
			float[] chartX = new float[m_XCoords.length];
			
			for (int i = 0 ; i < m_XCoords.length; i++)
			{
				chartX[i] = xCenter + (m_XCoords[i] * sizeMultiplier);
			}
			
			return chartX;
		}
		
		
		/**
		 * Returns the y coordinates of the symbol when plotting on the chart at the
		 * specified position, and where the symbol size is multiplied by the
		 * given factor.
		 * @param yCenter y coordinate of the symbol's centre point.
		 * @param sizeMultiplier multiplier by which default symbol size is scaled.
		 * @return the x coordinates of the polygon points, in the PDF page space, 
		 * 		where (0,0) is the bottom left hand corner of the page.
		 */
		public float[] getChartYCoordinates(float yCenter, float sizeMultiplier)
		{
			float[] chartY = new float[m_YCoords.length];
			
			for (int i = 0 ; i < m_YCoords.length; i++)
			{
				chartY[i] = yCenter + (m_YCoords[i] * sizeMultiplier);
			}
			
			return chartY;
		}
	}
	
	
	@SuppressWarnings("deprecation")
    public static void main(String[] args)
	{
		try
		{	
		//	String title = "Details on incident at 2010-04-08 11:35:00";
			String title = "Details on incident at 2010-04-08 11:35:00 : " +
				"Features in active metric (mdhmon) on 2 sources, plus 3 other time series features (2 data types)";
			CausalityPDFChartWriter chartWriter = new CausalityPDFChartWriter(
					title, Locale.getDefault(), TimeZone.getDefault());
			
			Date minTime = new Date(2010, 1, 10, 8, 0) ;
			Date maxTime = new Date(2010, 1, 10, 8, 14) ;
			chartWriter.setDateRange(minTime, maxTime);
			
			chartWriter.startNewPage();
			chartWriter.drawKeyHeader();
			
			chartWriter.drawTimeMarker(new Date(2010, 1, 10, 8, 10));
			
			FileOutputStream stream = new FileOutputStream("c:/pdfBoxChart.pdf");
			chartWriter.write(stream);
			chartWriter.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
