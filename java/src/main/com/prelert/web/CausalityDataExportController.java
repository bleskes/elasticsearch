/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CSVContext;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.IncidentDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityData;
import com.prelert.data.CausalityExportConfig;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.DateTimeFormatPatterns;
import com.prelert.data.Evidence;
import com.prelert.data.ProbableCause;
import com.prelert.data.ProbableCauseCollection;
import com.prelert.data.ProbableCauseDataPoint;
import static com.prelert.data.PropertyNames.*;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.server.CausalityDataUtilities;
import com.prelert.server.CausalityPDFChartWriter;


/**
 * Spring <code>MultiActionController</code> for handling requests for the export 
 * of causality data. It contains methods for exporting data to various file types,
 * such as CSV and PDF, with the name of the method mapping to the export file type.
 * 
 * @author Pete Harverson
 */
public class CausalityDataExportController extends MultiActionController
{
	static Logger s_Logger = Logger.getLogger(CausalityDataExportController.class);
	
	private CausalityDAO 	m_CausalityDAO;
	private IncidentDAO 	m_IncidentDAO;
	private TimeSeriesDAO	m_TimeSeriesDAO;
	private EvidenceDAO		m_EvidenceDAO;
	
	protected static final String FILE_NAME_DATE_PATTERN = "yyyyMMddHHmmss";
	
	
	/**
	 * Exports causality data to the comma-separated values (CSV) file type.
	 * @param request current servlet request.
	 * @param response current servlet response.
	 * @param exportConfig configuration object encapsulating the properties of the
	 * 		causality export that is being handled by the controller.
	 */
	public void csv(HttpServletRequest request, HttpServletResponse response, 
			CausalityExportConfig exportConfig) throws Exception
    {
		s_Logger.debug("csv() for config " + exportConfig);
		
		ArrayList<ProbableCauseDataPoint> dataItems = 
			new ArrayList<ProbableCauseDataPoint>();
		
		// Get the list of probable causes.
		int evidenceId = exportConfig.getEvidenceId();
		if (evidenceId >= 0)
		{
			List<ProbableCause> probableCauses = m_CausalityDAO.getProbableCausesForExport(
					evidenceId, exportConfig.getMetricsTimeSpan(), true);
			
			// Convert to list of ProbableCauseDataPoints ready for exporting.
			DataSourceType dsType;
			List<TimeSeriesDataPoint> timeSeriesPoints;
			for (ProbableCause probableCause : probableCauses)
			{
				dsType = probableCause.getDataSourceType();
				
				if (dsType.getDataCategory() == DataSourceCategory.TIME_SERIES)
				{
					// For time series probable causes, obtain the data points for the time
					// span specified in the export config.
					Date minTime = new Date(exportConfig.getMinTime());
					Date maxTime = new Date(exportConfig.getMaxTime());
					
					timeSeriesPoints = m_TimeSeriesDAO.getDataPointsRaw(
							probableCause.getTimeSeriesId(), minTime, maxTime);
					
					for (TimeSeriesDataPoint timeSeriesPoint : timeSeriesPoints)
					{
						dataItems.add(new ProbableCauseDataPoint(probableCause, timeSeriesPoint));
					}
				}
				else
				{
					// For notifications, create one ProbableCauseDataPoint per ProbableCause.
					dataItems.add(new ProbableCauseDataPoint(probableCause, null));
				}
			}
			
			s_Logger.debug("csv() number of rows to export " + dataItems.size());
		}
		
		writeToCSVFormat(dataItems, exportConfig, request, response);
    }
	
	
	/**
	 * Writes the supplied list of <code>ProbableCauseDataPoint</code> objects
	 * to CSV format.
	 * @param dataItems list of ProbableCauseDataPoint objects for export.
	 * @param exportConfig configuration object encapsulating the properties of the
	 * 		causality export that is being handled by the controller.
	 * @param request current servlet request.
	 * @param response servlet response to write the CSV output to.
	 * @throws IOException if an I/O error occurs.
	 */
	protected void writeToCSVFormat(
			List<ProbableCauseDataPoint> dataItems, CausalityExportConfig exportConfig, 
			HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		// Set up header for the file as a map of property name versus header label.
		LinkedHashMap<String, String> columnMap = new LinkedHashMap<String, String>();
		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", request.getLocale());
		columnMap.put(TYPE, bundle.getString("property.type"));
		columnMap.put(CATEGORY, bundle.getString("property.category"));
		columnMap.put(TIME, bundle.getString("property.time"));
		columnMap.put(DESCRIPTION, bundle.getString("property.description"));
		columnMap.put(SOURCE, bundle.getString("property.source"));
		columnMap.put(COUNT, bundle.getString("property.count"));
		columnMap.put(SIGNIFICANCE, bundle.getString("property.influence"));
		columnMap.put(MAGNITUDE, bundle.getString("property.magnitude"));
		columnMap.put(METRIC, bundle.getString("property.metric"));
		columnMap.put(VALUE, bundle.getString("property.value"));
		columnMap.put(SEVERITY, bundle.getString("property.severity"));
		columnMap.put(ATTRIBUTES, bundle.getString("property.attributes"));

		
		// Set up the cell processors for each of the fields.
		TimeZone timeZone = TimeZone.getTimeZone(exportConfig.getTimeZoneID());
		ConvertNullTo nullProcessor = new ConvertNullTo("");
		TimeZoneFmtDate timeProcessor = new TimeZoneFmtDate(
				DateTimeFormatPatterns.SECOND_PATTERN, timeZone);
		
		
		CellProcessor[] cellProcessors = new CellProcessor[] { 
				nullProcessor, nullProcessor, timeProcessor, 
				nullProcessor, nullProcessor, null, null, null, nullProcessor, 
				nullProcessor, new CSVSeverityCellProcessor(), 
				new CSVAttributesCellProcessor()
		};    
		
		// Write the header line, followed by the probable cause data.
		String fileName = getFileName(exportConfig);
		response.setContentType("text/csv");
		response.setHeader("Content-disposition", "attachment; filename=" + fileName);
		
		PrintWriter writer = response.getWriter();
		CsvBeanWriter csvBeanWriter = new CsvBeanWriter(writer, CsvPreference.STANDARD_PREFERENCE);    
		csvBeanWriter.writeHeader(columnMap.values().toArray(new String[columnMap.size()]));    
		
		for (ProbableCauseDataPoint dataItem : dataItems)
		{
			csvBeanWriter.write(dataItem, 
					columnMap.keySet().toArray(new String[columnMap.size()]), 
					cellProcessors);    
		}
		
		csvBeanWriter.close();   
		
		writer.close();
	}
	
	
	/**
	 * Exports causality data to the portable document format (PDF) file type.
	 * @param request current servlet request.
	 * @param response current servlet response.
	 * @param exportConfig configuration object encapsulating the properties of the
	 * 		causality export that is being handled by the controller.
	 */
    public void pdf(HttpServletRequest request, HttpServletResponse response,
    		CausalityExportConfig exportConfig) throws Exception
    {
    	s_Logger.debug("pdf() for config " + exportConfig);
    	
    	List<ProbableCauseCollection> collectionList =
    		new ArrayList<ProbableCauseCollection>();
    	
    	int evidenceId = exportConfig.getEvidenceId();
    	if (evidenceId >= 0)
    	{
    		int timeSpanSecs = exportConfig.getMetricsTimeSpan();
	    	List<ProbableCause> probableCauses = 
	    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs, true);
	    	
	    	// For time series probable causes, set a generic, localized
	    	// 'Features in xxx metric' description for aggregation.
	    	Locale requestLocale = request.getLocale();
			ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", requestLocale);
			String featuresPattern = bundle.getString("incident.description.featuresIn");
			MessageFormat featuresFormat;
	    	for (ProbableCause probCause : probableCauses)
	    	{
	    		if (probCause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
	    		{
	    			Object[] metricArg = {probCause.getMetric()};
	    			featuresFormat = new MessageFormat(featuresPattern, requestLocale);
	    			probCause.setDescription(featuresFormat.format(metricArg));
	    		}
	    	}
	    	
	    	// Aggregate probable causes by type and description.
	    	CausalityDataUtilities dataUtilities = CausalityDataUtilities.getInstance();
	    	collectionList = dataUtilities.aggregateProbableCauses(probableCauses, evidenceId);
	    	Collections.sort(collectionList, new CollectionComparator());
    	}
    	
		writeToPDFFormat(collectionList, exportConfig, request, response);
		
    }
    
    
    /**
	 * Plots the supplied list of aggregated probable cause data on a chart, and
	 * writes the chart and key out to PDF format.
	 * @param collectionList the list of aggregated probable cause data to plot
	 * 		on the PDF chart.
	 * @param exportConfig configuration object encapsulating the properties of the
	 * 		causality export that is being handled by the controller.
	 * @param request current servlet request.
	 * @param response servlet response to write the PDF output to.
	 * @throws IOException if an I/O error occurs.
	 */
	protected void writeToPDFFormat(
			List<ProbableCauseCollection> collectionList, CausalityExportConfig exportConfig, 
			HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		s_Logger.debug("writeToPDFFormat() exportConfig: " + exportConfig);
		TimeZone zone = TimeZone.getTimeZone(exportConfig.getTimeZoneID());	
		
		CausalityPDFChartWriter chartWriter = new CausalityPDFChartWriter(
				exportConfig.getTitle(), request.getLocale(), zone);
		
		Date minTime = new Date(exportConfig.getMinTime());
		Date maxTime = new Date(exportConfig.getMaxTime());
		chartWriter.setDateRange(minTime, maxTime);
		
		// Mark the midpoint, which should correspond to the incident time.
		long midPointTimeMs = (exportConfig.getMinTime() + exportConfig.getMaxTime())/2;
		chartWriter.drawTimeMarker(new Date(midPointTimeMs));
		
		// Plot notifications.
		List<Integer> notificationIds = exportConfig.getShowNotifications();
		if (notificationIds != null)
		{
			// Get the names of the attributes by which the data has been related.
			// Remove type, source and description.
			List<String> attributeNames = 
				m_IncidentDAO.getIncidentAttributeNames(exportConfig.getEvidenceId());
			attributeNames.remove(TYPE);
			attributeNames.remove(SOURCE);
			attributeNames.remove(DESCRIPTION);
			
			Evidence notification;
			Evidence latestEv;
			Evidence earliestEv;
			List<Attribute> attributes;
			Object attributeValue;
			CausalityData notificationData;
			
			for (int evidenceId : notificationIds)
			{
				// Get the start and end point notifications with the same
				// type, description, source and attributes.
				notification = m_EvidenceDAO.getEvidenceSingle(evidenceId);
				
				attributes = null;
				if (attributeNames != null)
				{
					attributes = new ArrayList<Attribute>();
					for (String attributeName : attributeNames)
					{
						attributeValue = notification.get(attributeName);
						if (attributeValue != null)
						{
							attributes.add(new Attribute(attributeName, attributeValue.toString()));
						}
					}
				}
				
				earliestEv = CausalityDataUtilities.getInstance().getEarliestEvidence(
						evidenceId, notification.getDataType(), 
						notification.getDescription(), notification.getSource(), 
						attributes, m_CausalityDAO);
				
				latestEv = CausalityDataUtilities.getInstance().getLatestEvidence(
						evidenceId, notification.getDataType(), 
						notification.getDescription(), notification.getSource(), 
						attributes, m_CausalityDAO);

				
				notificationData = new CausalityData();
				notificationData.setDataSourceType(new DataSourceType(
						notification.getDataType(), DataSourceCategory.NOTIFICATION));
				notificationData.setDescription(notification.getDescription());
				notificationData.setSource(notification.getSource());
				notificationData.setStartTime(earliestEv.getTime());
				notificationData.setEndTime(latestEv.getTime());
				notificationData.setAttributes(attributes);
				
				chartWriter.addNotifications(notificationData);
			}
		}
		
		// Plot time series.
		List<Integer> seriesIdsToShow = exportConfig.getShowSeries();
		if (seriesIdsToShow != null && seriesIdsToShow.size() > 0)
		{
			List<ProbableCause> probableCauses;
			List<TimeSeriesDataPoint> timeSeriesPoints;
			int timeSeriesId;
			double yScaling = exportConfig.getYAxisScaling();
			
			for (ProbableCauseCollection collection : collectionList)
			{
				if (collection.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
				{
					probableCauses = collection.getProbableCauses();
					for (ProbableCause probCause : probableCauses)
					{
						timeSeriesId = probCause.getTimeSeriesId();
						if (seriesIdsToShow.contains(timeSeriesId))
						{
							// For time series probable causes, obtain the data points for the time
							// span specified in the export config.
							timeSeriesPoints = m_TimeSeriesDAO.getDataPointsRaw(
									probCause.getTimeSeriesId(), minTime, maxTime);
							probCause.setScalingFactor(probCause.getScalingFactor() * yScaling);
							
							chartWriter.addTimeSeries(probCause, timeSeriesPoints);
						}
					}
				}
			}
		}
		
		String fileName = getFileName(exportConfig);
		response.setContentType("application/pdf");
		response.setHeader("Content-disposition", "attachment; filename=" + fileName);

		OutputStream out = response.getOutputStream(); 
		chartWriter.write(out);
		
		chartWriter.close();
		out.close();
	}
    
    
    /**
	 * Sets the data access object to be used to obtain causality data.
	 * @param causalityDAO the data access object for causality data.
	 */
	public void setCausalityDAO(CausalityDAO causalityDAO)
	{
		m_CausalityDAO = causalityDAO;
	}
	
	
	/**
	 * Sets the data access object to be used to obtain incident data.
	 * @param incidentDAO the data access object for incident data.
	 */
	public void setIncidentDAO(IncidentDAO incidentDAO)
	{
		m_IncidentDAO = incidentDAO;
	}
	
	
	/**
	 * Returns the data access object being used for obtaining causality data.
	 * @return the data access object for causality data.
	 */
	public CausalityDAO getCausalityDAO()
	{
		return m_CausalityDAO;
	}
	
	
	/**
	 * Returns the data access object being used for obtaining time series data.
	 * @return the data access object for time series data.
	 */
	public TimeSeriesDAO getTimeSeriesDAO()
    {
    	return m_TimeSeriesDAO;
    }


	/**
	 * Sets the data access object to be used to obtain time series data.
	 * @param timeSeriesDAO the data access object for time series data.
	 */
	public void setTimeSeriesDAO(TimeSeriesDAO timeSeriesDAO)
    {
		m_TimeSeriesDAO = timeSeriesDAO;
    }
	
	
	/**
	 * Sets the data access object to be used to obtain evidence data.
	 * @param evidenceDAO the data access object for evidence data.
	 */
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the data access object being used to obtain evidence data.
	 * @return the data access object for evidence data.
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	
	/**
	 * Creates and returns the file name to set in the Http response header
	 * for the specified export configuration.
	 * @param exportConfig configuration object encapsulating the properties 
	 * 		of the causality export.
	 * @return file name, including extension, based on the time of the incident.
	 */
	protected String getFileName(CausalityExportConfig exportConfig)
	{
		// Base the file name on the time of the incident.
		// Incident time will correspond to midpoint of times in config.
		long midPointTimeMs = (exportConfig.getMinTime() + exportConfig.getMaxTime())/2;
		SimpleDateFormat dateFormatter = new SimpleDateFormat(FILE_NAME_DATE_PATTERN);
		StringBuilder strRep = new StringBuilder();
		strRep.append(dateFormatter.format(new Date(midPointTimeMs)));
		strRep.append('.');
		strRep.append(exportConfig.getFileType().toLowerCase());
		
		return strRep.toString();
	}
	
	
	/**
	 * Cell processor for outputting a list of Attribute objects when
	 * exporting data to CSV file format.
	 */
	class CSVAttributesCellProcessor extends CellProcessorAdaptor
	{

		public CSVAttributesCellProcessor()
		{
			super();
		}


		@SuppressWarnings("unchecked")
        @Override
        public Object execute(Object value, CSVContext context)
        {
			StringBuilder strRep = new StringBuilder();
			
			List<Attribute> attributes = (List<Attribute>)value;
			if (attributes != null)
			{
				for (Attribute attribute : attributes)
				{
					strRep.append(attribute.getAttributeName());
					strRep.append('=');
					strRep.append(attribute.getAttributeValue());
					strRep.append(';');
				}
			}
			
			return next.execute(strRep.toString(), context);
        }
	}
	
	
	/**
	 * Extension of the Super CSV FmtDate CellProcessor to convert a date into a
	 * formatted String, with the calendar for the DateFormat object set to a 
	 * supplied time zone.
	 */
	class TimeZoneFmtDate extends FmtDate
	{		
		
		/**
		 * Creates a new TimeZoneFmtDate date cell processor.
		 * @param format the pattern describing the date and time format.
		 * @param timeZone time zone for the calendar of the DateFormat object.
		 */
		public TimeZoneFmtDate(String pattern, TimeZone timeZone)
		{
			super(pattern);
			this.formatter.setTimeZone(timeZone);
		}
		
	}
	
	
	/**
	 * Cell processor for exporting severity data to CSV file format.
	 */
	class CSVSeverityCellProcessor extends CellProcessorAdaptor
	{

		public CSVSeverityCellProcessor()
		{
			super(new ConvertNullTo(""));
		}


		@Override
		public Object execute(Object value, CSVContext context)
		{
			String severityStr = null;

			if (value != null)
			{
				severityStr = value.toString().toLowerCase();
			}

			return next.execute(severityStr, context);
		}
	}
	
	
	/**
     * Comparator which sorts ProbableCauseCollection alphabetically by data source
     * name and then by description.
     */
    class CollectionComparator implements Comparator<ProbableCauseCollection>
    {

		@Override
        public int compare(ProbableCauseCollection collection1,
        		ProbableCauseCollection collection2)
        {
			// Compare by peak value as they will appear on the chart.
        	String str1 = collection1.getDataSourceType().getName();
        	String str2 = collection2.getDataSourceType().getName();
        	
        	int compare = str1.compareToIgnoreCase(str2);
        	
        	if (compare == 0)
        	{
        		str1 = collection1.getProbableCause(0).getDescription();
        		str2 = collection2.getProbableCause(0).getDescription();
        		
        		compare = str1.compareToIgnoreCase(str2);
        	}
        	
        	return compare;
        }
    	
    }

}
