/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package demo.app.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.*;
import java.text.AttributedString;
import javax.sql.DataSource;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import demo.app.dao.EpisodeModelDAO;
import demo.app.data.EpisodeGraphEdge;
import demo.app.data.EpisodeGraphNode;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest2;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.graph.util.Pair;


/**
 * Class which builds a graph to display the model of the episode behaviour
 * as determined by the Prelert engine. The graph will consist of a number of networks,
 * with each network representing connected types of evidence. The nodes represent
 * the types of evidence, whilst the edges represent a causal link between the 
 * evidence with a given probability.
 * @author Pete Harverson
 */
public class EpisodeGraphBuilder
{
	static Logger logger = Logger.getLogger(EpisodeGraphBuilder.class);
	
	private EpisodeModelDAO m_EpisodeModelDAO;
	
	
	private static final int MODEL_BOX_WIDTH = 120;
	private static final int MODEL_BOX_HEIGHT = 45;
	private static final Color FILL_COLOR = new Color(100, 157, 215);
	private static final Color OUTLINE_COLOR = new Color(15, 46, 79);
	private static final BasicStroke OUTLINE_STROKE = new BasicStroke(2.0f, 
            BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_MITER);

	
	/**
	 * Creates a new episode graph builder.
	 */
	public EpisodeGraphBuilder()
	{	
		// Create the database connection and load the raw data.
		initialiseDataSource(); 
	}
	
	
	/**
	 * Initialises the data source and the connection to the database.
	 */
	private void initialiseDataSource()
	{
		try
        {
			// Read in DB connection properties from file.
			String propsFile = "episode_viewer.properties";
			InputStream propsStream = ClassLoader.getSystemClassLoader().getResourceAsStream(propsFile);
		
			Properties connectionProps = new Properties();
			connectionProps.load(propsStream);
			
	        DataSource dataSource = BasicDataSourceFactory.createDataSource(connectionProps);
	        EpisodeModelDAO episodeModelDAO = new EpisodeModelDAO();
	        episodeModelDAO.setDataSource(dataSource);
	        
	        // Override default queries if values have been set.
	        String nodesQuery = connectionProps.getProperty("nodesQuery");
	        if (nodesQuery != null)
	        {
	        	episodeModelDAO.setNodesQuery(nodesQuery);
	        }
	        String edgesQuery = connectionProps.getProperty("nodesQuery");
	        if (edgesQuery != null)
	        {
	        	episodeModelDAO.setEdgesQuery(edgesQuery);
	        }
	        
	        setEpisodeModelDAO(episodeModelDAO);
	        
        }
        catch (Exception e)
        {
	        logger.error("Error creating data source", e);
        }
	}
	
	
	/**
	 * Sets the EpisodeModelDAO to be used by the graph builder.
	 * @param episodeModelDAO the data access object for the episode model.
	 */
	public void setEpisodeModelDAO(EpisodeModelDAO episodeModelDAO)
	{
		m_EpisodeModelDAO = episodeModelDAO;
	}
	
	
	/**
	 * Returns the EpisodeModelDAO being used by the graph builder.
	 * @return the data access object for the episode model.
	 */
	public EpisodeModelDAO getEpisodeModelDAO()
	{
		return m_EpisodeModelDAO;
	}
	
	
	/**
	 * Returns the graph containing all the episodes in the Prelert database 
	 * as a series of networks, with each network consisting of one or more nodes.
	 * @return the graph containing all episodes.
	 */
	public Graph<EpisodeGraphNode, EpisodeGraphEdge> getAllEpisodesGraph()
	{
		// Obtain the nodes and edges from the database.
		List<EpisodeGraphNode> graphNodes = getNodes();
		logger.debug("generateGraph() - number of nodes: " + graphNodes.size());
		
		List<EpisodeGraphEdge> graphEdges = getEdges();
		logger.debug("generateGraph() - number of edges: " + graphEdges.size());
		
		return generateGraph(graphNodes, graphEdges);
	}
	
	
	/**
	 * Returns the list of nodes in the episode model, which correspond to the 
	 * evidence descriptions, their ids and severities.
	 * @return list of nodes.
	 */
	public List<EpisodeGraphNode> getNodes()
	{
		return m_EpisodeModelDAO.getNodes();
	}
	
	
	/**
	 * Returns the list of edges in the episode model. Only one edge will be
	 * returned for any connected pair of nodes e.g. if nodes A and B are connected,
	 * then only one edge will be returned for the link between them.
	 * An edge is set to be bi-directional if both A->B and B->A exist, and the max
	 * probability of the link in one direction does not exceed twice the probability
	 * of the reverse direction.
	 * @return list of edges.
	 */
	public List<EpisodeGraphEdge> getEdges()
	{
		// Obtain full list of edges i.e. A->B and B->A if they both exist.
		List<EpisodeGraphEdge> allGraphEdges = m_EpisodeModelDAO.getEdges();	
		
		List<EpisodeGraphEdge> edges = new ArrayList<EpisodeGraphEdge>();
		
		HashMap<String, EpisodeGraphEdge> allEdgesMap = new HashMap<String, EpisodeGraphEdge>();
		String label;
		for (EpisodeGraphEdge edge : allGraphEdges)
		{
			label = new String("" + edge.getFirstId() + "-" + edge.getSecondId());
			allEdgesMap.put(label, edge);
		}
		
		// Determine if the link is directional.
		boolean directional = false;
		String reverseLabel;
		EpisodeGraphEdge reverseEdge;
		for (EpisodeGraphEdge edge : allGraphEdges)
		{
			label = new String("" + edge.getFirstId() + "-" + edge.getSecondId());
			reverseLabel = new String("" + edge.getSecondId() + "-" + edge.getFirstId());
			reverseEdge = allEdgesMap.get(reverseLabel);
			if (reverseEdge == null)
			{
				edges.add(edge);
				edge.setDirectional(true);
			}
			else
			{
				if ( (edge.getProbability() >= reverseEdge.getProbability()) && 
						(edges.contains(reverseEdge) == false) )
				{
					edges.add(edge);
					directional = (edge.getProbability() >= 2*reverseEdge.getProbability());
					edge.setDirectional(directional);
				}
			}
		}
		
		return edges;
	}
	
	

