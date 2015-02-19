/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
 ************************************************************/

package com.prelert.transforms;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.job.TransformConfig;
import com.prelert.job.TransformConfigurationException;
import com.prelert.job.TransformType;


public class TransformFactory
{
	public static Transform create(TransformConfig transformConfig,
			Map<String, Integer> inputMap,
			Map<String, Integer> outputIndicies,
			Logger logger)
	throws TransformConfigurationException
	{
		int input[] = new int [transformConfig.getInputs().size()];
		int output[] = new int [transformConfig.getOutputs().size()];

		fillIndexArray(transformConfig.getInputs(), inputMap, input, logger);
		fillIndexArray(transformConfig.getOutputs(), outputIndicies, output, logger);

		TransformType type = transformConfig.type();

		switch (type)
		{
			case DOMAIN_LOOKUP:
			{
				return new HighestRegisteredDomain(input, output);

			}
			case CONCAT:
			{
				return new Concat(input, output);
			}
			default:
			{
				// This code will never be hit it's to
				// keep the compiler happy.
				throw new IllegalArgumentException("Uknown transfrom type " + type);
			}
		}
	}

	private static void fillIndexArray(List<String> fields, Map<String, Integer> indicies,
										int [] indexArray, Logger logger)
	{
		int i = 0;
		for (String field : fields)
		{
			Integer index = indicies.get(field);
			if (index != null)
			{
				indexArray[i++] = index;
			}
			else
			{
				logger.error("Field '" + field + "' not indexed");
			}
		}
	}
}
