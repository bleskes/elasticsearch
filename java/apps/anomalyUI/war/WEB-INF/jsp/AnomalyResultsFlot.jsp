<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
    <title>Prelert Anomaly (Flot chart)</title>
    <link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon">
    <link rel="icon" href="prelert_icon.ico" type="image/x-icon">
    
    <link rel="stylesheet" type="text/css" href="css/prelert.css">
    <link rel="stylesheet" type="text/css" href="extjs/resources/css/ext-all.css">
    
    <script type="text/javascript" src="thirdparty/jquery-1.10.2.min.js"></script>
    <script type="text/javascript" src="thirdparty/flot/jquery.flot.js"></script> 
    <script type="text/javascript" src="thirdparty/flot/jquery.flot.resize.js"></script> 
    <script type="text/javascript" src="thirdparty/flot/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="thirdparty/flot/jquery.flot.time.js"></script> 
    <script type="text/javascript" src="extjs/ext-all-debug.js"></script>
    
    <script>
    
    $(function () {
    	
    	var placeholder = $("#placeholder");
    	var $report= $('#report');
    	
    	// Create the Ext JS formatter for rendering dates in the chart and drilldown.
    	$.dateRenderer = Ext.util.Format.dateRenderer('M d H:i:s');
    	
    	$.displayDrilldown = function(bucketTime)
    	{
    	    console.log("Go and get anomaly records for bucket time " + bucketTime); 
    	    
    	    // NB Use $.ajax() instead of of $.getJSON() as easier to set cache to false.
    	    $.ajax({
    	    	dataType: "json",
    	    	url: "services/anomalyRecords/" + bucketTime,
    	    	cache: false
	    	}).done(function(records) {
				$.setDrilldownGridData(bucketTime, records);
    	   	}).fail(function() {
				console.log("Error obtaining anomaly records for bucket time " + bucketTime);
    	   	});
    	}


		// Build the data in the coordinate format used by Flot.	
		var anomalyData = [{label: "Anomaly score", data: [ <c:forEach var="anomalyData" items="${chartData}">[${anomalyData.timestamp}, ${anomalyData.score}],</c:forEach> ] } ];
		
		var options = {
	   			series: {
	   				lines: {
	   					show: true
	   				},
	   				points: {
	   					show: false
	   				}
	   			},
	   			grid: {
	   				color: '#ffffff',
	   				borderColor: '#6f6f6f',
	   				borderWidth: 1,
	   				hoverable: true,
	   				clickable: true
	   			},
				xaxis: {
					mode: "time",
					timeformat: "%d %b %Y",
					font: {
	    				color: "#ffffff"
					}
				},
				yaxis: {
					min: 0,
					font: {
	    				color: "#ffffff"
					}
				},
				legend: {
					show: true,
				    position: "ne",
				    backgroundColor: "#585a5d",
				    backgroundOpacity: 0.5
				},
				selection: {
					mode: "x"
				}
	   		};
		
		var plot = $.plot("#placeholder", anomalyData, options);
		
		// Show tooltips on point hover.
		$.showTooltip = function(x, y, contents) {
			$("<div id='tooltip'>" + contents + "</div>").css({
				position: "absolute",
				display: "none",
				top: y + 5,
				left: x + 5,
				border: "none",
				padding: "2px",
				"color": "#ffffff",
				"background-color": "#171717",
				opacity: 0.80
			}).appendTo("body").fadeIn(200);
		}
		
		var previousPoint = null;
		$("#placeholder").bind("plothover", function (event, pos, item) {
			if (item) {
				if (previousPoint != item.dataIndex) {

					previousPoint = item.dataIndex;

					$("#tooltip").remove();
					var x = item.datapoint[0].toFixed(2),
					y = item.datapoint[1].toFixed(2);
					var formattedDate = $.dateRenderer(new Date(item.datapoint[0]));
					$.showTooltip(item.pageX, item.pageY,
							formattedDate + "<br/>Anomaly score: " + y);
				}
			} else {
				$("#tooltip").remove();
				previousPoint = null;            
			}
		});
		
		// Load the anomaly drilldown table on point click.
		var previousSelectedItem = null;
		$("#placeholder").bind("plotclick", function (event, pos, item) {
			if (previousSelectedItem != null) {
				plot.unhighlight(previousSelectedItem.series, previousSelectedItem.datapoint);
			}
			
			if (item) {
				plot.highlight(item.series, item.datapoint);
				$.displayDrilldown(item.datapoint[0]);
				previousSelectedItem = item;
			}
		});
        
		placeholder.bind("plotselected", function (event, ranges) {

			plot = $.plot(placeholder, anomalyData, $.extend(true, {}, options, {
				xaxis: {
					min: ranges.xaxis.from,
					max: ranges.xaxis.to
				}
			}));
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
        
        //$.dateRenderer = Ext.util.Format.dateRenderer('M d H:i:s');
        
        $.setDrilldownGridData = function(bucketTime, records)
    	{
        	if ($.drilldownGrid == null) {
        		$.drilldownGrid = $.createDrilldownGrid();
        	}
        	
        	$.drilldownGridStore.removeAll();   	
        	$.drilldownGridStore.loadData(records, false); 
        	var bucketDate = new Date(bucketTime);
        	$.drilldownGrid.setTitle("Details on anomaly at " + $.dateRenderer(bucketDate));
    	};
        
        $.createDrilldownGrid = function() {
        	console.log("Create drilldown grid 1");
        	
        	var drilldownGrid = Ext.create('Ext.grid.Panel', {
                title: 'Details on anomaly',
                store: $.drilldownGridStore,
                columns: [
                    { text: 'time',  dataIndex: 'time', width: 110,
                    	renderer: function(value) {
                    		return $.dateRenderer(new Date(value));
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
        	
        };
        
    });

    </script>
    
  </head>  
   
  <body style="height:100%;" class="prl-body-13"> 
  
    <div class="prl-appHeader-13">
        <div class="prl-appLogoContainer"><a href="/prelertAnomalyUI" class="prl-appLogo-13"></a><h1>Prelert Anomaly UI</h1>
        </div>
    </div>
  
    <div id="body-content" class="prl-bodycontent-13">
  
        <div id="title" class="prl-title-13">
            <h1 style="font-size: 18px">AutoDetect Results using Flot chart</h1>
    			    
            <p>
                This page displays the anomaly results contained in the uploaded file <span style="font-weight:bold;">${fileName}</span>
                in a chart built using the Flot charting library.
            </p>
        </div>          
                    
        <div class="chart-container">
    	     <div id="placeholder" style="width:100%; height:400px;"></div>
    	</div>
        
        <div id="anomalyDrilldownDiv" style="width:100%;"></div>
        
        <div id="report"></div>
        
    </div>
    
  </body> 
  
</html>