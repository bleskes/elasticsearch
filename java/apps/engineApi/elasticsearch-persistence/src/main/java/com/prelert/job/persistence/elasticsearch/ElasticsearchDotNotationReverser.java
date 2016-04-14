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

package com.prelert.job.persistence.elasticsearch;

import java.util.TreeMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.prelert.job.results.ReservedFieldNames;

/**
 * Interprets field names containing dots as nested JSON structures.
 * This matches what Elasticsearch does.
 */
class ElasticsearchDotNotationReverser
{
    private static final char DOT = '.';
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final Map<String, Object> m_ResultsMap;

    public ElasticsearchDotNotationReverser()
    {
        m_ResultsMap = new TreeMap<>();
    }

    // TODO - could handle values of all types Elasticsearch does, e.g. date,
    // long, int, double, etc.  However, at the moment field values in our
    // results are only strings, so it's not "minimum viable product" right
    // now.  Hence this method only takes fieldValue as a String and there are
    // no overloads.
    /**
     * Given a field name and value, convert it to a map representation of the
     * (potentially nested) JSON structure Elasticsearch would use to store it.
     * For example:
     * <code>foo = x</code> goes to <code>{ "foo" : "x" }</code> and
     * <code>foo.bar = y</code> goes to <code>{ "foo" : { "bar" : "y" } }</code>
     */
    @SuppressWarnings("unchecked")
    public void add(String fieldName, String fieldValue)
    {
        if (fieldName == null || fieldValue == null)
        {
            return;
        }

        // Minimise processing in the simple case of no dots in the field name.
        if (fieldName.indexOf(DOT) == -1)
        {
            if (ReservedFieldNames.RESERVED_FIELD_NAMES.contains(fieldName))
            {
                return;
            }
            m_ResultsMap.put(fieldName, fieldValue);
            return;
        }

        String[] segments = DOT_PATTERN.split(fieldName);

        // If any segment created by the split is a reserved word then ignore
        // the whole field.
        for (String segment : segments)
        {
            if (ReservedFieldNames.RESERVED_FIELD_NAMES.contains(segment))
            {
                return;
            }
        }

        Map<String, Object> layerMap = m_ResultsMap;
        for (int i = 0; i < segments.length; ++i)
        {
            String segment = segments[i];
            if (i == segments.length - 1)
            {
                layerMap.put(segment, fieldValue);
            }
            else
            {
                Object existingLayerValue = layerMap.get(segment);
                if (existingLayerValue == null)
                {
                    Map<String, Object> nextLayerMap = new TreeMap<>();
                    layerMap.put(segment, nextLayerMap);
                    layerMap = nextLayerMap;
                }
                else
                {
                    if (existingLayerValue instanceof Map)
                    {
                        layerMap = (Map<String, Object>)existingLayerValue;
                    }
                    else
                    {
                        // This implies an inconsistency - different additions
                        // imply the same path leads to both an object and a
                        // value.  For example:
                        // foo.bar = x
                        // foo.bar.baz = y
                        return;
                    }
                }
            }
        }
    }

    public Map<String, Object> getResultsMap()
    {
        return m_ResultsMap;
    }

    /**
     * Mappings for a given hierarchical structure are more complex than the
     * basic results.
     */
    public Map<String, Object> getMappingsMap()
    {
        Map<String, Object> mappingsMap = new TreeMap<>();
        recurseMappingsLevel(m_ResultsMap, mappingsMap);
        return mappingsMap;
    }

    @SuppressWarnings("unchecked")
    private void recurseMappingsLevel(Map<String, Object> resultsMap,
            Map<String, Object> mappingsMap)
    {
        for (Map.Entry<String, Object> entry : resultsMap.entrySet())
        {
            Map<String, Object> typeMap = new TreeMap<>();

            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map)
            {
                Map<String, Object> propertiesMap = new TreeMap<>();
                recurseMappingsLevel((Map<String, Object>)value, propertiesMap);

                typeMap.put(ElasticsearchMappings.TYPE,
                        ElasticsearchMappings.OBJECT);
                typeMap.put(ElasticsearchMappings.PROPERTIES, propertiesMap);
                mappingsMap.put(name, typeMap);
            }
            else
            {
                typeMap.put(ElasticsearchMappings.TYPE,
                        // Even though the add() method currently only supports
                        // strings, this way of getting the type would work for
                        // many Elasticsearch types, e.g. date, int, long,
                        // double and boolean
                        value.getClass().getSimpleName().toLowerCase());
                mappingsMap.put(name, typeMap);
            }
        }
    }
}
