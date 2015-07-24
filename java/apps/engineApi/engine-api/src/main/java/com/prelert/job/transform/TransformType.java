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

import com.google.common.collect.Range;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.exceptions.TransformConfigurationException;

/**
 * Enum type representing the different transform functions
 * with functions for converting between the enum and its
 * pretty name i.e. human readable string.
 */
public enum TransformType
{
    // Name, arity, arguments, outputs, default output names, has condition, argument validator
    DOMAIN_SPLIT(Names.DOMAIN_SPLIT_NAME, Range.singleton(1), Range.singleton(0),
            Range.closed(1, 2), Arrays.asList("subDomain", "hrd")),
    CONCAT(Names.CONCAT_NAME, Range.atLeast(2), Range.closed(0, 1), Range.singleton(1),
            Arrays.asList("concat")),
    REGEX_EXTRACT(Names.EXTRACT_NAME, Range.singleton(1), Range.singleton(1), Range.atLeast(1),
            Arrays.asList("extract"), false, new RegexExtractVerifier()),
    REGEX_SPLIT(Names.SPLIT_NAME, Range.singleton(1), Range.singleton(1), Range.atLeast(1),
            Arrays.asList("split"), false, new RegexPatternVerifier()),
    EXCLUDE(Names.EXCLUDE_NAME, Range.atLeast(1), Range.singleton(0), Range.singleton(0),
            Arrays.asList(), true),
    LOWERCASE(Names.LOWERCASE_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("lowercase")),
    UPPERCASE(Names.UPPERCASE_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("uppercase")),
    TRIM(Names.TRIM_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("trim"));

    /**
     * Transform names.
     *
     * Enums cannot use static fields in their constructors as the
     * enum values are initialised before the statics.
     * Having the static fields in nested class means they are created
     * when required.
     */
    public class Names
    {
        public static final String DOMAIN_SPLIT_NAME = "domain_split";
        public static final String CONCAT_NAME = "concat";
        public static final String EXTRACT_NAME = "extract";
        public static final String SPLIT_NAME = "split";
        public static final String EXCLUDE_NAME = "exclude";
        public static final String LOWERCASE_NAME = "lowercase";
        public static final String UPPERCASE_NAME = "uppercase";
        public static final String TRIM_NAME = "trim";

        private Names()
        {
        }
    }

    private final Range<Integer> m_ArityRange;
    private final Range<Integer> m_ArgumentsRange;
    private final Range<Integer> m_OutputsRange;
    private final String m_PrettyName;
    private final List<String> m_DefaultOutputNames;
    private final boolean m_HasCondition;
    private final ArgumentVerifier m_ArgumentVerifier;

    private TransformType(String prettyName, Range<Integer> arityRange,
            Range<Integer> argumentsRange, Range<Integer> outputsRange,
            List<String> defaultOutputNames)
    {
        this(prettyName, arityRange, argumentsRange, outputsRange, defaultOutputNames, false);
    }

    private TransformType(String prettyName, Range<Integer> arityRange,
            Range<Integer> argumentsRange, Range<Integer> outputsRange,
            List<String> defaultOutputNames, boolean hasCondition)
    {
        this(prettyName, arityRange, argumentsRange, outputsRange, defaultOutputNames, hasCondition,
                (arg, transform) -> {});
    }

    private TransformType(String prettyName, Range<Integer> arityRange,
            Range<Integer> argumentsRange, Range<Integer> outputsRange,
            List<String> defaultOutputNames, boolean hasCondition,
            ArgumentVerifier argumentVerifier)
    {
        m_ArityRange = arityRange;
        m_ArgumentsRange = argumentsRange;
        m_OutputsRange = outputsRange;
        m_PrettyName = prettyName;
        m_DefaultOutputNames = defaultOutputNames;
        m_HasCondition = hasCondition;
        m_ArgumentVerifier = argumentVerifier;
    }

    /**
     * The count range of inputs the transform expects.
     * @return
     */
    public Range<Integer> arityRange()
    {
        return m_ArityRange;
    }

    /**
     * The count range of arguments the transform expects.
     * @return
     */
    public Range<Integer> argumentsRange()
    {
        return m_ArgumentsRange;
    }

