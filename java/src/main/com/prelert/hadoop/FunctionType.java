/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.hadoop;

/**
 * Description copied from FunctionTypes.h
 *
 * An enumeration of possible functions we can run on a data stream
 * on which we do anomaly detection. These map to a set of data
 * features which we'll model (see function_t::features for details).
 * The possible functions are:
  <dl>
  <dt>Individual count</dt><dd> for which we look at all bucket counts and
	 is intended to detect both rare messages and large deviations
	 in the rate of messages w.r.t. the history of a single category
	 of categorical data.</dd>
  <dt>Individual non-zero count</dt><dd> for which we look at the non-zero bucket
	 counts and is intended to detect large deviations in the rate of
	 messages w.r.t. the history of a single category of categorical
	 data and is well suited to sparse data.</dd>
  <dt>Individual rare count</dt><dd> for which we look at all bucket counts and
	  calculate the relative rarity (in time) of each category and is
	  intended to detect both rare messages and large deviations in the
	  rate of messages w.r.t. the history of categorical data.</dd>
  <dt>Individual rare non-zero count</dt><dd> for which we look at the non-zero
	  bucket counts and calculate the relative rarity (in time) of each
	  category and is intended to detect rare messages and large
	  deviations in the rate of messages w.r.t. the history sparse
	  categorical data.</dd>
  <dt>Individual metric</dt><dd> for which we look at the minimum, mean and
	 maximum values in a bucket for a single metric time series over
	 time and is our default analysis for metric data.</dd>
  <dt>Individual metric mean</dt><dd> for which we look at the mean
	 value in a bucket for a single metric time series over time.</dd>
  <dt>Individual metric min</dt><dd> for which we look at the minimum
	 value in a bucket for a single metric time series over time.</dd>
  <dt>Individual metric max</dt><dd> for which we look at the maximum
	 value in a bucket for a single metric time series over time.</dd>
  <dt>Population count</dt><dd> for which we look at the number of messages each
	 person generates in a bucket partitioned by category. This is used
	 for analyzing categorical data as a population.</dd>
  <dt>Population distinct count</dt><dd> for which we look at the number of
	 different categories a person in a population generates in a bucket.
	 This is used for analyzing categorical data as a population.</dd>
  <dt>Population rare</dt><dd> for which we analyze the number of people hitting
	 each category over all time. This is used for analyzing categorical
	 data as a population.</dd>
  <dt>Population rare count</dt><dd> for which we analyze the number of people
	 hitting each category over all time and also the number of messages
	 each person generates in a bucket partitioned by category. This is
	 used for analyzing categorical data as a population.</dd>
  <dt>Population frequent rare</dt><dd> for which we analyze the number of people
	 hitting each category over all time and weight the person's categories
	 according to their relative frequency when computing their overall
	 probability. This is used for analyzing categorical data as a
	 population.</dd>
  <dt>Population rare count</dt><dd> for which we analyze the number of people
	 hitting each category over all time and also the number of messages
	 each person generates in a bucket partitioned by category and weight
	 the person's categories according to their relative frequency when
	 computing their overall probability. This is used for analyzing
	 categorical data as a population.</dd>
	</dl>
 *
 *
 */
