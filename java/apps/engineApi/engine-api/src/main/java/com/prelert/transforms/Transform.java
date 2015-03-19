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

package com.prelert.transforms;

import java.util.List;

import org.apache.log4j.Logger;

/**
 * Abstract transform class.
 * Instances are created with maps telling it which field(s)
 * to read from in the input array and where to write to in
 * the output array. Input and output arrays are passed to the
 * {@link #transform(String[], String[]) function
 */
public abstract class Transform
{
    protected final Logger m_Logger;

    public static class TransformIndex
    {
        public final int array;
        public final int index;

        public TransformIndex(int a, int b)
        {
            this.array = a;
            this.index = b;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + array;
            result = prime * result + index;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            TransformIndex other = (TransformIndex) obj;
            if (array != other.array || index != other.index)
            {
                return false;
            }
            return true;
        }
    }

    protected List<TransformIndex> m_ReadIndicies;
    protected List<TransformIndex> m_WriteIndicies;

    /**
     *
     * @param inputIndicies Indicies into the input record
     * @param scratchAreaIndicies Indicies into the scratch area
     * (where reading the output of another transform or writing
     * an intermediate result)
     * @param outputIndicies Indicies into the output record.
     * @param logger
     * Transform results go into these indicies
     */
    public Transform(List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        m_Logger = logger;

        m_ReadIndicies = readIndicies;
        m_WriteIndicies = writeIndicies;
    }

    /**
     * The indicies for the inputs
     * @return
     */
    public List<TransformIndex> getReadIndicies()
    {
        return m_ReadIndicies;
    }

    /**
     * The write output indicies
     * @return
     */
    public List<TransformIndex> getWriteIndicies()
    {
        return m_WriteIndicies;
    }

    /**
     * Transform function.
     * The read write array of arrays area typically contains an input array,
     * scratch area array and the output array. The scratch area is used in the
     * case where the transform is chained so reads/writes to an intermediate area
     *
     * @param readWriteArea
     * @return
     * @throws TransformException
     */
    public abstract boolean transform(String[][] readWriteArea)
    throws TransformException;
}
