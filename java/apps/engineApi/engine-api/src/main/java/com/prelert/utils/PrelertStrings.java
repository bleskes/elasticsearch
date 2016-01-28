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

package com.prelert.utils;

import java.util.regex.Pattern;

/**
 * Another String utilities class. Class name is prefixed with Prelert to avoid confusion
 * with one of the myriad String utility classes out there.
 */
public final class PrelertStrings
{
    private static final Pattern NEEDS_QUOTING = Pattern.compile("\\W");

    private PrelertStrings()
    {
        // do nothing
    }

    /**
     * Surrounds with double quotes the given {@code input} if it contains
     * any non-word characters. Any double quotes contained in {@code input}
     * will be escaped.
     *
     * @param input any non null string
     * @return {@code input} when it does not contain non-word characters, or a new string
     * that contains {@code input} surrounded by double quotes otherwise
     */
    public static String doubleQuoteIfNotAlphaNumeric(String input)
    {
        if (!NEEDS_QUOTING.matcher(input).find())
        {
            return input;
        }

        StringBuilder quoted = new StringBuilder();
        quoted.append('\"');

        for (int i = 0; i < input.length(); ++i)
        {
            char c = input.charAt(i);
            if (c == '\"' || c == '\\')
            {
                quoted.append('\\');
            }
            quoted.append(c);
        }

        quoted.append('\"');
        return quoted.toString();
    }
}
