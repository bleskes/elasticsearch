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
 * Split a hostname into Highest Registered Domain and sub domain.
 * TODO Reimplement porting the code from C++
 */
public class HighestRegisteredDomain extends Transform
{
    /**
     * Immutable class for the domain split results
     */
    public static class DomainSplit
    {
        private String m_SubDomain;
        private String m_HighestRegisteredDomain;

        private DomainSplit(String subDomain, String highestRegisteredDomain)
        {
            m_SubDomain = subDomain;
            m_HighestRegisteredDomain = highestRegisteredDomain;
        }

        public String getSubDomain()
        {
            return m_SubDomain;
        }

        public String getHighestRegisteredDomain()
        {
            return m_HighestRegisteredDomain;
        }
    }

    public HighestRegisteredDomain(List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea)
    {
        return TransformResult.FAIL;
    }
}
