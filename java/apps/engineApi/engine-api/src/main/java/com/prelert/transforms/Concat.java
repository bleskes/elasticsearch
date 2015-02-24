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

/**
 * Concatenate input fields
 */
public class Concat extends Transform
{
    public Concat(int[] inputIndicies, int[] outputIndicies)
    {
        super(inputIndicies, outputIndicies);
    }

    /**
     * Concat has only 1 output field
     */
    @Override
    public boolean transform(String[] inputRecord, String[] outputRecord)
    throws TransformException
    {
        if (m_OutputIndicies.length == 0)
        {
            return true;
        }

        StringBuilder builder = new StringBuilder();

        for (int i=0; i<m_InputIndicies.length; i++)
        {
            builder.append(inputRecord[m_InputIndicies[i]]);
        }

        outputRecord[m_OutputIndicies[0]] = builder.toString();

        return true;
    }

}
