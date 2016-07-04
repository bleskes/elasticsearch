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

package com.prelert.transforms;

import static org.apache.lucene.spatial.util.GeoEncodingUtils.mortonUnhashLat;
import static org.apache.lucene.spatial.util.GeoEncodingUtils.mortonUnhashLon;
import static org.apache.lucene.spatial.util.GeoHashUtils.mortonEncode;

import java.util.List;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

public class StringTransform extends Transform
{
    private final Function<String, String> m_ConvertFunction;

    private StringTransform(Function<String, String> convertFunction,
            List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
        m_ConvertFunction = convertFunction;
        Preconditions.checkArgument(readIndicies.size() == 1);
        Preconditions.checkArgument(writeIndicies.size() == 1);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea) throws TransformException
    {
        TransformIndex readIndex = m_ReadIndicies.get(0);
        TransformIndex writeIndex = m_WriteIndicies.get(0);
        String input = readWriteArea[readIndex.array][readIndex.index];
        readWriteArea[writeIndex.array][writeIndex.index] = m_ConvertFunction.apply(input);
        return TransformResult.OK;
    }

    public static StringTransform createLowerCase(List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        return new StringTransform(s -> s.toLowerCase(), readIndicies, writeIndicies, logger);
    }

    public static StringTransform createUpperCase(List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        return new StringTransform(s -> s.toUpperCase(), readIndicies, writeIndicies, logger);
    }

    public static StringTransform createTrim(List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        return new StringTransform(s -> s.trim(), readIndicies, writeIndicies, logger);
    }

    public static StringTransform createGeoUnhash(List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        return new StringTransform(s -> geoUnhash(s), readIndicies, writeIndicies, logger);
    }

    /**
     * Convert a Geohash (https://en.wikipedia.org/wiki/Geohash) value to a
     * string of the form "latitude,longitude".
     */
    private static String geoUnhash(String geoHash)
    {
        long hash = mortonEncode(geoHash);
        StringBuilder strBuilder = new StringBuilder();
        return strBuilder.append(mortonUnhashLat(hash))
                .append(',').append(mortonUnhashLon(hash)).toString();
    }
}
