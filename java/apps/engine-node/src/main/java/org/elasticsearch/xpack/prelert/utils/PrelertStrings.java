/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.utils;

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
