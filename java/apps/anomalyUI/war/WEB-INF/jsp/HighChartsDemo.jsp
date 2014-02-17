<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
    <title>Highcharts JS Demo</title>
    <link rel="stylesheet" type="text/css" href="css/prelert.css">
    <link rel="stylesheet" type="text/css" href="extjs/resources/css/ext-all.css">
    <link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon">
    <link rel="icon" href="prelert_icon.ico" type="image/x-icon">
    
    <script>

    function processPrelertAPIData()
    {
    	document.forms[0].action = "processAPIData.do";
    	document.forms[0].submit();
    }
    
    
    function viewSavedJobData()
    {
    	document.forms[1].submit();
    }

    </script>
    
  </head>  
   
<body class="prl-body-13">
    
    <div class="prl-appHeader-13">
	    <div class="prl-appLogoContainer"><a href="/prelertAnomalyUI" class="prl-appLogo-13"></a><h1>Prelert Anomaly UI</h1>
        </div>
    </div>
	    
	<div id="body-content" style="margin: 10px;">
	    
	    <h1 style="font-size: 18px">Select input</h1>
			    
	    <p>
	        Use this page to select the JSON file containing anomaly data outputted by prelert_autodetect_api.
	    </p>
	    
	    <form action="processJSONData.do" method="post" enctype="multipart/form-data">
	    
		    <label>Select file:</label>
            <input type="hidden" name="viewName" value="AnomalyResultsHighcharts" /> 
		    <input type="file" value="" size="40" name="fileData" id="fileData" /> 
             
		    <p>
            </p>
            
            <button type="button" onclick="processPrelertAPIData();">Upload file</button>
	    
	    </form>
        
        <p style="padding-top: 30px;">
            Or select from one of the JSON files saved on the server.
        </p>
        
        <form action="jobResults.do" method="get">
        
            <label>Select file:</label>
            <select name="fileName">
                <option value="wikibyurl_day.json" selected="selected">Wiki count by URL (Daily)</option>
                <option value="wikibyurl_6hours.json">Wiki count by URL (6 hourly)</option>
                <option value="prelertapi_test.json">Random hourly data</option>
            </select>
            
            <p>
            </p>
            
            <label>Select charting library:</label>
            <select name="viewName">
                <option value="AnomalyResultsHighcharts" selected="selected">Highcharts</option>
                <option value="AnomalyResultsFlot">Flot</option>
            </select>
            
            <p>
            </p>
            
            <button type="button" onclick="viewSavedJobData();">View results</button>
        
        </form>
	    
	</div>
    
</body>
     
    
</html>
