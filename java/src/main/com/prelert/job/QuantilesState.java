/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.prelert.rs.data.parsing.AutoDetectParseException;

/**
 * This stores the serialised quantiles from autodetect. The serialised form
 * is a long XML string.  There are two kinds of quantiles, each with its
 * own XML string.
 */
public class QuantilesState
{
	/**
	 * These MUST match the constants used in the C++ code
	 * in lib/model/CAnomalyScore.cc
	 */
	public static final String SYS_CHANGE_QUANTILES_KIND = "sysChange";
	public static final String UNUSUAL_QUANTILES_KIND = "unusual";

	private static final Logger s_Logger = Logger.getLogger(QuantilesState.class);

	private Map<String, String> m_QuantilesKindToState;

	public QuantilesState()
	{
		m_QuantilesKindToState = new HashMap<>();
	}


	/**
	 * Expose the map of quantiles kind -> state
	 * @return Quantiles kind -> state map
	 */
	public Map<String, String> getMap()
	{
		return m_QuantilesKindToState;
	}


	/**
	 * Get the set of all kinds of quantiles
	 * @return The set of kinds of quantiles
	 */
	public Set<String> getQuantilesKinds()
	{
		return m_QuantilesKindToState.keySet();
	}


	/**
	 * Return the serialised quantiles of the specified <code>kind</code>
	 * or <code>null</code> if the <code>kind</code> is not
	 * recognised.
	 *
	 * @param kind
	 * @return <code>null</code> or the serialised state
	 */
	public String getQuantilesState(String kind)
	{
		return m_QuantilesKindToState.get(kind);
	}

	/**
	 * Set the state of the detector where state is the serialised model.
	 *
	 * @param kind
	 * @param state
	 */
	public void setQuantilesState(String kind, String state)
	{
		m_QuantilesKindToState.put(kind, state);
	}
}

