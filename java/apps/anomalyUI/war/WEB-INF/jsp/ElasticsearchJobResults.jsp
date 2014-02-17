<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
    <title>Prelert API Job results</title>
    <link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon">
    <link rel="icon" href="prelert_icon.ico" type="image/x-icon">
    
    <link rel="stylesheet" type="text/css" href="css/prelert.css">
    <link rel="stylesheet" type="text/css" href="extjs/resources/css/ext-all.css">
    
    <script type="text/javascript" src="thirdparty/jquery-1.10.2.min.js"></script>
    <script type="text/javascript" src="thirdparty/highcharts/highcharts.js"></script>  
    <script type="text/javascript" src="thirdparty/highcharts/gray.js"></script>
    <script type="text/javascript" src="extjs/ext-all-debug.js"></script>
    
    <script>
    
    $(function () {
    	
    	var $report= $('#report');
    	
    	$.displayDrilldown = function(bucketTime)
    	{
    	    console.log("Go and get anomaly records for bucket time " + bucketTime); 
    	    
    	    // NB Use $.ajax() instead of of $.getJSON() as easier to set cache to false.
    	    $.ajax({
    	    	dataType: "json",
    	    	url: "services/viewElasticsearchBucket/${jobId}/" + bucketTime,
    	    	cache: false
	    	}).done(function(records) {
				$.setDrilldownGridData(bucketTime, records);
    	   	}).fail(function() {
				console.log("Error obtaining anomaly records for bucket time " + bucketTime);
    	   	});
    	}

    	
        $('#container').highcharts({
        	chart: {
        		zoomType: 'x',
        		events: {
                    selection: function(event) {
                    	console.log("span is ${span}");
                    	
                        if (event.xAxis) {
                            console.log("Selected range min: "+ event.xAxis[0].min +", max: "+ event.xAxis[0].max);
                            var rangeSelected = event.xAxis[0].max - event.xAxis[0].min;
                        	if (rangeSelected < 43200000) {
                        		//event.preventDefault();
                        		console.log("Don't load more data");
                        	}
                        } 
                    }
                }
        	},
            title: {
                text: 'Anomaly Score',
                x: -20 //center
            },
            subtitle: {
                text: 'Prelert API Job ID: ${jobId}',
                x: -20
            },
            xAxis: {
            	 type: 'datetime',
            },
            yAxis: {
                title: {
                    text: 'Anomaly score'
                },
                min: 0,
                plotLines: [{
                    value: 0,
                    width: 1,
                    color: '#808080'
                }]
            },
            tooltip: {
                shared: true
            },
            legend: {
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'middle',
                borderWidth: 0
            },
            plotOptions: {
                series: {
                	allowPointSelect: true,
                    marker: {
                    	radius: 1,
                        states: {
                            hover: {
                                enabled: true,
                                radius: 5,
                            },
                       
                            select: {
                                enabled: true,
                                lineColor: '#e79b3e',
                                radius: 5
                            }
                        }
                    },
                    point: {
                        events: {
                            select: function() {
                                $.displayDrilldown(this.category);
                            }
                        }
                    }
                }
            
            },
            series: [{
                name: 'Anomaly score',
                color: '#e79b3e',
                pointStart: ${startTimeMs},
                pointInterval: ${span},
                data: [
				<c:forEach var="anomalyData" items="${chartData}">${anomalyData.score},</c:forEach>     
                ]
            }],
        });
        
        
        Ext.define('AnomalyRecord', {
            extend: 'Ext.data.Model',
        	fields   : [
                    {name : 'time', type: 'long'  },
                    {name : 'fieldName',    type : 'string'},
                    {name : 'fieldValue',   type : 'string'},
                    {name : 'probability',   type : 'float'},
                    {name : 'anomalyFactor',   type : 'float'},
                    {name : 'metricField',   type : 'string'}
             ]
        });
        
        $.drilldownGridStore = Ext.create('Ext.data.Store', {
            model: 'AnomalyRecord',
            data: []
        });
        
        $.dateRenderer = Ext.util.Format.dateRenderer('M d H:i:s');
        
        $.setDrilldownGridData = function(bucketTime, records)
    	{
        	if ($.drilldownGrid == null) {
        		$.drilldownGrid = $.createDrilldownGrid();
        	}
        	
        	$.drilldownGridStore.removeAll();   	
        	$.drilldownGridStore.loadData(records, false); 
        	var bucketDate = new Date(bucketTime);
        	$.drilldownGrid.setTitle("Details on anomaly at " + $.dateRenderer(bucketDate));
    	}
        
        $.createDrilldownGrid = function() {
        	console.log("Create drilldown grid 1");
        	
        	var drilldownGrid = Ext.create('Ext.grid.Panel', {
                title: 'Details on anomaly',
                store: $.drilldownGridStore,
                columns: [
                    { text: 'time',  dataIndex: 'time', width: 110,
                    	renderer: function(value) {
                    		return $.dateRenderer(new Date(value))
                        }
                    },
                    { text: 'field', dataIndex: 'fieldName', width: 90 },
                    { text: 'value', dataIndex: 'fieldValue', width: 200 },
                    { text: 'probability', dataIndex: 'probability', width:80, 
                    	renderer: function(value) {
                    		if (value >= 0.01)
                    		{
                    			return Ext.Number.toFixed(value, 2) + '%';
                    		}
                    		else
                    		{
                    			return '&lt;0.01%';
                    		}
                        }
                    },
                    { text: 'anomaly score', dataIndex: 'anomalyFactor', width:90, 
                    	renderer: function(value) {
                    		return Ext.Number.toFixed(value, 3);
                        }
                    },
                    { text: 'description', dataIndex: 'metricField', flex: 1, 
                    	renderer: function(value) {
                    		// TODO - use typical/actual values in description.
                    		return 'Unusual value of ' + value + ' ' + Math.floor((Math.random()*1000)) + ', typical value ' + Math.floor((Math.random()*10));
                    	}
                	}
                ],
                height: 300,
                width: '100%',
                style: 'padding-top: 15px;',
                viewConfig: { 
                    stripeRows: false, 
                    enableTextSelection: true,
                    getRowClass: function(record) {
                    	var prob = record.get('probability');
                    	if (prob <= 0.01){
                    		return 'prl-anomalyResultsTableSev1'; 
                    	}
                    	else if (prob <= 1) {
                    		return 'prl-anomalyResultsTableSev2'; 
                    	}
                    	else if (prob <= 3) {
                    		return 'prl-anomalyResultsTableSev3'; 
                    	}
                    	else {
                    		return 'prl-anomalyResultsTableSev4'; 
                    	}
                        
                    } 
                },
                renderTo: anomalyDrilldownDiv
            });
        	
        	// Resize the grid to fit the available width on window resize.
        	Ext.EventManager.onWindowResize(drilldownGrid.doLayout, drilldownGrid);
        	
        	return drilldownGrid;
        	
        }
        
    });

    </script>
    
  </head>  
   
  <body style="height:100%;" class="prl-body-13"> 
  
    <div class="prl-appHeader-13">
        <div class="prl-appLogoContainer"><a href="/prelertAnomalyUI/ElasticsearchDemo.do" class="prl-appLogo-13"></a><h1>Prelert Anomaly UI</h1>
        </div>
    </div>
  
    <div id="body-content" class="prl-bodycontent-13">
  
        <div id="title" class="prl-title-13">
            <h1 style="font-size: 18px">View AutoDetect Results</h1>
    			    
            <p>
                Use this page to view the anomalies that Prelert has detected in one of the jobs you have run with the Prelert API.
            </p>
            
            <!-- 
            <label>Prelert API Job:</label>
            <select name="fileName">
                <option value="wikibyurl_day.json" selected="selected">Wiki count by URL (Daily)</option>
                <option value="wikibyurl_6hours.json">Wiki count by URL (6 hourly)</option>
            </select>
            -->
            
            <p>
            Peaks significantly higher than others indicate periods of anomalous activities. Click on a peak to drill down. 
            </p>
            
        </div>          
                    
        <div id="container" style="width:100%; height:400px;"></div>
        
        <div id="anomalyDrilldownDiv" style="width:100%;"></div>
        
        <div id="report"></div>
        
    </div>
    
  </body> 
  
</html>