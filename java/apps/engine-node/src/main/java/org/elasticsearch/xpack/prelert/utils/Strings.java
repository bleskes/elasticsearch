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

public final class Strings
{
    private static final String EMPTY_STRING = "";

    private Strings()
    {
        // Hide default constructor
    }

    public static boolean isNullOrEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    public static String nullToEmpty(String s)
    {
        return s == null ? EMPTY_STRING : s;
    }
}
