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

package com.prelert.job.alert;


public enum AlertType
{
    BUCKET
    {
        @Override
        public String toString()
        {
            return new String("bucket");
        }
    },

    INFLUENCER
    {
        @Override
        public String toString()
        {
            return new String("influencer");
        }
    },

    BUCKETINFLUENCER
    {
        @Override
        public String toString()
        {
            return new String("bucketinfluencer");
        }
    };

    public static AlertType fromString(String str)
    {
        for (AlertType at : AlertType.values())
        {
            if (at.toString().equals(str))
            {
                return at;
            }
        }

        throw new IllegalArgumentException("The string '" + str +
                "' cannot be converted to an AlertType");
    }
}