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
package com.prelert.job.config.verification;

import com.prelert.job.Detector;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public final class DetectorVerifier
{
    private DetectorVerifier()
    {
    }

    /**
     * Checks the configuration is valid
     * <ol>
     * <li>One of fieldName, byFieldName, overFieldName or function must be set</li>
     * <li>Unless the function is 'count' one of fieldName, byFieldName
     * or overFieldName must be set</li>
     * <li>If byFieldName is set function or fieldName must bet set</li>
     * <li>If overFieldName is set function or fieldName must bet set</li>
     * <li>function is one of the strings in the set {@link #ANALYSIS_FUNCTIONS}</li>
     * <li>Function cannot be 'metric' (explicitly or implicitly) in jobs that
     * take pre-summarised input</li>
     * <li>If function is not set but the fieldname happens to be the same
     * as one of the function names (e.g.a field called 'count')
     * set function to 'metric'</li>
     * <li>Check the metric/by/over fields are set as required by the different
     * functions</li>
     * <li>Check the metric/by/over fields that cannot be set with certain
     * functions are not set</li>
     * <li>If the function is NON_ZERO_COUNT or NZC
     * then overFieldName must not be set</li>
     * </ol>
     *
     *@param detector The detector configuration
     * @param isSummarised Is this detector in a pre-summarised job?
     * @return true
     * @throws JobConfigurationException
     */
    public static boolean verify(Detector detector, boolean isSummarised)
    throws JobConfigurationException
    {
        boolean emptyField = detector.getFieldName() == null || detector.getFieldName().isEmpty();
        boolean emptyByField = detector.getByFieldName() == null || detector.getByFieldName().isEmpty();
        boolean emptyOverField = detector.getOverFieldName() == null || detector.getOverFieldName().isEmpty();
        boolean emptyFunction = detector.getFunction() == null || detector.getFunction().isEmpty();

        if (emptyField && emptyByField && emptyOverField)
        {
            if (emptyFunction)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_NO_ANALYSIS_FIELD),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

            if (!Detector.COUNT_WITHOUT_FIELD_FUNCTIONS.contains(detector.getFunction()))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_NO_ANALYSIS_FIELD_NOT_COUNT),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }
        }

        if (!emptyFunction && Detector.ANALYSIS_FUNCTIONS.contains(detector.getFunction()) == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UNKNOWN_FUNCTION, detector.getFunction()),
                    ErrorCodes.UNKNOWN_FUNCTION);
        }

        if (isSummarised && (emptyFunction || detector.getFunction().equals(Detector.METRIC)))
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_INCOMPATIBLE_PRESUMMARIZED,
                            Detector.METRIC),
                    ErrorCodes.INVALID_FUNCTION);
        }

        // If function is not set but the fieldname happens
        // to be the same as one of the function names (e.g.
        // a field called 'count' set function to 'metric'
        if (emptyFunction && Detector.ANALYSIS_FUNCTIONS.contains(detector.getFieldName()))
        {
            detector.setFunction(Detector.METRIC);
            emptyFunction = false;
        }


        if (!emptyByField && emptyField && emptyFunction)
        {
            throw new JobConfigurationException(
                            Messages.getMessage(Messages.JOB_CONFIG_BYFIELD_NEEDS_ANOTHER),
                            ErrorCodes.INVALID_FIELD_SELECTION);
        }

        if (!emptyOverField && emptyField && emptyFunction)
        {
            throw new JobConfigurationException(
                            Messages.getMessage(Messages.JOB_CONFIG_OVERFIELD_NEEDS_ANOTHER),
                            ErrorCodes.INVALID_FIELD_SELECTION);
        }

        // check functions have required fields
        if (!emptyFunction)
        {
            if (Detector.FIELD_NAME_FUNCTIONS.contains(detector.getFunction()) && emptyField)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_REQUIRES_FIELDNAME,
                                detector.getFunction()),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

            if (!emptyField && (Detector.FIELD_NAME_FUNCTIONS.contains(detector.getFunction()) == false))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_FIELDNAME_INCOMPATIBLE_FUNCTION,
                                detector.getFunction()),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

            if (Detector.BY_FIELD_NAME_FUNCTIONS.contains(detector.getFunction()) && emptyByField)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_REQUIRES_BYFIELD,
                                detector.getFunction()),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

            if (!emptyByField && Detector.NO_BY_FIELD_NAME_FUNCTIONS.contains(detector.getFunction()))
            {
                throw new JobConfigurationException(
                            Messages.getMessage(Messages.JOB_CONFIG_BYFIELD_INCOMPATIBLE_FUNCTION,
                                detector.getFunction()),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

            if (emptyOverField && Detector.OVER_FIELD_NAME_FUNCTIONS.contains(detector.getFunction()))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_REQUIRES_OVERFIELD,
                                detector.getFunction()),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

            if (!emptyOverField && Detector.NO_OVER_FIELD_NAME_FUNCTIONS.contains(detector.getFunction()))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_OVERFIELD_INCOMPATIBLE_FUNCTION,
                                detector.getFunction()),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }

        }

        // field names cannot contain certain characters
        String [] fields = {detector.getFieldName(), detector.getByFieldName(), detector.getOverFieldName(), detector.getPartitionFieldName()};
        for (String field : fields)
        {
            verifyFieldName(field);
        }

        return true;
    }


    /**
     * Check that the characters used in a field name will not cause problems.
     * @param field The field name to be validated
     * @return true
     * @throws JobConfigurationException
     */
    public static boolean verifyFieldName(String field)
    throws JobConfigurationException
    {
        if (field != null)
        {
            for (Character ch : Detector.PROHIBITED_FIELDNAME_CHARACTERS)
            {
                if (field.indexOf(ch) >= 0)
                {
                    throw new JobConfigurationException(
                            Messages.getMessage(Messages.JOB_CONFIG_INVALID_FIELDNAME_CHARS,
                                    field, Detector.PROHIBITED),
                            ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);
                }
            }
        }
        return true;
    }
}