	/**
	 * Generates the episode graph for the evidence types represented by the
	 * supplied list of graph nodes and linked by the supplied list of graph edges.
	 * @param nodes list of nodes for the evidence types.
	 * @param edges list of edge linking the evidence types.
	 * @return the graph containing all episodes.
	 */
	public Graph<EpisodeGraphNode, EpisodeGraphEdge> generateGraph(
			List<EpisodeGraphNode> nodes, List<EpisodeGraphEdge> edges)
	{	
		// Build the graph.
		SparseGraph<EpisodeGraphNode, EpisodeGraphEdge> graph =  
			new SparseGraph<EpisodeGraphNode, EpisodeGraphEdge>();
		
		HashMap<Integer, EpisodeGraphNode> graphNodes = new HashMap<Integer, EpisodeGraphNode>();
		for (EpisodeGraphNode node : nodes)
		{
			graph.addVertex(node);
			graphNodes.put(node.getId(), node);
		}
		
		for (EpisodeGraphEdge edge : edges)
		{
			graph.addEdge(edge, 
					graphNodes.get(edge.getFirstId()), graphNodes.get(edge.getSecondId()));
		}

		logger.debug("Generated graph: " + graph);
		logger.debug("Generated graph, vertex count: " + graph.getVertexCount() + ", edge count: " + graph.getEdgeCount());
		
		return graph;
	}
	
	
	/**
	 * Lays out the episodes contained within the supplied graph.
	 * @param episodesGraph graph of episodes to lay out.
	 * @return list of layouts, with each network of connected nodes
	 * contained within a separate layout.
	 */
	public List<Layout<EpisodeGraphNode, EpisodeGraphEdge>> layoutEpisodes(
			Graph<EpisodeGraphNode, EpisodeGraphEdge> episodesGraph)
	{	
		// Get the individual connected graphs.
		MinimumSpanningForest2<EpisodeGraphNode, EpisodeGraphEdge>prim = 
        	new MinimumSpanningForest2<EpisodeGraphNode, EpisodeGraphEdge>(episodesGraph,
        		new DelegateForest<EpisodeGraphNode, EpisodeGraphEdge>(), DelegateTree.<EpisodeGraphNode, EpisodeGraphEdge>getFactory(),
        		new ConstantTransformer(1.0));
        
        Forest<EpisodeGraphNode, EpisodeGraphEdge> forest = prim.getForest();
        
        Collection<Tree<EpisodeGraphNode, EpisodeGraphEdge>> trees = forest.getTrees();
        logger.debug("Number of trees: " + trees.size());
        Iterator<Tree<EpisodeGraphNode, EpisodeGraphEdge>> treeIter = trees.iterator();
        Tree<EpisodeGraphNode, EpisodeGraphEdge> tree;
        
        ArrayList<Layout<EpisodeGraphNode, EpisodeGraphEdge>> graphLayouts = 
        	new ArrayList<Layout<EpisodeGraphNode, EpisodeGraphEdge>>();
        
        HashMap<Integer, EpisodeGraphNode> graphNodes = new HashMap<Integer, EpisodeGraphNode>();
		for (EpisodeGraphNode node : episodesGraph.getVertices())
		{
			graphNodes.put(node.getId(), node);
		}
        
        while (treeIter.hasNext())
        {
        	SparseGraph<EpisodeGraphNode, EpisodeGraphEdge> subGraph = 
    			new SparseGraph<EpisodeGraphNode, EpisodeGraphEdge>();
        	
        	tree = treeIter.next();
        	
        	for (EpisodeGraphNode node : tree.getVertices())
    		{
        		subGraph.addVertex(node);
        		Collection<EpisodeGraphEdge> incidentEdges = episodesGraph.getIncidentEdges(node);
        		for (EpisodeGraphEdge edge : incidentEdges)
        		{
        			subGraph.addEdge(edge, 
        					graphNodes.get(edge.getFirstId()),
        					graphNodes.get(edge.getSecondId()));
        		}
    		}
    		
    		Layout<EpisodeGraphNode, EpisodeGraphEdge> layout = layoutEpisode(subGraph);
    		graphLayouts.add(layout);
    		
        }
        
        return graphLayouts;
	}
	
	
	/**
	 * Lays out the episode represented by the given graph.
	 * @param graph graph for the episode.
	 * @return layout data for the episode.
	 */
	public Layout<EpisodeGraphNode, EpisodeGraphEdge> layoutEpisode(Graph<EpisodeGraphNode, EpisodeGraphEdge> graph)
	{
        // Lay out the nodes using a TreeLayout.
        Layout<EpisodeGraphNode, EpisodeGraphEdge> layout;
        
        float numNodes = graph.getVertexCount();
		if (numNodes > 2)
		{
			// Use a force-directed layout.
			FRLayout<EpisodeGraphNode, EpisodeGraphEdge> frLayout = 
				new FRLayout<EpisodeGraphNode, EpisodeGraphEdge>(graph);
			
			// Set initial layout width/height according to number of nodes to
			// give adequate spacing between the nodes.
			float scalingFactor = (numNodes*1.5F);
			frLayout.setSize(new Dimension((int)(scalingFactor * MODEL_BOX_WIDTH), (int)(scalingFactor * MODEL_BOX_HEIGHT)));
		
			for (int i = 0; i < 50; i++)
			{
				frLayout.step();
			}
			
			layout = frLayout;
		}
		else
		{
			// Lay out the nodes using a TreeLayout.
			MinimumSpanningForest2<EpisodeGraphNode, EpisodeGraphEdge>prim = 
	        	new MinimumSpanningForest2<EpisodeGraphNode, EpisodeGraphEdge>(graph,
	        		new DelegateForest<EpisodeGraphNode, EpisodeGraphEdge>(), 
	        		DelegateTree.<EpisodeGraphNode, EpisodeGraphEdge>getFactory(),
	        		new ConstantTransformer(1.0));
	        Forest<EpisodeGraphNode, EpisodeGraphEdge> forest = prim.getForest();
			layout = new TreeLayout<EpisodeGraphNode, EpisodeGraphEdge>(forest, MODEL_BOX_WIDTH, MODEL_BOX_HEIGHT+100);
		}
        
        return layout;
	}
	
	
	/**
	 * Generates an SVG image of the episodes represented by the supplied list 
	 * of graph layouts.
	 * @param graphLayouts list of episode graph layouts.
	 */
	public void generateSVGImage(List<Layout<EpisodeGraphNode, EpisodeGraphEdge>> graphLayouts,
			String fileName)
	{
		DOMImplementation impl =
		    GenericDOMImplementation.getDOMImplementation();
		String svgNS = "http://www.w3.org/2000/svg";
		Document myFactory = impl.createDocument(svgNS, "svg", null);

		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(myFactory);
        try
        {
        	Collections.sort(graphLayouts, new EpisodeGraphSizeComparator());
        	
			SVGGraphics2D g2d = new SVGGraphics2D(ctx, false);
			
			int xOffset = 0;
			for (Layout<EpisodeGraphNode, EpisodeGraphEdge> layout : graphLayouts)
			{
				Point minExtent = getMinExtent(layout);
				xOffset -= minExtent.getX();
				
				// Draw the connections first.
				Graph<EpisodeGraphNode, EpisodeGraphEdge> graph = layout.getGraph();
				Pair<EpisodeGraphNode> endPoints;
				Point2D startPoint;
				Point2D endPoint;
				
				for (EpisodeGraphEdge edge : graph.getEdges())
		        {
					endPoints = graph.getEndpoints(edge);
					//logger.debug("generateSVGImage() endPoints:" + endPoints);
					startPoint = layout.transform(endPoints.getFirst());
					endPoint = layout.transform(endPoints.getSecond());
					//logger.debug("generateSVGImage() startPoint:" + startPoint + " - " + endPoint + " for edge id " + edge.getId());
					
					
					g2d.setPaint(OUTLINE_COLOR);	
					g2d.setStroke(new BasicStroke(1.0f, 
				            BasicStroke.CAP_BUTT, 
				            BasicStroke.JOIN_MITER));
		        	g2d.drawLine((int)startPoint.getX() + xOffset + MODEL_BOX_WIDTH/2, (int)startPoint.getY()+MODEL_BOX_HEIGHT/2, 
		        			(int)endPoint.getX()+ xOffset + MODEL_BOX_WIDTH/2, (int)endPoint.getY()+MODEL_BOX_HEIGHT/2);
		        	
		        	// Draw the arrow head(s) to indicate directionality, and add labels.
		        	drawArrowHead(g2d, startPoint, endPoint, xOffset, edge.isDirectional());
		        	drawEdgeText(g2d, startPoint, endPoint, edge, xOffset);
		        }
				
				
				// Draw the nodes.
				Point2D vertexPoint;
				int vertexX;
				int vertexY;
				for (EpisodeGraphNode vertex : graph.getVertices())
		        {
					vertexPoint = layout.transform(vertex);
					
					vertexX = (int)vertexPoint.getX();
					vertexY = (int)vertexPoint.getY();

					g2d.setPaint(FILL_COLOR);
			        g2d.fillRect(vertexX + xOffset, vertexY, MODEL_BOX_WIDTH, MODEL_BOX_HEIGHT);
			        
			        g2d.setPaint(OUTLINE_COLOR);
					g2d.setStroke(OUTLINE_STROKE);
			        g2d.drawRect(vertexX+ xOffset, vertexY,MODEL_BOX_WIDTH, MODEL_BOX_HEIGHT);
			        //logger.debug("generateSVGImage() " + vertex + " rect starts : (" + 
			        //		(vertexX + xOffset) + "," + vertexY + ")");
			        
			        
			        // Draw the description text.
			        AttributedString desc = new AttributedString(vertex.getDescription());
			        FontRenderContext frc = g2d.getFontRenderContext();
			        LineBreakMeasurer measurer = new LineBreakMeasurer(desc.getIterator(), frc);
			        float wrappingWidth = MODEL_BOX_WIDTH - 10;
			        Point pen = new Point(vertexX + xOffset + 5, vertexY);
			        g2d.setPaint(OUTLINE_COLOR);
			        while (measurer.getPosition() < vertex.getDescription().length()) 
			        {
			            TextLayout textLayout = measurer.nextLayout(wrappingWidth);
			            pen.y += (textLayout.getAscent());
			            
			            textLayout.draw(g2d, pen.x, pen.y);
			            pen.y += textLayout.getDescent() + textLayout.getLeading();
			        }

		        }
				
				Point maxExtent = getMaxExtent(layout);
				xOffset += maxExtent.getX() + MODEL_BOX_WIDTH + 25;
			
			}
	        
			
			// Write the image out to a PNG file.
	        ByteArrayOutputStream baos = new ByteArrayOutputStream( 8192 );
	        Writer svgOutput = new OutputStreamWriter( baos, "UTF-8" );
	        g2d.stream( svgOutput, true );
	        
	        PNGTranscoder t = new PNGTranscoder();
	    	t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(xOffset+ 25));
	    	t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(getMaxHeight(graphLayouts) + MODEL_BOX_HEIGHT + 5));

