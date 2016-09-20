
package org.elasticsearch.xpack.prelert.job.transform;

import java.util.Objects;

public class IntRange
{
    public enum BoundType
    {
        OPEN, CLOSED
    }

    public static class Bound
    {
        private final int m_Value;
        private final BoundType m_BoundType;

        public Bound(int value, BoundType boundType)
        {
            m_Value = value;
            m_BoundType = Objects.requireNonNull(boundType);
        }
    }

    private static String PLUS_INFINITY = "+\u221E";
    private static String MINUS_INFINITY = "-\u221E";
    private static char LEFT_BRACKET = '(';
    private static char RIGHT_BRACKET = ')';
    private static char LEFT_SQUARE_BRACKET = '[';
    private static char RIGHT_SQUARE_BRACKET = ']';
    private static char BOUNDS_SEPARATOR = '\u2025';

    private final Bound m_Lower;
    private final Bound m_Upper;

    private IntRange(Bound lower, Bound upper)
    {
        m_Lower = Objects.requireNonNull(lower);
        m_Upper = Objects.requireNonNull(upper);
    }

    public boolean contains(int value)
    {
        int lowerIncludedValue = m_Lower.m_BoundType == BoundType.CLOSED ? m_Lower.m_Value : m_Lower.m_Value + 1;
        int upperIncludedValue = m_Upper.m_BoundType == BoundType.CLOSED ? m_Upper.m_Value : m_Upper.m_Value - 1;
        return value >= lowerIncludedValue && value <= upperIncludedValue;
    }

    public boolean hasLowerBound()
    {
        return m_Lower.m_Value != Integer.MIN_VALUE;
    }

    public boolean hasUpperBound()
    {
        return m_Upper.m_Value != Integer.MAX_VALUE;
    }

    public int lower()
    {
        return m_Lower.m_Value;
    }

    public int upper()
    {
        return m_Upper.m_Value;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(hasLowerBound() && m_Lower.m_BoundType == BoundType.CLOSED ? LEFT_SQUARE_BRACKET : LEFT_BRACKET);
        builder.append(hasLowerBound() ? m_Lower.m_Value : MINUS_INFINITY);
        builder.append(BOUNDS_SEPARATOR);
        builder.append(hasUpperBound() ? m_Upper.m_Value : PLUS_INFINITY);
        builder.append(hasUpperBound() && m_Upper.m_BoundType == BoundType.CLOSED ? RIGHT_SQUARE_BRACKET : RIGHT_BRACKET);
        return builder.toString();
    }

    public static IntRange singleton(int value)
    {
        return closed(value, value);
    }

    public static IntRange closed(int lower, int upper)
    {
        return new IntRange(closedBound(lower), closedBound(upper));
    }

    public static IntRange open(int lower, int upper)
    {
        return new IntRange(openBound(lower), openBound(upper));
    }

    public static IntRange openClosed(int lower, int upper)
    {
        return new IntRange(openBound(lower), closedBound(upper));
    }

    public static IntRange closedOpen(int lower, int upper)
    {
        return new IntRange(closedBound(lower), openBound(upper));
    }

    public static IntRange atLeast(int lower)
    {
        return closed(lower, Integer.MAX_VALUE);
    }

    private static Bound openBound(int value)
    {
        return new Bound(value, BoundType.OPEN);
    }

    private static Bound closedBound(int value)
    {
        return new Bound(value, BoundType.CLOSED);
    }
}
