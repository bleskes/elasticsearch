<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
    <title>Elasticsearch Demo</title>
    <link rel="stylesheet" type="text/css" href="css/prelert.css">
    <link rel="stylesheet" type="text/css" href="extjs/resources/css/ext-all.css">
    <link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon">
    <link rel="icon" href="prelert_icon.ico" type="image/x-icon">
    
    <script>
    
    
    function viewJobData()
    {
    	document.forms[0].submit();
    }

    </script>
    
  </head>  
   
<body class="prl-body-13">
    
    <div class="prl-appHeader-13">
	    <div class="prl-appLogoContainer"><a href="/prelertAnomalyUI/ElasticsearchDemo.do" class="prl-appLogo-13"></a><h1>Prelert Anomaly UI</h1>
        </div>
    </div>
	    
	<div id="body-content" style="margin: 10px;">
	    
	    <h1 style="font-size: 18px">View results stored in elasticsearch</h1>
			    
	    <p>
	        Use this page to view results processed by the Prelert API stored in elasticsearch.
	    </p>
        
        <h2 style="font-size: 18px">Select input</h2>
	    
	    <form action="viewElasticsearchJob.do" method="post" enctype="multipart/form-data">
	    
		    <label>Enter job ID:</label>
            <input type="hidden" name="viewName" value="ElasticsearchJobResults" /> 
		    <input type="text" name="jobId" size="30" value="20131213114610-00001">
             
		    <p>
            </p>
            
            <button type="button" onclick="viewJobData();">View results</button>
	    
	    </form>
	    
	</div>
    
</body>
     
    
</html>
