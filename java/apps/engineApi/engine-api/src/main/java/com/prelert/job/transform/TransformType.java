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

package com.prelert.job.transform;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.prelert.rs.data.ErrorCode;

/**
 * Enum type representing the different transform functions
 * with functions for converting between the enum and its
 * pretty name i.e. human readable string.
 */
public enum TransformType
{
    DOMAIN_LOOKUP(Names.DOMAIN_LOOKUP_NAME, 1, 0, Arrays.asList("subDomain", "hrd")),
    CONCAT(Names.CONCAT, Names.VARIADIC_ARGS, 0, Arrays.asList("concat")),
    REGEX_EXTRACT(Names.REGEX_EXTRACT, 1, 1, Arrays.asList("")),
    REGEX_SPLIT(Names.REGEX_SPLIT, 1, 1, Arrays.asList("")),
    EXCLUDE_FILTER(Names.EXCLUDE_FILTER, 1, 1, Arrays.asList());

    /**
     * Enums cannot use static fields in their constructors as the
     * enum values are initialised before the statics.
     * Having the static fields in nested class means they are created
     * when required.
     */
    public class Names
    {
        public static final String DOMAIN_LOOKUP_NAME = "domain_lookup";
        public static final String CONCAT = "concat";
        public static final String REGEX_EXTRACT = "regex_extract";
        public static final String REGEX_SPLIT = "regex_split";
        public static final String EXCLUDE_FILTER = "exclude_filter";

        private static final int VARIADIC_ARGS = -1;

        private Names()
        {
        }
    }

    private final int m_Arity;
    private final int m_InitArgumentCount;
    private final String m_PrettyName;
    private final List<String> m_DefaultOutputNames;

    private TransformType(String prettyName, int arity, int requiredArgumentCount,
                        List<String> defaultOutputNames)
    {
        m_Arity = arity;
        m_InitArgumentCount = requiredArgumentCount;
        m_PrettyName = prettyName;
        m_DefaultOutputNames = defaultOutputNames;
    }

    /**
     * Arity of -1 means the function is variadic e.g. concat
     * @return
     */
    public int arity()
    {
        return m_Arity;
    }

    /**
     * The number of non-input arguments required by the transform
     * when it is created.
     * e.g. RegexExtract requires 1 argument that is the actual regex
     * @return
     */
    public int initArgumentCount()
    {
        return m_InitArgumentCount;
    }

    public String prettyName()
    {
        return m_PrettyName;
    }

    public List<String> defaultOutputNames()
    {
        return m_DefaultOutputNames;
    }

    public boolean verify(TransformConfig tc) throws TransformConfigurationException
    {
        if (tc.getInputs() == null)
        {
            String msg = "Function arity error no inputs defined";
            throw new TransformConfigurationException(msg, ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT);
        }

        if (m_InitArgumentCount != tc.getArguments().size())
        {
            String msg = String.format("Transform type %s must be defined with %d arguments",
                                            tc.getTransform(), m_InitArgumentCount);
            throw new TransformConfigurationException(msg, ErrorCode.TRANSFORM_MISSING_INITAILISER_ARGUMENT);
        }

        if (m_Arity == Names.VARIADIC_ARGS)
        {
            if (!tc.getInputs().isEmpty())
            {
                return true;
            }
            else
            {
                String msg = "Function arity error expected at least one argument, got 0";
                throw new TransformConfigurationException(msg, ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT);
            }
        }

        if (tc.getInputs().size() != m_Arity)
        {
            String msg = "Function arity error expected " + m_Arity + " arguments, got "
                        + tc.getInputs().size();
            throw new TransformConfigurationException(msg, ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT);
        }

        return true;
    }

    @Override
    public String toString()
    {
        return prettyName();
    }

    /**
     * Get the enum for the given pretty name.
     * The static function valueOf() cannot be overridden so use
     * this method instead when converting from the pretty name
     * to enum.
     *
     * @param prettyName
     * @return
     */
    public static TransformType fromString(String prettyName) throws TransformConfigurationException
    {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all)
        {
            if (type.prettyName().equals(prettyName))
            {
                return type;
            }
        }

        throw new TransformConfigurationException(
                                "Unknown TransformType '" + prettyName + "'",
                                ErrorCode.UNKNOWN_TRANSFORM);
    }

}