    /**
     * The count range of outputs the transform expects.
     * @return
     */
    public Range<Integer> outputsRange()
    {
        return m_OutputsRange;
    }

    public String prettyName()
    {
        return m_PrettyName;
    }

    public List<String> defaultOutputNames()
    {
        return m_DefaultOutputNames;
    }

    public boolean hasCondition()
    {
        return m_HasCondition;
    }

    public boolean verify(TransformConfig tc) throws TransformConfigurationException
    {
        checkInputs(tc);
        checkArguments(tc);
        checkOutputs(tc);
        return true;
    }

    private void checkInputs(TransformConfig tc) throws TransformConfigurationException
    {
        List<String> inputs = tc.getInputs();
        checkValidInputCount(tc, inputs);
        checkInputsAreNonEmptyStrings(tc, inputs);
    }

    private void checkValidInputCount(TransformConfig tc, List<String> inputs)
            throws TransformConfigurationException
    {
        int inputsSize = (inputs == null) ? 0 : inputs.size();
        if (!m_ArityRange.contains(inputsSize))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_INPUT_COUNT,
                    tc.getTransform(), rangeAsString(m_ArityRange), inputsSize);
            throw new TransformConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT);
        }
    }

    private void checkInputsAreNonEmptyStrings(TransformConfig tc, List<String> inputs)
            throws TransformConfigurationException
    {
        if (containsEmptyString(inputs))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_TRANSFORM_INPUTS_CONTAIN_EMPTY_STRING, tc.getTransform());
            throw new TransformConfigurationException(msg,
                    ErrorCodes.TRANSFORM_INPUTS_CANNOT_BE_EMPTY_STRINGS);
        }
    }

    private static boolean containsEmptyString(List<String> strings)
    {
        return strings.stream().anyMatch(s -> s.trim().isEmpty());
    }

    private void checkArguments(TransformConfig tc) throws TransformConfigurationException
    {
        List<String> arguments = tc.getArguments();
        int argumentsSize = (arguments == null) ? 0 : arguments.size();
        if (!m_ArgumentsRange.contains(argumentsSize))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_ARGUMENT_COUNT,
                    tc.getTransform(), rangeAsString(m_ArgumentsRange), argumentsSize);
            throw new TransformConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_ARGUMENT_COUNT);
        }

        if (arguments != null)
        {
            for (String argument : arguments)
            {
                m_ArgumentVerifier.verify(argument, tc);
            }
        }
    }

    private void checkOutputs(TransformConfig tc) throws TransformConfigurationException
    {
        List<String> outputs = tc.getOutputs();
        checkValidOutputCount(tc, outputs);
        checkOutputsAreNonEmptyStrings(tc, outputs);
    }

    private void checkValidOutputCount(TransformConfig tc, List<String> outputs)
            throws TransformConfigurationException
    {
        int outputsSize = (outputs == null) ? 0 : outputs.size();
        if (!m_OutputsRange.contains(outputsSize))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_OUTPUT_COUNT,
                    tc.getTransform(), rangeAsString(m_OutputsRange), outputsSize);
            throw new TransformConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_OUTPUT_COUNT);
        }
    }

    private void checkOutputsAreNonEmptyStrings(TransformConfig tc, List<String> outputs)
            throws TransformConfigurationException
    {
        if (containsEmptyString(outputs))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_TRANSFORM_OUTPUTS_CONTAIN_EMPTY_STRING, tc.getTransform());
            throw new TransformConfigurationException(msg,
                    ErrorCodes.TRANSFORM_OUTPUTS_CANNOT_BE_EMPTY_STRINGS);
        }
    }

    private static String rangeAsString(Range<Integer> range)
    {
        if (range.hasLowerBound() && range.hasUpperBound()
                && range.lowerEndpoint() == range.upperEndpoint())
        {
            return String.valueOf(range.lowerEndpoint());
        }
        return range.toString();
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
                Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_UNKNOWN_TYPE, prettyName),
                ErrorCodes.UNKNOWN_TRANSFORM);
    }

}
