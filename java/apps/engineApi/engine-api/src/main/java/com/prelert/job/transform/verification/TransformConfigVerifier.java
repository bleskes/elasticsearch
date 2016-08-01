/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
package com.prelert.job.transform.verification;

import java.util.List;

import com.google.common.collect.Range;
import com.prelert.job.condition.verification.ConditionVerifier;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;

public final class TransformConfigVerifier
{
    private TransformConfigVerifier()
    {
        // Hide default constructor
    }

    /**
     * Checks the transform configuration is valid
     * <ol>
     * <li>Checks there are the correct number of inputs for a given
     * transform type and that those inputs are not empty strings</li>
     * <li>Check the number of arguments is correct for the transform type
     * and verify the argument (i.e. is is a valid regex)</li>
     * <li>Check there is a valid number of ouputs for the transform type
     * and those outputs are not empty strings</li>
     * <li>If the transform has a condition verify it</li>
     * </ol>
     *
     * @param tc
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(TransformConfig tc) throws JobConfigurationException
    {
        TransformType type;
        try
        {
            type = tc.type();
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_UNKNOWN_TYPE, tc.getTransform()),
                    ErrorCodes.UNKNOWN_TRANSFORM);
        }

        checkCondition(tc, type);
        checkInputs(tc, type);
        checkArguments(tc, type);
        checkOutputs(tc, type);

        return true;
    }

    private static void checkCondition(TransformConfig tc, TransformType type)
    throws JobConfigurationException
    {
        if (type.hasCondition())
        {
            if (tc.getCondition() == null)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_CONDITION_REQUIRED,
                                type.prettyName()),
                        ErrorCodes.TRANSFORM_REQUIRES_CONDITION);
            }

            ConditionVerifier.verify(tc.getCondition());
        }
    }

    private static void checkInputs(TransformConfig tc, TransformType type)
    throws JobConfigurationException
    {
        List<String> inputs = tc.getInputs();
        checkValidInputCount(tc, type, inputs);
        checkInputsAreNonEmptyStrings(tc, inputs);
    }

    private static void checkValidInputCount(TransformConfig tc, TransformType type, List<String> inputs)
            throws JobConfigurationException
    {
        int inputsSize = (inputs == null) ? 0 : inputs.size();
        if (!type.arityRange().contains(inputsSize))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_INPUT_COUNT,
                    tc.getTransform(), rangeAsString(type.arityRange()), inputsSize);
            throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT);
        }
    }

    private static void checkInputsAreNonEmptyStrings(TransformConfig tc, List<String> inputs)
            throws JobConfigurationException
    {
        if (containsEmptyString(inputs))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_TRANSFORM_INPUTS_CONTAIN_EMPTY_STRING, tc.getTransform());
            throw new JobConfigurationException(msg,
                    ErrorCodes.TRANSFORM_INPUTS_CANNOT_BE_EMPTY_STRINGS);
        }
    }

    private static boolean containsEmptyString(List<String> strings)
    {
        return strings.stream().anyMatch(s -> s.trim().isEmpty());
    }

    private static void checkArguments(TransformConfig tc, TransformType type) throws JobConfigurationException
    {
        checkArgumentsCountValid(tc, type);
        checkArgumentsValid(tc, type);
    }

    private static void checkArgumentsCountValid(TransformConfig tc, TransformType type) throws JobConfigurationException
    {
        List<String> arguments = tc.getArguments();
        int argumentsSize = (arguments == null) ? 0 : arguments.size();
        if (!type.argumentsRange().contains(argumentsSize))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_ARGUMENT_COUNT,
                    tc.getTransform(), rangeAsString(type.argumentsRange()), argumentsSize);
            throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_ARGUMENT_COUNT);
        }
    }

    private static void checkArgumentsValid(TransformConfig tc, TransformType type) throws JobConfigurationException
    {

        if (tc.getArguments() != null)
        {
            ArgumentVerifier av = argumentVerifierForType(type);

            for (String argument : tc.getArguments())
            {
                av.verify(argument, tc);
            }
        }
    }

    private static ArgumentVerifier argumentVerifierForType(TransformType type)
    {
        switch (type)
        {
            case REGEX_EXTRACT:
                return new RegexExtractVerifier();
            case REGEX_SPLIT:
                return new RegexPatternVerifier();
            default:
                return (argument, config) -> {};
        }
    }


    private static void checkOutputs(TransformConfig tc, TransformType type)
    throws JobConfigurationException
    {
        List<String> outputs = tc.getOutputs();
        checkValidOutputCount(tc, type, outputs);
        checkOutputsAreNonEmptyStrings(tc, outputs);
    }

    private static void checkValidOutputCount(TransformConfig tc, TransformType type,
                                            List<String> outputs)
    throws JobConfigurationException
    {
        int outputsSize = (outputs == null) ? 0 : outputs.size();
        if (!type.outputsRange().contains(outputsSize))
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_OUTPUT_COUNT,
                    tc.getTransform(), rangeAsString(type.outputsRange()), outputsSize);
            throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_OUTPUT_COUNT);
        }
    }

    private static void checkOutputsAreNonEmptyStrings(TransformConfig tc, List<String> outputs)
            throws JobConfigurationException
    {
        if (containsEmptyString(outputs))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_TRANSFORM_OUTPUTS_CONTAIN_EMPTY_STRING, tc.getTransform());
            throw new JobConfigurationException(msg,
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
}
