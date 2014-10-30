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

package com.prelert.proxy.plugin.introscope;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IntroscopeRegExUtilities 
{
	/**
	 * If the given <code>agentRegex</code> can be refined using the 
	 * <code>host</code> string then a regex with the <code>host</code> 
	 * parameter as its first part i.e. host|process|agent is returned.
	 * 
	 * If it can't be matched because the regex contains a literal for 
	 * example if <code>agentRegex</code> = "hostA|processA|agentA" and 
	 * <code>host</code> = "hostB" then <code>null</code> is returned as  
	 * the <code>agentRegex</code> would never be applicable for the 
	 * <code>host</code>.
	 * 
	 * Otherwise the orignal <code>agentRegex</code> is returned.
	 * 
	 * @param agentRegex Agent path regex with its some of its regex special
	 * 			chars escaped. i.e '|' separating path elements becomes "\\|" 
	 * @param host a literal string
	 * @return An new regex with the host of the <code>agentRegex</code> 
	 * 			replaced with <code>host</code> if possible 
	 * OR
	 * 			<code>null</code> if the <code>host</code> string is not 
	 * 			compatible with the <code>agentRegex</code>
	 * OR
	 * 			The original <code>agentRegex</code> if it cannot be 
	 * 			evaluated. 
	 */
	public static String mergeHostIntoAgentRegex(String agentRegex, String host)
	{	
		if (host == null)
		{
			return agentRegex;
		}
		
		StringBuilder builder = new StringBuilder();
		
		String splitRegex = "\\\\\\|";
		String [] agentPathSplit = agentRegex.split(splitRegex);
		
		if (agentPathSplit.length == 3) // nicely divided into host|process|agent
		{
			String hostRegex = agentPathSplit[0];
			if (host.matches(hostRegex))
			{
				// build a new regex with this host and escape the '|' char.
				builder.append(host);
				builder.append("\\|");
				builder.append(agentPathSplit[1]);
				builder.append("\\|");
				builder.append(agentPathSplit[2]);
				
				return builder.toString();
			}
			else // the host does not match the regex.
			{
				return null;
			}
		}
		
		return agentRegex;
	}
	
	
	
	/**
	 * This method tries to bunch externalKeys together into larger more 
	 * general regexs.
	 * 
	 * If 2 keys have the same agent but different metrics it will return;
	 * host|process|agent (metric1)|(metric2)
	 * 
	 * If 2 keys have the same 'host|process' but different agetns and metrics 
	 * it will return:
	 * host|process|(agent1)|(agent2) (metric1)|(metric2)
	 * 
	 * @param externalKeys list of external keys
	 * @return
	 */
	public static List<AgentMetricPair> mergeRelatedExternalKeysIntoRegex(List<String> externalKeys)
	{
		StringBuilder builder = new StringBuilder();
		
		List<AgentMetricPair> result = new ArrayList<AgentMetricPair>();
		List<AgentMetricPair> keyPairs = new ArrayList<AgentMetricPair>();
		
		for (String externalKey : externalKeys)
		{
			try
			{
				keyPairs.add(new AgentMetricPair(externalKey));
			}
			catch (ParseException pe)
			{
				
			}
		}
	

		Map<String, List<String>> groupMetricsByAgent = new HashMap<String, List<String>>();
		
		// group by the agent path
		for (AgentMetricPair pair : keyPairs)
		{
			List<String> metric = groupMetricsByAgent.get(pair.getAgent());
			if (metric == null)
			{
				metric = new ArrayList<String>();
				groupMetricsByAgent.put(pair.getAgent(), metric);
			}
			
			metric.add(pair.getMetric());
		}
		
		List<String> elementsToRemove = new ArrayList<String>();
		
		Set<String> agentKeys = groupMetricsByAgent.keySet();
		for (String agentKey : agentKeys)
		{
			List<String> metrics = groupMetricsByAgent.get(agentKey);
			if (metrics.size() > 1) // group these metrics together
			{
				builder.delete(0, builder.length());
				
				boolean firstMetric = true;
				for (String metric : metrics)
				{
					if (firstMetric)
					{
						builder.append("(");
					}
					else
					{
						builder.append("|(");
					}
					builder.append(metric);
					builder.append(")");
					
					firstMetric = false;
				}
				
				// remove this from the set and add to the results list.
				AgentMetricPair pair = AgentMetricPair.createFromEscapedRegexs(
											agentKey, 
											builder.toString());
				
				result.add(pair);
				elementsToRemove.add(agentKey);		
			}
		}
		
		for (String element : elementsToRemove)
		{
			groupMetricsByAgent.remove(element);	
		}
			
		// Now the set does not share any metrics on the same agent path.
		// look for metrics which share host|process.
		final class AgentAndMetricPath
		{
			final String m_Agent;
			final String m_MetricPath;
			
			AgentAndMetricPath(String agent, String metricPath)
			{
				m_Agent = agent;
				m_MetricPath = metricPath;
			}
		}
		
		
		// merge keys that share the same host|process
		Map<String, List<AgentAndMetricPath>> groupMetricsByHostProcess = new HashMap<String, List<AgentAndMetricPath>>();

		agentKeys = groupMetricsByAgent.keySet();
		for (String agentKey : agentKeys)     
		{
			String [] split = agentKey.split("\\\\\\|");
			String hostProcess = split[0] + "\\|" + split[1];
			
			List<AgentAndMetricPath> agentMetric = groupMetricsByHostProcess.get(hostProcess);
			if (agentMetric == null)
			{
				agentMetric = new ArrayList<AgentAndMetricPath>();
				groupMetricsByHostProcess.put(hostProcess, agentMetric);
			}
			agentMetric.add(new AgentAndMetricPath(split[2], groupMetricsByAgent.get(agentKey).get(0)));
									
		}
		                 
		// Generate regexs for keys sharing the same host|process
		Set<String> hostProcessKeys = groupMetricsByHostProcess.keySet();
		for (String hostProcess : hostProcessKeys)
		{
			List<AgentAndMetricPath> agentMetricPaths = groupMetricsByHostProcess.get(hostProcess);
			if (agentMetricPaths.size() > 1)
			{
				builder.delete(0, builder.length());
				
				// merge metric paths
				boolean firstTime = true;
				for (AgentAndMetricPath agentMetricPath : agentMetricPaths)
				{
					if (firstTime)
					{
						builder.append("(");
					}
					else
					{
						builder.append("|(");
					}
					builder.append(agentMetricPath.m_MetricPath);
					builder.append(")");
					
					firstTime = false;
				}
				
				String metricPath = builder.toString();
				
				builder.delete(0, builder.length());
				
				// merge agents
				firstTime = true;
				for (AgentAndMetricPath agentMetricPath : agentMetricPaths)
				{
					if (firstTime)
					{
						builder.append("(");
					}
					else
					{
						builder.append("|(");
					}
					builder.append(agentMetricPath.m_Agent);
					builder.append(")");
					
					firstTime = false;
				}
				
				String agent = builder.toString();
				
				AgentMetricPair pair = AgentMetricPair.createFromEscapedRegexs(
						hostProcess + "\\|" + agent, 
						metricPath);
				
				// remove this from the set and add to the results list.
				result.add(pair);
				elementsToRemove.add(hostProcess);
			}
		}
		
		
		for (String element : elementsToRemove)
		{
			groupMetricsByHostProcess.remove(element);	
		}
		
		
		// Add remaining ungrouped keys.
		Set<String> remaining = groupMetricsByHostProcess.keySet();
		for (String hostProcess : remaining)
		{
			AgentAndMetricPath ap = groupMetricsByHostProcess.get(hostProcess).get(0);
			
			AgentMetricPair pair = AgentMetricPair.createFromEscapedRegexs(
											hostProcess + "\\|" + ap.m_Agent, 
											ap.m_MetricPath);
			
			result.add(pair);
		}
					
		return result;
	}
}
