package demo.app.server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import demo.app.dao.EvidenceDAO;


public class DailyReportWriter
{
	
	static Logger logger = Logger.getLogger(DailyReportWriter.class);
	
	
	public DailyReportWriter()
	{
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"applicationContext.xml"});
		EvidenceDAO evidenceDAO = (EvidenceDAO) context.getBean("evidenceDAO");
	}
	
	
	public HSSFWorkbook loadTemplate(String fileName)
	{
		HSSFWorkbook wb = null;
		
        try
        {
        	FileInputStream inputStream = new FileInputStream(fileName);
        	POIFSFileSystem fileSystem = new POIFSFileSystem(inputStream);
        	wb = new HSSFWorkbook(fileSystem);
        	
        	logger.debug("Loaded template: " + fileName);
        }
        catch (IOException e)
        {
        	logger.error("Error loading template file: " + e);
        }
        
        return wb;
	}
	
	
	/**
	 * Writes notification data to the specified workbook.
	 * @param wb template daily report workbook.
	 */
	public void writeNotificationData(HSSFWorkbook wb)
	{
		HSSFSheet notifSheet = wb.getSheet("Notifications");
		
		HSSFRow criticalRow = notifSheet.getRow(1);
		HSSFCell criticalValCell = criticalRow.getCell(1);
		criticalValCell.setCellValue(560);
		
		HSSFRow majorRow = notifSheet.getRow(2);
		HSSFCell majorValCell = majorRow.getCell(1);
		majorValCell.setCellValue(1012);
		
		HSSFRow minorRow = notifSheet.getRow(3);
		HSSFCell minorValCell = minorRow.getCell(1);
		minorValCell.setCellValue(876);
		
		HSSFRow warningRow = notifSheet.getRow(4);
		HSSFCell warningValCell = warningRow.getCell(1);
		warningValCell.setCellValue(9716);
		
		HSSFRow unknownRow = notifSheet.getRow(5);
		HSSFCell unknownValCell = unknownRow.getCell(1);
		unknownValCell.setCellValue(1761);
		
	}
	
	
	public void writeReport(HSSFWorkbook wb)
	{
	    FileOutputStream fileOut;
        try
        {
        	String fileName = "c:/work/daily_report.xls";
	        fileOut = new FileOutputStream(fileName);
	        wb.write(fileOut);
		    fileOut.close();
		    
		    logger.debug("Written file: " + fileName);
        }
        catch (IOException e)
        {
        	logger.error("Error writing daily report file: " + e);
        }
	}
	
	
	/**
	 * Creates a new daily report from scratch.
	 */
	public void createReport()
	{
		// Create a new workbook, and two worksheets.
		HSSFWorkbook wb = new HSSFWorkbook();
	    HSSFSheet sheet1 = wb.createSheet("Notifications");
	    HSSFSheet sheet2 = wb.createSheet("User Usage");
	    
	    logger.debug("Number of sheets in workbook: " + wb.getNumberOfSheets());
	    
	    // Write out some data to the Notifications worksheet.
	    HSSFRow headerRow = sheet1.createRow(0);
	    headerRow.createCell(0, HSSFCell.CELL_TYPE_STRING).setCellValue(new HSSFRichTextString("Severity"));
	    headerRow.createCell(1, HSSFCell.CELL_TYPE_STRING).setCellValue(new HSSFRichTextString("Number of Events"));
	    
	    HSSFRow row1 = sheet1.createRow(1);
	    HSSFRow row2 = sheet1.createRow(2);
	    HSSFRow row3 = sheet1.createRow(3);
	    HSSFRow row4 = sheet1.createRow(4);
	    HSSFRow row5 = sheet1.createRow(5);
	    
	    
	    HSSFCellStyle criticalStyle = wb.createCellStyle();
	    criticalStyle.setFillForegroundColor(new HSSFColor.RED().getIndex()); 
	    criticalStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	    
	    HSSFCellStyle majorStyle = wb.createCellStyle();
	    majorStyle.setFillForegroundColor(new HSSFColor.LIGHT_ORANGE().getIndex()); 
	    majorStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	    
	    HSSFCellStyle minorStyle = wb.createCellStyle();
	    minorStyle.setFillForegroundColor(new HSSFColor.YELLOW().getIndex()); 
	    minorStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	    
	    HSSFCellStyle warningStyle = wb.createCellStyle();
	    warningStyle.setFillForegroundColor(new HSSFColor.PALE_BLUE().getIndex()); 
	    warningStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	    
	    HSSFCellStyle unknownStyle = wb.createCellStyle();
	    unknownStyle.setFillForegroundColor(new HSSFColor.LAVENDER().getIndex()); 
	    unknownStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	    
	    
	    HSSFCell cell10 = row1.createCell(0, HSSFCell.CELL_TYPE_STRING);
	    cell10.setCellValue(new HSSFRichTextString("critical"));
	    cell10.setCellStyle(criticalStyle);
	    
	    HSSFCell cell11 = row1.createCell(1, HSSFCell.CELL_TYPE_NUMERIC);
	    cell11.setCellValue(540);
	    cell11.setCellStyle(criticalStyle);
	    
	    
	    HSSFCell cell20 = row2.createCell(0, HSSFCell.CELL_TYPE_STRING);
	    cell20.setCellValue(new HSSFRichTextString("major"));
	    cell20.setCellStyle(majorStyle);
	    
	    HSSFCell cell21 = row2.createCell(1, HSSFCell.CELL_TYPE_NUMERIC);
	    cell21.setCellValue(156);
	    cell21.setCellStyle(majorStyle);
	    
	    
	    HSSFCell cell30 = row3.createCell(0, HSSFCell.CELL_TYPE_STRING);
	    cell30.setCellValue(new HSSFRichTextString("minor"));
	    cell30.setCellStyle(minorStyle);
	    
	    HSSFCell cell31 = row3.createCell(1, HSSFCell.CELL_TYPE_NUMERIC);
	    cell31.setCellValue(1285);
	    cell31.setCellStyle(minorStyle);
	    
	    
	    HSSFCell cell40 = row4.createCell(0, HSSFCell.CELL_TYPE_STRING);
	    cell40.setCellValue(new HSSFRichTextString("warning"));
	    cell40.setCellStyle(warningStyle);
	    
	    HSSFCell cell41 = row4.createCell(1, HSSFCell.CELL_TYPE_NUMERIC);
	    cell41.setCellValue(13677);
	    cell41.setCellStyle(warningStyle);
	    
	    
	    HSSFCell cell50 = row5.createCell(0, HSSFCell.CELL_TYPE_STRING);
	    cell50.setCellValue(new HSSFRichTextString("unknown"));
	    cell50.setCellStyle(unknownStyle);
	    
	    HSSFCell cell51 = row5.createCell(1, HSSFCell.CELL_TYPE_NUMERIC);
	    cell51.setCellValue(144);
	    cell51.setCellStyle(unknownStyle);
	    

	    writeReport(wb);
	}
	

	public static void main(String[] args)
	{
		// main() method for standalone testing.
		
		// Configure the log4j logging properties.
		PropertyConfigurator.configure("C:/eclipse/workspace/Ext GWT/config/log4j.properties");
		
		DailyReportWriter reportWriter = new DailyReportWriter();
		//reportWriter.writeReport();
		HSSFWorkbook wb = reportWriter.loadTemplate("c:/work/daily_report_template.xls");
		reportWriter.writeNotificationData(wb);
		reportWriter.writeReport(wb);
	}

}
