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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Transform argument validation methods
 */
public class ArgumentValidators
{
    private ArgumentValidators()
    {

    }

    /**
     * Validate a regular expression argument
     * If arg complies with {@linkplain Pattern#compile(String)} then
     * it is valid
     *
     * @param arg
     * @return Return true if arg is a valid regular expression else false.
     */
    public static boolean regexChecker(String arg)
    {
        try
        {
            Pattern.compile(arg);
            return true;
        }
        catch (PatternSyntaxException e)
        {
            return false;
        }
    }
}
