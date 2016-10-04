
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