public enum FunctionType
{
	None,
	PopulationCount,
	PopulationDistinctCount,
	PopulationRare,
	PopulationRareCount,
	PopulationFreqRare,
	PopulationFreqRareCount,
	IndividualCount
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}
	},
	IndividualRareCount
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}
	},
	IndividualRareNonZeroCount
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}
	},
	IndividualRare
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}
	},
	IndividualNonZeroCount
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}
	},
	IndividualMetric
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}

		@Override
		boolean isMetric()
		{
			return true;
		}
	},
	IndividualMetricMean
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}

		@Override
		boolean isMetric()
		{
			return true;
		}
	},
	IndividualMetricMin
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}

		@Override
		boolean isMetric()
		{
			return true;
		}
	},
	IndividualMetricMax
	{
		@Override
		boolean isIndividual()
		{
			return true;
		}

		@Override
		boolean isPopulation()
		{
			return false;
		}

		@Override
		boolean isMetric()
		{
			return true;
		}
	};

	boolean isIndividual()
	{
		return false;
	}

	boolean isPopulation()
	{
		return true;
	}

	boolean isMetric()
	{
		return false;
	}


	// Individual event rate functions
	public static final String FUNCTION_COUNT = "count";
	public static final String FUNCTION_COUNT_ABBREV = "c";
	public static final String FUNCTION_NON_ZERO_COUNT = "non_zero_count";
	public static final String FUNCTION_NON_ZERO_COUNT_ABBREV = "nzc";
	public static final String FUNCTION_RARE = "rare";
	// No abbreviation for "rare" as "r" is a little too obscure

	// Metric functions
	public static final String FUNCTION_AVERAGE = "avg";
	public static final String FUNCTION_MEAN = "mean";
	public static final String FUNCTION_MIN = "min";
	public static final String FUNCTION_MAX = "max";

	// Population functions
	public static final String FUNCTION_POPULATION_COUNT = "population_count";
	public static final String FUNCTION_POPULATION_COUNT_ABBREV = "pc";
	public static final String FUNCTION_POPULATION_DISTINCT_COUNT = "population_distinct_count";
	public static final String FUNCTION_POPULATION_DISTINCT_COUNT_ABBREV = "pdc";
	public static final String FUNCTION_POPULATION_RARE = "population_rare";
	public static final String FUNCTION_POPULATION_RARE_ABBREV = "pr";
	public static final String FUNCTION_POPULATION_RARE_COUNT = "population_rare_count";
	public static final String FUNCTION_POPULATION_RARE_COUNT_ABBREV = "prc";
	public static final String FUNCTION_POPULATION_FREQ_RARE = "population_freq_rare";
	public static final String FUNCTION_POPULATION_FREQ_RARE_ABBREV = "pfr";
	public static final String FUNCTION_POPULATION_FREQ_RARE_COUNT = "population_freq_rare_count";
	public static final String FUNCTION_POPULATION_FREQ_RARE_COUNT_ABBREV = "pfrc";


	/**
	 * Return the function type enum for the string (or abbreviation)
	 * or <code>None</code> if the functionName is not recognised.
	 *
	 * @param functionName the full name or an abbreviation.
	 * @return The function type for the string or <code>None</code>
	 * if the string is not recognised.
	 */
	static public FunctionType fromString(String functionName)
	{
		if (functionName.equals(FUNCTION_COUNT) ||
				functionName.equals(FUNCTION_COUNT_ABBREV))
		{
			return FunctionType.IndividualRareCount;
		}
		else if (functionName.equals(FUNCTION_NON_ZERO_COUNT) ||
				functionName.equals(FUNCTION_NON_ZERO_COUNT_ABBREV))
		{
			return FunctionType.IndividualRareNonZeroCount;
		}
		else if (functionName.equals(FUNCTION_RARE))
		{
			return FunctionType.IndividualRare;
		}
		else if (functionName.equals(FUNCTION_AVERAGE) ||
				functionName.equals(FUNCTION_MEAN))
		{
			return FunctionType.IndividualMetricMean;
		}
		else if (functionName.equals(FUNCTION_MIN))
		{
			return FunctionType.IndividualMetricMin;
		}
		else if (functionName.equals(FUNCTION_MAX))
		{
			return FunctionType.IndividualMetricMax;
		}
		else if (functionName.equals(FUNCTION_POPULATION_COUNT) ||
				functionName.equals(FUNCTION_POPULATION_COUNT_ABBREV))
		{
			return FunctionType.PopulationCount;
		}
		else if (functionName.equals(FUNCTION_POPULATION_DISTINCT_COUNT) ||
				functionName.equals(FUNCTION_POPULATION_DISTINCT_COUNT_ABBREV))
		{
			return FunctionType.PopulationDistinctCount;
		}
		else if (functionName.equals(FUNCTION_POPULATION_RARE) ||
				functionName.equals(FUNCTION_POPULATION_RARE_ABBREV))
		{
			return FunctionType.PopulationRare;
		}
		else if (functionName.equals(FUNCTION_POPULATION_RARE_COUNT) ||
				functionName.equals(FUNCTION_POPULATION_RARE_COUNT_ABBREV))
		{
			return FunctionType.PopulationRareCount;
		}
		else if (functionName.equals(FUNCTION_POPULATION_FREQ_RARE) ||
				functionName.equals(FUNCTION_POPULATION_FREQ_RARE_ABBREV))
		{
			return FunctionType.PopulationFreqRare;
		}
		else if (functionName.equals(FUNCTION_POPULATION_FREQ_RARE_COUNT) ||
				functionName.equals(FUNCTION_POPULATION_FREQ_RARE_COUNT_ABBREV))
		{
			return FunctionType.PopulationFreqRareCount;
		}

		return FunctionType.None;
	}
}
