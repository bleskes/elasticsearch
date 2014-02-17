<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
    <title>Prelert Anomaly Chart</title>
    <link rel="stylesheet" type="text/css" href="css/prelert.css">
    <link rel="stylesheet" type="text/css" href="extjs/resources/css/ext-all.css">
    <link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon">
    <link rel="icon" href="prelert_icon.ico" type="image/x-icon">
    
    <script type="text/javascript" src="extjs/ext-all-debug.js"></script>
    
    <script>
    
	    Ext.require('Ext.chart.*');
	    Ext.require(['Ext.Window', 'Ext.fx.target.Sprite', 'Ext.layout.container.Fit', 'Ext.window.MessageBox']);
	
	    Ext.onReady(function () {
	    	
	    	Ext.define('Anomaly', {
	            extend: 'Ext.data.Model',
	            fields: ['score', 'time', 'records']
	        });
	    	
	    	Ext.define('AnomalyRecord', {
	            extend: 'Ext.data.Model',
	            fields: ['time', 'fn', 'fv', 'prob', 'sc', 'desc']
	        });
	    	
	    	var _store = Ext.create('Ext.data.Store', {
	            model: 'Anomaly',
	            data: [
	                   
			<c:forEach var="anomalyData" items="${chartData}">
			{ score: ${anomalyData.score}, time: new Date(${anomalyData.time.time}),
			  records: [ 
                <c:forEach var="anomalyRecord" items="${anomalyData.records}">
                {time: new Date(${anomalyRecord.time.time}), fn:'${anomalyRecord.fieldName}', fv:'${anomalyRecord.fieldValue}', prob:${anomalyRecord.probability},
                	sc:${anomalyRecord.anomalyFactor}, desc:'Unusual value of ${anomalyRecord.metricField} ${anomalyRecord.currentMean}, typical value ${anomalyRecord.baselineMean}'},</c:forEach>
	                ]},
        	</c:forEach>
			
	            ]
	        });
	        
	    	var _selectedItem = null;
	    	
	        var _anomalyChart = Ext.create('Ext.chart.Chart', {
	            renderTo: Ext.get("anomalyChartDiv"),
	            width: 1300,
	     	    height: 400,
                style: 'background:#fff',
                animate: true,
                store: _store,
                shadow: true,
                theme: 'Category1',
                enableMask: true,
                mask: 'horizontal',
                listeners: {
                   select: {
                        fn: function(me, selection) {
                            me.setZoom(selection);
                            me.mask.hide();
                        }
                    }
                },
                legend: {
                    position: 'right'
                },
                axes: [{
                    type: 'Numeric',
                    minimum: 0,
                    //maximum: 100,
                    position: 'left',
                    fields: ['score'],
                    title: 'Anomaly score',
                    minorTickSteps: 1,
                    grid: {
                        odd: {
                            opacity: 1,
                            fill: '#ddd',
                            stroke: '#bbb',
                            'stroke-width': 0.5
                        }
                    }
                }, {
                	title: 'Time',
    	              type: 'Time',
    	              position: 'bottom',
    	              fields: ['time'],
    	              step: [Ext.Date.DAY, 1/2],
    	              minorTickSteps: 5,
    	              
    	              dateFormat: 'M d H:i'
                }],
                series: [{
                    type: 'line',
                    highlight: {
                        size: 7,
                        radius: 7
                    },
                    axis: 'left',
                    xField: 'time',
                    yField: 'score',
                    markerConfig: {
                    	type: 'circle',
                        size: 3,
                        radius: 3,
                        fill: '#880000',
                        stroke: '#880000',
                        'stroke-width': 0
                    },
                    style: {
                    	stroke: '#880000'
                    },
                    tips: {
                        trackMouse: true,
                        width: 210,
                        height: 35,
                        renderer: function(storeItem, item) {
                    	    this.setTitle('Date: ' + storeItem.get('time').toLocaleString() + '<br/>Anomaly score: ' + storeItem.get('score'));
                        }
                    }
                }]
            });
	        
	        
	        zoomInToSelected = function() {
	        	console.log("zoomInToSelected()");
	        	if (_selectedItem != null) {
	        		var anomalyDate = _selectedItem.get('time');
	            	console.log("Zoom in on time " + anomalyDate.toLocaleString() + ", with span=" + ${span});
	        	}
	        };
	        
	        
	        // Create a context menu for zooming in/out around points on the chart.
	        var zoomIn = Ext.create('Ext.Action', {
	            iconCls: 'zoom-in-button',
	            text: 'Zoom in',
	            me: this,
	            handler: zoomInToSelected
	        });
	        
	        
	        var menu = Ext.create('Ext.menu.Menu', {
	            items : [
	                zoomIn
	            ]
	        });
	        
	        
	        _anomalyChart.el.on('contextmenu', function(evtObj) {        
	            var left = _anomalyChart.getEl().getLeft();
	            var top = _anomalyChart.getEl().getTop();
	            var item = _anomalyChart.series.getAt(0).getItemForPoint(evtObj.getX() - left, evtObj.getY() - top);
	            if (item != null)
	            {
	            	var anomaly = item.storeItem;
	            	_selectedItem = anomaly;
	            	var anomalyDate = _selectedItem.get('time');
	            	menu.showAt(evtObj.getXY());
	            	evtObj.stopEvent();
	            }
	        }); 
	        
	        
	        var _drilldownGridStore = Ext.create('Ext.data.Store', {
	            model: 'AnomalyRecord',
	            data: []
	        });
	        
	        
	        _anomalyChart.series.getAt(0).on("itemmousedown", function(item) {  
	        	var anomaly = item.storeItem;
            	var anomalyDate = anomaly.get('time');
            	var records = anomaly.get('records');
            	
            	if (this._drilldownGrid == null)
            	{
            		this._drilldownGrid = createDrilldownGrid();
            	}
            	
            	_drilldownGridStore.removeAll();   	
            	_drilldownGridStore.loadData(records, false); 
            	this._drilldownGrid.setTitle("Details on anomaly at " + anomalyDate.toLocaleString());
	        });
	        
	        
	        createDrilldownGrid = function(chartData) {
	        	console.log("Create drilldown grid");
	        	
	        	var drilldownGrid = Ext.create('Ext.grid.Panel', {
	                title: 'Details on anomaly',
	                store: _drilldownGridStore,
	                columns: [
	                    { text: 'time',  dataIndex: 'time', xtype:'datecolumn', format:'M d H:i:s', width: 110 },
                        { text: 'field', dataIndex: 'fn', width: 90 },
                        { text: 'value', dataIndex: 'fv', width: 200 },
                        { text: 'probability', dataIndex: 'prob', width:80, 
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
                        { text: 'anomaly score', dataIndex: 'sc', width:90, 
                        	renderer: function(value) {
	                    		return Ext.Number.toFixed(value, 3);
	                        }
                        },
                        { text: 'description', dataIndex: 'desc', flex: 1 },
	                ],
	                height: 250,
	                width: 1000,
	                viewConfig: { 
	                    stripeRows: false, 
	                    enableTextSelection: true,
	                    getRowClass: function(record) {
	                    	var prob = record.get('prob');
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
	        	
	        	return drilldownGrid;
	        	
	        };
	
	
	        
	    });

    
    </script>
    
</head>
    <body id="docbody">
	    
	    <div class="prl-appHeader">
		    <div class="prl-appLogoContainer"><a href="/prelertAnomalyUI" class="prl-appLogo"></a><h1>Prelert Anomaly UI</h1>
        	</div>
	    </div>
		    
		<div id="body-content" style="margin: 10px;">
	    
	        <h1>Anomaly Chart Results</h1>
			    
	        <p>
	            This page displays the anomaly results contained in the uploaded file <span style="font-weight:bold;">${fileName}</span>
	        </p>
	        
	        <div id="anomalyChartDiv" style="margin: 10px;"></div>
	        
	        <div id="anomalyDrilldownDiv" style="margin: 10px;"></div>
				    
	        <button onclick="window.location.href='AnomalyFileSelect.html'">Process another file</button>
	    
	    </div>
    
    </body>
     
</html>
