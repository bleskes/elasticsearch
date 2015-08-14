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

package com.prelert.rs.data;

/**
 * The acknowledgement message for the REST API.
 * Operations such as delete that don't return data
 * should return this.
 */
public class Acknowledgement
{
    private boolean m_Ack;

    /**
     * Default is true
     */
    public Acknowledgement()
    {
        m_Ack = true;
    }

    public Acknowledgement(boolean ack)
    {
        m_Ack = ack;
    }

    /**
     * Get the acknowledgement value.
     * @return true
     */
    public boolean getAcknowledgement()
    {
        return m_Ack;
    }

    /**
     * Set the acknowledgement value.
     * @param value
     */
    public void setAcknowledgement(boolean value)
    {
        m_Ack = value;
    }
}
