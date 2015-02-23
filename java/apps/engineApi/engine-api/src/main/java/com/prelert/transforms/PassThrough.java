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
 * A no op transform simply passes data through it
 */
public class PassThrough extends Transform
{
    public PassThrough(int[] inputIndicies, int[] outputIndicies)
    {
        super(inputIndicies, outputIndicies);
    }

    @Override
    public boolean transform(String[] inputRecord, String[] outputRecord)
    throws TransformException
    {
        for (int i=0; i<m_InputIndicies.length; i++)
        {
            outputRecord[m_OutputIndicies[i]] = inputRecord[m_InputIndicies[i]];
        }

        return true;
    }

}
