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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.apache.log4j.Logger;

import static com.prelert.data.PropertyNames.COUNT;
import static com.prelert.data.PropertyNames.METRIC;

import com.prelert.data.Attribute;
import com.prelert.data.CausalityData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.Evidence;


/**
 * Class which analyses an activity by building a tree of the constituent data,
 * recursing down through the attributes in order according to the most common
 * shared attribute values.
 * 
 * @author Pete Harverson
 */
public class ActivityAnalysisTree
{
	static Logger s_Logger = Logger.getLogger(ActivityAnalysisTree.class);

	private List<String> m_AttributeNames;
	private ArrayList<Evidence> m_CausalityData;

	private AnalysisTreeNode m_RootNode;


	/**
	 * Creates a new <code>ActivityAnalysisTree</code> to analyse activity data by the
	 * specified list of attribute names.
	 * @param attributeNames  names of the attributes to analyse.
	 */
	public ActivityAnalysisTree(List<String> attributeNames)
	{
		m_AttributeNames = attributeNames;
		m_CausalityData = new ArrayList<Evidence>();

		m_RootNode = new AnalysisTreeNode(m_AttributeNames.size(), "All data", 0);
	}


	/**
	 * Reads the input data for building the tree from the supplied InputStream.
	 * @param stream InputStream from which to read tree data.
	 * @throws IOException if an error occurs reading data from the InputStream.
	 */
	public void readInputFromStream(InputStream stream) throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));

		String inputLine;
		while ((inputLine = in.readLine()) != null)
		{
			addInputLine(inputLine);
		}

		in.close();

		s_Logger.debug("Number of lines in input: " + getInputSize());
	}


	/**
	 * Adds a line of input data for analysis.
	 * @param line activity data, consisting of the attribute values separated by
	 *            <code>'|'</code> delimiters.
	 */
	public void addInputLine(String line)
	{
		// Each line of input split into tokens separated by '|'.
		String[] tokens = line.split("\\|", -1);
		s_Logger.debug("Num tokens: " + tokens.length);
		s_Logger.debug("m_AttributeNames.size: " + m_AttributeNames.size());

		//if (tokens.length == (m_AttributeNames.size() + 1)) // Current input data, last token is empty.
		if (tokens.length == (m_AttributeNames.size()) )
		{
			// Convert to Evidence object.
			Evidence data = new Evidence();
			for (int i = 0; i < m_AttributeNames.size(); i++)
			{
				data.set(m_AttributeNames.get(i), tokens[i]);
			}

			// Set count to 1 for input read from file
			data.set(COUNT, new Integer(1));

			m_CausalityData.add(data);
		}
		else
		{
			s_Logger.debug("Invalid input - wrong number of tokens: " + line);
		}
	}
	
	
	/**
	 * Adds an item of causality data for analysis.
	 * @param causalityData <code>CausalityData</code> to be added for analysis in the tree.
	 */
	public void addCausalityData(CausalityData causalityData)
	{
		// Currently works with Evidence objects.
		Evidence evidence = new Evidence();
		evidence.setDataType(causalityData.getDataSourceType().getName());
		evidence.setSource(causalityData.getSource());
		evidence.set(COUNT, causalityData.getCount());
		
		// TODO - work with metric/description
		if (causalityData.getDataSourceType().getDataCategory().equals(DataSourceCategory.TIME_SERIES_FEATURE))
		{
			evidence.set(METRIC, causalityData.getDescription());
		}
		List<Attribute> attributes = causalityData.getAttributes();
		if (attributes != null)
		{
			for (Attribute attribute : attributes)
			{
				evidence.set(attribute.getAttributeName(), attribute.getAttributeValue());
			}
		}
		
		m_CausalityData.add(evidence);
	}


	/**
	 * Builds and dumps the tree to debug.
	 * @param forcedFields  list of field numbers, which may be empty, 
	 *           to force to the top of the analysis.
	 */
	public void dumpTree(List<Integer> forcedFields)
	{
		buildTree(forcedFields);

		s_Logger.debug("Tree: " + m_RootNode.toStringWithChildren(0));
	}
	
	
	/**
	 * Builds and dumps the tree to debug, optionally 'forcing' a particular 
	 * attribute to the front of the analysis.
	 * @param forceField index of field to 'force' to the top of the analysis,
	 * 	or <code>-1</code> to use the natural ordering based on count.
	 * 	The forced field will appear above other fields with higher counts. 
	 * 	Fields with counts equal to the size of the input data will be placed 
	 * 	above the forced field.
	 */
	public void dumpTree(int forceField)
	{
		buildTree(forceField);

		s_Logger.debug("Tree: " + m_RootNode.toStringWithChildren(0));
	}


	/**
	 * Returns the root node of the activity analysis tree.
	 * @return the root node.
	 */
	public AnalysisTreeNode getRoot()
	{
		return m_RootNode;
	}


	/**
	 * Builds the tree of activity data.
	 * @param forcedFields list of field numbers, which may be empty, 
	 *            to force to the top of the analysis.
	 */
	public void buildTree(List<Integer> forcedFields)
	{
		m_RootNode.setCount(getInputCount());
		
		int forcedFieldCount = 1;
		for (int fieldNum : forcedFields)
		{
			s_Logger.debug("Field " + fieldNum + " forced to level " + 1);
			forcedFieldCount++;
		}

		int numFields = m_AttributeNames.size();
		ArrayList<Integer> toDoFieldNums = new ArrayList<Integer>();
		for (int i = 0; i < numFields; i++)
		{
			toDoFieldNums.add(i);
		}

		ArrayList<Integer> doneFieldNums = new ArrayList<Integer>(
		        toDoFieldNums.size());

		// Get the most common field name at each level of the tree.
		String attrName;
		Object attrValObj;
		String attributeValue;
		Integer count;
		int highestCount = 0;
		int mostCommonFieldNum = 0;
		int mostCommonFieldNumIndex = 0;
		HashMap<String, Integer> countsMap = new HashMap<String, Integer>();
		Iterator<Integer> forcedFieldsIter = forcedFields.iterator();
		HashMap<List<String>, List<Evidence>> groupsSoFar;

		while (toDoFieldNums.isEmpty() == false)
		{
			groupsSoFar = getGroupsOfData(doneFieldNums);

			List<Integer> toDoFieldNumsCopy = new ArrayList<Integer>();
			toDoFieldNumsCopy.addAll(toDoFieldNums);
			highestCount = 0;

			Collection<List<Evidence>> groups = groupsSoFar.values();
			Iterator<List<Evidence>> groupsIter = groups.iterator();
			while (groupsIter.hasNext())
			{
				List<Evidence> evidenceList = groupsIter.next();

				if (forcedFieldsIter.hasNext() == true)
				{
					// In this case, the "most common" field is forced, so it's
					// not necessarily really the most common.
					mostCommonFieldNum = forcedFieldsIter.next();
					countsMap.clear();

					for (Evidence data : evidenceList)
					{
						attrName = m_AttributeNames.get(mostCommonFieldNum);
						attrValObj = data.get(attrName);
						attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.

						count = countsMap.get(attributeValue);
						if (count == null)
						{
							count = 0;
						}

						count += (Integer)data.get(COUNT);
						countsMap.put(attributeValue, count);

						if (count > highestCount)
						{
							highestCount = count;
						}
					}
				}
				else
				{
					for (int fieldNum : toDoFieldNumsCopy)
					{
						countsMap.clear();

						for (Evidence data : evidenceList)
						{
							attrName = m_AttributeNames.get(fieldNum);
							attrValObj = data.get(attrName);
							attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.
							count = countsMap.get(attributeValue);
							if (count == null)
							{
								count = 0;
							}

							count += (Integer)data.get(COUNT);
							countsMap.put(attributeValue, count);
						}

						// Find the highest count for this attribute name.
						Iterator<String> countsIter = countsMap.keySet().iterator();
						String attrVal;
						while (countsIter.hasNext())
						{
							attrVal = countsIter.next();
							if (attrVal.length() > 0)	// Only consider blank values worth ranking.
							{
								int num = countsMap.get(attrVal);
								if (num > highestCount)
								{
									highestCount = num;
									mostCommonFieldNum = fieldNum;
								}
							}
						}

					}
				}
			}

			// If we're not forcing fields, and the highest count is 1 then
			// there's no commonality from now on. Stop building the tree, unless
			// we are still on a single branch.
			if (highestCount <= 1 && groupsSoFar.size() > 1)
			{
				for (int skippedFieldNum : toDoFieldNums)
				{
					s_Logger.debug("Field " + skippedFieldNum + 
							" has highest count 1 and is not included in the tree");
				}
				break;
			}

			mostCommonFieldNumIndex = toDoFieldNumsCopy.indexOf(mostCommonFieldNum);
			toDoFieldNums.remove(mostCommonFieldNumIndex);
			doneFieldNums.add(mostCommonFieldNum);

			s_Logger.debug("At level " + doneFieldNums.size() + " mostCommonFieldNum " + 
					mostCommonFieldNum + " highestCount " + highestCount);

			// Build the next level of the tree.
			AnalysisTreeNode currentNode;
			AnalysisTreeNode childNode;
			for (Evidence data : m_CausalityData)
			{
				// Trace through the tree from the root to the correct place
				currentNode = m_RootNode;

				for (int i = 0; i < doneFieldNums.size(); i++)
				{
					int fieldNum = doneFieldNums.get(i);
					count = (Integer)data.get(COUNT);
					attrName = m_AttributeNames.get(fieldNum);
					attrValObj = data.get(attrName);
					attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.
					if (i == doneFieldNums.size() - 1)
					{
						// We're at the level where the new nodes go.
						childNode = currentNode.getChildByValue(attributeValue);
						if (childNode != null)
						{
							childNode.incrementCount(count);
						}
						else
						{
							AnalysisTreeNode newNode = new AnalysisTreeNode(
							        fieldNum, attributeValue, count);
							currentNode.add(newNode);
						}

					}
					else
					{
						// We're at a higher level, and should definitely find a
						// node
						currentNode = currentNode.getChildByValue(attributeValue);
					}
				}
			}
		}
		
		m_RootNode.sort();
	}
	
	
	/**
	 * Builds the tree of activity data, optionally 'forcing' a particular 
	 * attribute to the front of the analysis.
	 * @param forceFieldIdx index of field to 'force' to the top of the analysis,
	 * 	or <code>-1</code> to use the natural ordering based on count.
	 * 	The forced field will appear above other fields with higher counts. 
	 * 	Fields with counts equal to the size of the input data will be placed 
	 * 	above the forced field.
	 */
	public void buildTree(int forceFieldIdx)
	{
		int inputSize = getInputCount();
		m_RootNode.setCount(inputSize);
		
		if (forceFieldIdx > -1)
		{
			s_Logger.debug(m_AttributeNames.get(forceFieldIdx) + " forced to top");
		}

		int numFields = m_AttributeNames.size();
		ArrayList<Integer> toDoFieldNums = new ArrayList<Integer>();
		for (int i = 0; i < numFields; i++)
		{
			toDoFieldNums.add(i);
		}

		ArrayList<Integer> doneFieldNums = new ArrayList<Integer>(
		        toDoFieldNums.size());

		// Get the most common field name at each level of the tree,
		// checking for the forced field ahead of higher count fields.
		String attrName;
		Object attrValObj;
		String attributeValue;
		Integer count;
		int highestCount = 0;
		int mostCommonFieldNum = -2;	// Use -2 as forceFieldIdx is -1 for 'no forced field'.
		int mostCommonFieldNumIndex = 0;
		HashMap<String, Integer> countsMap = new HashMap<String, Integer>();
		HashMap<List<String>, List<Evidence>> groupsSoFar;

		while (toDoFieldNums.isEmpty() == false)
		{
			groupsSoFar = getGroupsOfData(doneFieldNums);

			List<Integer> toDoFieldNumsCopy = new ArrayList<Integer>();
			toDoFieldNumsCopy.addAll(toDoFieldNums);
			highestCount = 0;
			mostCommonFieldNum = -2;	// Use -2 as forceFieldIdx is -1 for 'no forced field'.

			Collection<List<Evidence>> groups = groupsSoFar.values();
			Iterator<List<Evidence>> groupsIter = groups.iterator();
			while (groupsIter.hasNext())
			{
				List<Evidence> evidenceList = groupsIter.next();

				for (int fieldNum : toDoFieldNumsCopy)
				{
					countsMap.clear();

					for (Evidence data : evidenceList)
					{
						attrName = m_AttributeNames.get(fieldNum);
						attrValObj = data.get(attrName);
						attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.
						count = countsMap.get(attributeValue);
						if (count == null)
						{
							count = 0;
						}

						count += (Integer)data.get(COUNT);
						countsMap.put(attributeValue, count);
					}

					// Find the highest count for this attribute name.
					Iterator<String> countsIter = countsMap.keySet().iterator();
					String attrVal;
					while (countsIter.hasNext())
					{
						attrVal = countsIter.next();
						if (attrVal.length() > 0)	// Only consider blank values worth ranking.
						{
							int num = countsMap.get(attrVal);
							if (fieldNum == forceFieldIdx)
							{
								// Put this to the top if the current highest count
								// is less than the size of the input list.
								if (highestCount < inputSize)
								{
									if (num > highestCount)
									{
										highestCount = num;
									}
									mostCommonFieldNum = fieldNum;
								}
							}
							else
							{
								// Put this to the top if it is the first field
								// in this loop to have count=input size, or if
								// it's the highest count so far and the current
								// top node is not the forced field.
								if ( (highestCount < inputSize && num == inputSize) ||
										(highestCount < inputSize && num > highestCount && mostCommonFieldNum != forceFieldIdx) )
								{
									highestCount = num;
									mostCommonFieldNum = fieldNum;
								}
								
							}
						}
					}

				}
			}

			// If we're not forcing fields, and the highest count is 1 then
			// there's no commonality from now on. Stop building the tree, unless
			// we are still on a single branch.
			if (highestCount <= 1 && groupsSoFar.size() > 1)
			{
				for (int skippedFieldNum : toDoFieldNums)
				{
					s_Logger.debug("Field " + skippedFieldNum + 
							" has highest count 1 and is not included in the tree");
				}
				break;
			}

			mostCommonFieldNumIndex = toDoFieldNumsCopy.indexOf(mostCommonFieldNum);
			toDoFieldNums.remove(mostCommonFieldNumIndex);
			doneFieldNums.add(mostCommonFieldNum);

			s_Logger.debug("At level " + doneFieldNums.size() + " top field is " + 
					m_AttributeNames.get(mostCommonFieldNum) + ", highestCount " + highestCount);

			// Build the next level of the tree.
			AnalysisTreeNode currentNode;
			AnalysisTreeNode childNode;
			for (Evidence data : m_CausalityData)
			{
				// Trace through the tree from the root to the correct place
				currentNode = m_RootNode;

				for (int i = 0; i < doneFieldNums.size(); i++)
				{
					int fieldNum = doneFieldNums.get(i);
					count = (Integer)data.get(COUNT);
					attrName = m_AttributeNames.get(fieldNum);
					attrValObj = data.get(attrName);
					attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.
					if (i == doneFieldNums.size() - 1)
					{
						// We're at the level where the new nodes go.
						childNode = currentNode.getChildByValue(attributeValue);
						if (childNode != null)
						{
							childNode.incrementCount(count);
						}
						else
						{
							AnalysisTreeNode newNode = new AnalysisTreeNode(
							        fieldNum, attributeValue, count);
							currentNode.add(newNode);
						}

					}
					else
					{
						// We're at a higher level, and should definitely find a
						// node
						currentNode = currentNode.getChildByValue(attributeValue);
					}
				}
			}
		}
		
		m_RootNode.sort();
	}


	/**
	 * Groups the activity data by the specified list of field numbers.
	 * @param doneFieldNums field numbers which have been analysed.
	 * @return map of activity data.
	 */
	public HashMap<List<String>, List<Evidence>> getGroupsOfData(
	        List<Integer> doneFieldNums)
	{
		HashMap<List<String>, List<Evidence>> groups = new HashMap<List<String>, List<Evidence>>();

		List<String> key;
		List<Evidence> evidenceList;

		for (Evidence data : m_CausalityData)
		{
			key = getFieldsSubset(doneFieldNums, data);
			evidenceList = groups.get(key);
			if (evidenceList == null)
			{
				evidenceList = new ArrayList<Evidence>();
				groups.put(key, evidenceList);
			}
			evidenceList.add(data);
		}

		return groups;
	}


	/**
	 * Returns the list of attribute values from an item of evidence for the
	 * attributes with the specified field numbers.
	 * @param fieldNums numbers of the fields whose values to return.
	 * @param data item of <code>Evidence</code> data.
	 * @return list of attribute values.
	 */
	protected List<String> getFieldsSubset(List<Integer> fieldNums,
	        Evidence data)
	{
		ArrayList<String> fieldVals = new ArrayList<String>();

		for (int fieldNum : fieldNums)
		{
			fieldVals.add((String) data.get(m_AttributeNames.get(fieldNum)));
		}

		return fieldVals;
	}


	/**
	 * Returns the number of lines of activity data which were inputted for
	 * analysis.
	 * @return number of lines of data.
	 */
	public int getInputSize()
	{
		return m_CausalityData.size();
	}


	/**
	 * Returns the total count of all activity data.
	 * @return Total count for all input.
	 */
	public int getInputCount()
	{
		int totalCount = 0;

		for (Evidence data : m_CausalityData)
		{
			totalCount += (Integer)data.get(COUNT);
		}

		return totalCount;
	}
	
	
	/**
	 * Builds the tree of activity data using attributes in the order of the list 
	 * that was supplied in the constructor. 
	 * @param startAtField index of field at which to start the analysis.
	 */
	public void buildFixedTree()
	{
		m_RootNode.setCount(getInputCount());

		int numFields = m_AttributeNames.size();
		ArrayList<Integer> toDoFieldNums = new ArrayList<Integer>();
		for (int i = 0; i < numFields; i++)
		{
			toDoFieldNums.add(i);
		}
		
		ArrayList<Integer> doneFieldNums = new ArrayList<Integer>(toDoFieldNums.size());

		// Look for the level at which the highest count drops to 0.
		String attrName;
		Object attrValObj;
		String attributeValue;
		Integer count;
		int highestCount = 0;
		HashMap<String, Integer> countsMap = new HashMap<String, Integer>();
		HashMap<List<String>, List<Evidence>> groupsSoFar;

		for (int fieldNumber = 0; fieldNumber < numFields; fieldNumber++)
		{
			groupsSoFar = getGroupsOfData(doneFieldNums);
			
			highestCount = 0;
			Collection<List<Evidence>> groups = groupsSoFar.values();
			Iterator<List<Evidence>> groupsIter = groups.iterator();
			while (groupsIter.hasNext())
			{
				List<Evidence> evidenceList = groupsIter.next();
				countsMap.clear();

				for (Evidence data : evidenceList)
				{
					attrName = m_AttributeNames.get(fieldNumber);
					attrValObj = data.get(attrName);
					attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.
					
					count = countsMap.get(attributeValue);
					if (count == null)
					{
						count = 0;
					}

					count += (Integer)data.get(COUNT);
					countsMap.put(attributeValue, count);
				}

				// Find the highest count for this attribute name.
				Iterator<String> countsIter = countsMap.keySet().iterator();
				String attrVal;
				while (countsIter.hasNext())
				{
					attrVal = countsIter.next();
					int num = countsMap.get(attrVal);
					if (num > highestCount)
					{
						highestCount = num;
					}
				}
			}

			// If we're not forcing fields, and the highest count is 1 then
			// there's no commonality from now on. Stop building the tree, unless
			// we are still on a single branch.
			if (highestCount <= 1 && groupsSoFar.size() > 1)
			{
				for (int skippedFieldNum : toDoFieldNums)
				{
					s_Logger.debug(m_AttributeNames.get(skippedFieldNum) + "(" + skippedFieldNum + 
							") has highest count 1 and is not included in the tree");
				}
				break;
			}

			toDoFieldNums.remove(0);
			doneFieldNums.add(fieldNumber);

			s_Logger.debug("At level " + doneFieldNums.size() + " top field is (" + 
					fieldNumber + ") " + m_AttributeNames.get(fieldNumber) + ", highestCount " + highestCount);

			// Build the next level of the tree.
			AnalysisTreeNode currentNode;
			AnalysisTreeNode childNode;
			for (Evidence data : m_CausalityData)
			{
				// Trace through the tree from the root to the correct place
				currentNode = m_RootNode;

				for (int fieldNum = 0; fieldNum < doneFieldNums.size(); fieldNum++)
				{
					count = (Integer)data.get(COUNT);
					attrName = m_AttributeNames.get(fieldNum);
					attrValObj = data.get(attrName);
					attributeValue = (attrValObj != null ? attrValObj.toString() : "");	// Need to count null/empty attribute values.
					if (fieldNum == doneFieldNums.size() - 1)
					{
						// We're at the level where the new nodes go.
						childNode = currentNode.getChildByValue(attributeValue);
						if (childNode != null)
						{
							childNode.incrementCount(count);
						}
						else
						{
							AnalysisTreeNode newNode = new AnalysisTreeNode(
							        fieldNum, attributeValue, count);
							currentNode.add(newNode);
						}

					}
					else
					{
						// We're at a higher level, and should definitely find a
						// node
						currentNode = currentNode.getChildByValue(attributeValue);
					}
				}
			}
		}
		
		m_RootNode.sort();
	}
	
	
	/**
	 * Prunes the tree, so that only nodes up to and including the first branch
	 * point are added.
	 * @param node top node at hierarchy currently being pruned.
	 * @param addChildren <code>true</code> to add the children of this node.
	 * @return root node of pruned tree.
	 */
	public AnalysisTreeNode pruneToBranch(AnalysisTreeNode node, boolean addChildren)
	{
		AnalysisTreeNode nodeCopy = new AnalysisTreeNode(
				node.getFieldNumber(), node.getAttributeValue(), node.getCount());
		nodeCopy.setHasMoreLevels(node.getChildCount() > 0);
		
		if (addChildren == true)
		{
			int childCount = node.getChildCount();
			boolean addGrandChildren = (childCount == 1);
			AnalysisTreeNode childNode;
			for (int i = 0; i < childCount; i++)
			{
				childNode = (AnalysisTreeNode)(node.getChildAt(i));
				nodeCopy.add(pruneToBranch(childNode, addGrandChildren));
			}
		}
		
		return nodeCopy;
	}
	

	/**
	 * Extension of the Swing DefaultMutableTree to represent a node in the
	 * activity analysis tree.
	 */
	public class AnalysisTreeNode extends DefaultMutableTreeNode
	{
		private static final long serialVersionUID = -5173050428031603196L;

		private int m_FieldNumber;
		private String m_FieldName;
		private String m_Value;
		private int m_Count;
		private boolean m_HasMoreLevels;


		/**
		 * Creates a new <code>AnalysisTreeNode</code>.
		 * @param fieldNumber number of the field for the level in the tree for this node.
		 * @param value attribute value for this node.
		 * @param count number of occurrences for which the combination of
		 * 		attributes at this level of the tree has occurred.
		 */
		public AnalysisTreeNode(int fieldNumber, String value, int count)
		{
			m_FieldNumber = fieldNumber;
			m_Value = value;
			m_Count = count;
			if (fieldNumber < m_AttributeNames.size())
			{
				m_FieldName = m_AttributeNames.get(m_FieldNumber);
			}
		}	


		/**
		 * Returns the number of the field for this level in the tree.
		 * @return the field number.
		 */
		public int getFieldNumber()
		{
			return m_FieldNumber;
		}

		
		/**
		 * Sets the number of occurrences for which the combination of
		 * attributes at this level of the tree has occurred.
		 * @param count the count.
		 */
		public void setCount(int count)
		{
			m_Count = count;
		}
		

		/**
		 * Returns the number of occurrences for which the combination of
		 * attributes at this level of the tree has occurred.
		 * @return the count.
		 */
		public int getCount()
		{
			return m_Count;
		}
		
		
		/**
		 * Returns the attribute name for this node.
		 * @return the attribute name.
		 */
		public String getAttributeName()
		{
			return m_FieldName;
		}


		/**
		 * Returns the value of the attribute represented by this level in the tree.
		 * @return the attribute value.
		 */
		public String getAttributeValue()
		{
			return m_Value;
		}
		
		
		/**
		 * Returns the list of metric path attribute values represented by this tree node,
		 * starting from the root node.
		 * @return list of attribute values, which may include <code>null</code> values,
		 * 	where the attribute for the root node is the first entry in the returned list.
		 */
		public List<String> getPathAttributeValues()
		{
			ArrayList<String> attributeValues = new ArrayList<String>();
			
			TreeNode[] path = getPath();
			AnalysisTreeNode treeNode;
			String value;
			for (TreeNode node : path)
			{
				treeNode = (AnalysisTreeNode)node;
				if (treeNode.isRoot() == false)
				{
					value = treeNode.getAttributeValue();
					if (value.length() > 0)
					{
						attributeValues.add(value);
					}
					else
					{
						attributeValues.add(null);
					}
				}
			}
			
			
			return attributeValues;
		}
		
		
		/**
		 * Sets the value of the attribute represented by this level in the tree.
		 * @param value the attribute value.
		 */
		public void setValue(String value)
		{
			m_Value = value;
		}


		/**
		 * Returns true if this node has a child node with the given attribute
		 * value.
		 * @param value attribute value.
		 * @return <code>true</code> if the node does have a child with this
		 *         value, <code>false</code> if not.
		 */
		public boolean hasChildWithValue(String value)
		{
			AnalysisTreeNode child;
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++)
			{
				child = (AnalysisTreeNode) (getChildAt(i));
				if (child.getAttributeValue().equals(value) == true)
				{
					return true;
				}
			}

			return false;
		}


		/**
		 * Returns the child node which has the specified attribute value.
		 * @param value attribute value.
		 * @return the child node, or <code>null</code> if there is no node with
		 *         the specified value.
		 */
		public AnalysisTreeNode getChildByValue(String value)
		{
			int childCount = getChildCount();
			AnalysisTreeNode child = null;
			for (int i = 0; i < childCount; i++)
			{
				child = (AnalysisTreeNode) (getChildAt(i));
				if (child.getAttributeValue().equals(value) == true)
				{
					return child;
				}
			}

			return null;
		}


		/**
		 * Increments the occurrence count of this attribute value.
		 */
		public void incrementCount(int amount)
		{
			m_Count += amount;
		}


		/**
		 * Sorts the children of the node in descending order by occurrence
		 * count, then field number, and finally alphabetically by value.
		 */
		public void sort()
		{
			int childCount = getChildCount();
			List<AnalysisTreeNode> children = new ArrayList<AnalysisTreeNode>(
			        childCount);
			AnalysisTreeNode child;
			for (int i = 0; i < childCount; i++)
			{
				child = (AnalysisTreeNode) (getChildAt(i));
				children.add(child);
			}

			Collections.sort(children, new Comparator<AnalysisTreeNode>()
			{

				@Override
				public int compare(AnalysisTreeNode node1,
				        AnalysisTreeNode node2)
				{
					int comp = 0;

					// Sort order is count then fieldNum then value
					comp = node2.getCount() - node1.getCount();
					if (comp == 0)
					{
						comp = node2.getFieldNumber() - node1.getFieldNumber();

						if (comp == 0)
						{
							comp = node2.getAttributeValue().compareTo(node1.getAttributeValue());
						}
					}

					return comp;
				}

			});

			removeAllChildren();
			for (AnalysisTreeNode childNode : children)
			{
				add(childNode);
			}

			// Tell children to sort their children.
			for (int i = 0; i < childCount; i++)
			{
				child = (AnalysisTreeNode) (getChildAt(i));
				child.sort();
			}
		}
		
		
		/**
		 * Returns whether there are more levels in the tree below this node. After
		 * 'pruning' a tree, a node may be a leaf even though it does not represent
		 * the lowest possible level in the analysis tree.
		 * @return <code>true</code> if there are more levels in the tree below this node, 
		 * 	<code>false</code> if not.
		 */
		public boolean hasMoreLevels()
		{
			boolean moreLevels = (!isLeaf());
			if (moreLevels == false)
			{
				moreLevels = m_HasMoreLevels;
			}
			
			return moreLevels;
		}
        
        
		/**
		 * Sets whether there are more levels in the tree below this node. After
		 * 'pruning' a tree, a node may be a leaf even though it does not represent
		 * the lowest possible level in the analysis tree.
		 * @param moreLevels <code>true</code> if there are more levels in the tree  
		 * 	below this node, <code>false</code> if not.
		 */
        public void setHasMoreLevels(boolean moreLevels)
        {
        	m_HasMoreLevels = moreLevels;
        }


		/**
		 * Returns a String representation of this node and all its children.
		 * @param level  the level of the tree, used for indenting output.
		 * @return String representation of the branch headed by this node.
		 */
		public String toStringWithChildren(int level)
		{
			StringBuilder strRep = new StringBuilder();
			for (int i = 0; i < level; i++)
			{
				strRep.append(' ');
			}

			strRep.append("Node: { ");
			strRep.append(m_FieldNumber).append(", ");
			strRep.append(m_Value).append(", ");
			strRep.append(m_Count);
			strRep.append(" } Children: [\n");

			int childCount = getChildCount();
			AnalysisTreeNode child = null;
			for (int i = 0; i < childCount; i++)
			{
				child = (AnalysisTreeNode) (getChildAt(i));
				strRep.append(child.toStringWithChildren(level + 1));
			}

			for (int i = 0; i < level; i++)
			{
				strRep.append(' ');
			}

			strRep.append("]\n");

			return strRep.toString();
		}


		@Override
		public String toString()
		{
			return "{" + m_FieldName + "=" + m_Value + "} count=" + m_Count;
		}

	}


	public static void main(String[] args)
	{
		s_Logger.debug("AnalysisTree starting run");
		String causalityInputFile = "../dev/activity/ids_input.txt";
		s_Logger.debug("Reading input from file:" + causalityInputFile);

		// Create the list of the names of the attributes
		// whose values are contained in the input data.
		ArrayList<String> attributeNames = new ArrayList<String>();
		attributeNames.add("Type");
		attributeNames.add("Metric");
		attributeNames.add("Source");
		attributeNames.add("Domain");
		attributeNames.add("Process");
		attributeNames.add("AgentName");
		attributeNames.add("RecordType");
		attributeNames.add("Resource0");
		attributeNames.add("Resource1");
		attributeNames.add("Resource2");
		attributeNames.add("Resource3");
		attributeNames.add("Resource4");
		attributeNames.add("Resource5");

		// Check for forced fields supplied on the command line.
		ArrayList<Integer> forcedFields = new ArrayList<Integer>();
		int forcedFieldIdx = -1;
		if (args.length > 0)
		{
			int index = -1;
			for (String attributeName : args)
			{
				index = attributeNames.indexOf(attributeName);
				if (index > -1)
				{
					forcedFields.add(index);
					forcedFieldIdx = index;
				}
			}
		}

		ActivityAnalysisTree analysisTree = new ActivityAnalysisTree(attributeNames);

		// Read in the input from a file.
		InputStream stream = analysisTree.getClass().getResourceAsStream(causalityInputFile);
		try
		{
			analysisTree.readInputFromStream(stream);
		}
		catch (IOException e)
		{
			s_Logger.error("Error reading input from file: " + causalityInputFile, e);
		}

		s_Logger.debug("Number of lines in input: " + analysisTree.getInputSize());
		
		analysisTree.dumpTree(forcedFieldIdx);

		s_Logger.debug("AnalysisTree run complete");
	}

}