	        TranscoderInput input =
	            new TranscoderInput( new StringReader(baos.toString()) );
	        
	        OutputStream ostream = new FileOutputStream(fileName + ".png");
	        TranscoderOutput output = new TranscoderOutput(ostream);

            t.transcode(input, output);

            ostream.flush();
            ostream.close();
	        
	   


        }
        catch (Exception e)
        {
	        logger.debug("Error writing layout to file: " + e);
        }
		
	}
	
	
	/**
	 * Draws the arrow head for the line connecting the two points supplied.
	 * @param g2d graphics context.
	 * @param startPoint starting point of the line at which to draw an arrow head.
	 * @param endPoint end point of the line at which to draw an arrow head.
	 * @param xOffset current offset on x-axis for which to offset the arrowhead in
	 * 			relation to the supplied coordinates.
	 * @param directional true if an arrowhead should only be added at the end point
	 * 		to indicate a uni-directional connection.
	 */
	private void drawArrowHead(Graphics2D g2d, Point2D startPoint, Point2D endPoint, 
			int xOffset, boolean directional)
	{
		double startX;
		double startY;
		double endX;
		double endY;
		
		boolean addAtStart = !(directional);
		boolean addAtEnd = true;
		
		// In order to simplify drawing the arrow, ensure the start point x-coord 
		// is to the left of the end point.
		if (startPoint.getX() < endPoint.getX())
		{
			startX = startPoint.getX();
			startY = startPoint.getY();
			
			endX = endPoint.getX();
			endY = endPoint.getY();
		}
		else if (startPoint.getX() > endPoint.getX())
		{
			startX = endPoint.getX();
			startY = endPoint.getY();
			
			endX = startPoint.getX();
			endY = startPoint.getY();
			
			addAtStart = true;
			addAtEnd = !(directional);
		}
		else
		{
			// For equal x-coords, place the start point below the end point.
			if (startPoint.getY() > endPoint.getY())
			{
				startX = startPoint.getX();
				startY = startPoint.getY();
				
				endX = endPoint.getX();
				endY = endPoint.getY();
			}
			else
			{
				startX = endPoint.getX();
				startY = endPoint.getY();
				
				endX = startPoint.getX();
				endY = startPoint.getY();
			}
		}
		

		/*
		g2d.setColor(Color.red);
		g2d.drawLine(
				(int)startX + (MODEL_BOX_WIDTH/2) + xOffset, (int)startY + (MODEL_BOX_HEIGHT/2), 
				(int)endX + (MODEL_BOX_WIDTH/2) + xOffset, (int)endY + (MODEL_BOX_HEIGHT/2));
		*/
		
		/*
		logger.debug("Line: (" + ((int)startX + (MODEL_BOX_WIDTH/2) + xOffset) + "," + 
				((int)startY + (MODEL_BOX_HEIGHT/2)) + ") to (" + 
				((int)endX + (MODEL_BOX_WIDTH/2) + xOffset) + "," + 
				((int)endY + (MODEL_BOX_HEIGHT/2)) + ")");
		*/
		
		/*
		BasicStroke thickStroke = new BasicStroke(4.0f, 
	            BasicStroke.CAP_BUTT, 
	            BasicStroke.JOIN_MITER);
		g2d.setStroke(thickStroke);
		g2d.drawRect((int)(startX + xOffset),
				(int)(startY),
				MODEL_BOX_WIDTH,
				MODEL_BOX_HEIGHT);
		*/
		
		// Calculate the angle of the connecting line so that the arrowhead
		// can be rotated accordingly.
		double lineAngle =  Math.atan((endY - startY)/(endX - startX));
		
		
		// Draw the arrowhead for the end point of the edge.
		Point2D[] clippingPoints = ClippingRectangle.getClipped((int)startX + (MODEL_BOX_WIDTH/2) + xOffset, (int)startY + (MODEL_BOX_HEIGHT/2), 
				(int)endX + (MODEL_BOX_WIDTH/2) + xOffset, (int)endY + (MODEL_BOX_HEIGHT/2), 
				(int)(endX + xOffset), (int)(endX + xOffset + MODEL_BOX_WIDTH), 
				(int)(endY), (int)(endY + MODEL_BOX_HEIGHT));
		
		
		//logger.debug("intersects:" + clippingPoints);
		
		if (clippingPoints != null && addAtEnd == true)
        {
        	//logger.debug("intersects 1:" + clippingPoints[1]);
			
			double secondAngle = lineAngle;
        	
        	int arrowPointX = (int)clippingPoints[1].getX();
        	int arrowPointY = (int)clippingPoints[1].getY();
        	
        	Polygon arrowHead = new Polygon();
        	arrowHead.addPoint(arrowPointX, arrowPointY);
    		arrowHead.addPoint(arrowPointX-15, arrowPointY-6);
    		arrowHead.addPoint(arrowPointX-15, arrowPointY+6);
    		
    		GeneralPath arrowPath = new GeneralPath(arrowHead);
    		arrowPath.transform(AffineTransform.getRotateInstance(
    				secondAngle, clippingPoints[1].getX(), clippingPoints[1].getY()));
    		
    		g2d.setColor(OUTLINE_COLOR);	
    		g2d.fill(arrowPath);
        	
        }	
		
		if (clippingPoints !=null && addAtStart == true)
		{
			clippingPoints = ClippingRectangle.getClipped((int)startX + (MODEL_BOX_WIDTH/2) + xOffset, (int)startY + (MODEL_BOX_HEIGHT/2), 
					(int)endX + (MODEL_BOX_WIDTH/2) + xOffset, (int)endY + (MODEL_BOX_HEIGHT/2), 
					(int)(startX + xOffset), (int)(startX + xOffset + MODEL_BOX_WIDTH), 
					(int)(startY), (int)(startY + MODEL_BOX_HEIGHT));
			
			
			if (clippingPoints != null)
	        {
	        	//logger.debug("intersects 1:" + clippingPoints[1]);
				
				double firstAngle = lineAngle;
	        	
	        	int arrowPointX = (int)clippingPoints[1].getX();
	        	int arrowPointY = (int)clippingPoints[1].getY();
	        	
	        	Polygon arrowHead = new Polygon();
	    		arrowHead.addPoint(arrowPointX, arrowPointY);
	    		arrowHead.addPoint(arrowPointX+15, arrowPointY-6);
	    		arrowHead.addPoint(arrowPointX+15, arrowPointY+6);
	    		
	    		GeneralPath arrowPath = new GeneralPath(arrowHead);
	    		arrowPath.transform(AffineTransform.getRotateInstance(
	    				firstAngle, clippingPoints[1].getX(), clippingPoints[1].getY()));
	    		
	    		g2d.setColor(OUTLINE_COLOR);	
	    		g2d.fill(arrowPath);
	        	
	        }
		
		}

	}
	
	
	/**
	 * Labels the graph edge connecting the two supplied points with information
	 * such as the probability and attributes.
	 * @param g2d graphics context.
	 * @param startPoint starting point of the connection to label.
	 * @param endPoint end point of the connection to label.
	 * @param edge the graph edge that is being labelled.
	 * @param xOffset current offset on x-axis for which to offset the label in
	 * 			relation to the supplied coordinates.
	 */
	private void drawEdgeText(Graphics2D g2d, Point2D startPoint, Point2D endPoint, 
			EpisodeGraphEdge edge, int xOffset)
	{

		int midPointX = (int)(startPoint.getX() + endPoint.getX())/2;
		int midPointY = (int)(startPoint.getY() + endPoint.getY())/2;
		
		g2d.setColor(Color.black);
		g2d.drawString((int)(edge.getProbability()*100) + "%", midPointX+ xOffset + (MODEL_BOX_WIDTH/2)-10,
				midPointY + (MODEL_BOX_HEIGHT/2)-10);
		g2d.drawString(edge.getAttributes(), midPointX+ xOffset + (MODEL_BOX_WIDTH/2)-20,
				midPointY + (MODEL_BOX_HEIGHT/2)+10);
	}
	
	
	private int getMaxHeight(List<Layout<EpisodeGraphNode, EpisodeGraphEdge>> graphLayouts)
	{
		int maxHeight = 0;
		for (Layout<EpisodeGraphNode, EpisodeGraphEdge> layout : graphLayouts)
		{
			maxHeight = Math.max(maxHeight, getMaxExtent(layout).y);
		}
		
		return maxHeight;
	}
	
	
	private Point getMaxExtent(Layout<EpisodeGraphNode, EpisodeGraphEdge> layout)
	{
		int maxX = 0;
		int maxY = 0;
		
		Graph<EpisodeGraphNode, EpisodeGraphEdge> graph = layout.getGraph();
		Point2D vertexPoint;
		for (EpisodeGraphNode vertex : graph.getVertices())
        {
			vertexPoint = layout.transform(vertex);
			
			maxX = Math.max(maxX, (int)vertexPoint.getX());
			maxY = Math.max(maxY, (int)vertexPoint.getY());
        }
		
		return new Point(maxX, maxY);
	}
	
	
	private Point getMinExtent(Layout<EpisodeGraphNode, EpisodeGraphEdge> layout)
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		
		Graph<EpisodeGraphNode, EpisodeGraphEdge> graph = layout.getGraph();
		Point2D vertexPoint;
		for (EpisodeGraphNode vertex : graph.getVertices())
        {
			vertexPoint = layout.transform(vertex);
			
			minX = Math.min(minX, (int)vertexPoint.getX());
			minY = Math.min(minY, (int)vertexPoint.getY());
        }
		
		return new Point(minX, minY);
	}
	
	
	/**
	 * Generates a small graph for testing with a list of hard-coded nodes and edges.
	 */
	private Graph<EpisodeGraphNode, EpisodeGraphEdge> getTestEpisodesGraph()
	{
		ArrayList<EpisodeGraphNode> graphNodes = new ArrayList<EpisodeGraphNode>();
		graphNodes.add(new EpisodeGraphNode(1, "1", 1));
		graphNodes.add(new EpisodeGraphNode(2, "2", 1));
		graphNodes.add(new EpisodeGraphNode(3, "3", 1));
		graphNodes.add(new EpisodeGraphNode(4, "4", 1));
		
		ArrayList<EpisodeGraphEdge> graphEdges = new ArrayList<EpisodeGraphEdge>();
		graphEdges.add(new EpisodeGraphEdge(101, 1, 2, 0.6F, "source"));
		graphEdges.add(new EpisodeGraphEdge(102, 2, 3, 0.5F, "service"));
		graphEdges.add(new EpisodeGraphEdge(103, 2, 4, 0.8F, "source, service"));
		graphEdges.add(new EpisodeGraphEdge(104, 3, 4, 0.9F, ""));
		
		return generateGraph(graphNodes, graphEdges);
	}
	
	
	/**
	 * Comparator class to compare two graph layouts based 
	 * on their number of vertices.
	 */
	class EpisodeGraphSizeComparator implements Comparator<Layout>
	{
        public int compare(Layout layout1, Layout layout2)
        {
	        return layout2.getGraph().getVertexCount() - layout1.getGraph().getVertexCount();
        }
	}
	
	
	public static void main(String[] args)
	{
		// main() method for standalone testing.
		
		// Configure the log4j logging properties.
		PropertyConfigurator.configure("config/log4j.properties");
		
		EpisodeGraphBuilder graphBuilder = new EpisodeGraphBuilder();
		
		Date startDate = new Date();
		
		// Get a graph containing all the episodes from the configured DB.
		Graph<EpisodeGraphNode, EpisodeGraphEdge> episodesGraph = graphBuilder.getAllEpisodesGraph();   
		
		// Get a graph containing a small network of test data.
		//Graph<EpisodeGraphNode, EpisodeGraphEdge> episodesGraph = graphBuilder.getTestEpisodesGraph();
		
		// Lay out each episode into a separate layout.
        List<Layout<EpisodeGraphNode, EpisodeGraphEdge>> graphLayouts = 
        	graphBuilder.layoutEpisodes(episodesGraph);
        
        // Generate the SVG image:
        
        // Write all layouts to a single file.
        //graphBuilder.generateSVGImage(graphLayouts, "layout");
        
        // Write each layout to a separate file.
        int num = 0;
    	for (Layout<EpisodeGraphNode, EpisodeGraphEdge> layout : graphLayouts)
    	{
    		++num;
    		List<Layout<EpisodeGraphNode, EpisodeGraphEdge>> oneLayout =
    				new ArrayList<Layout<EpisodeGraphNode, EpisodeGraphEdge>>();
    		oneLayout.add(layout);
    		graphBuilder.generateSVGImage(oneLayout, "layout" + num);
    	}

        
        
        Date endDate = new Date();
		
        logger.debug("Finished creating PNG in " + (endDate.getTime() - startDate.getTime()) + "ms");
	}
}
