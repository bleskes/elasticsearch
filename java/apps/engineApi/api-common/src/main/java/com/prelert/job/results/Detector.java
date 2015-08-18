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

package com.prelert.job.results;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents the anomaly detector.
 * Only the detector name is serialised anomaly records aren't.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties({"records"})
public class Detector
{
    public static final String TYPE = "detector";
    public static final String NAME = "name";
    public static final String RECORDS = "records";

    private String m_Name;
    private List<AnomalyRecord> m_Records;


    public Detector()
    {
        m_Records = new ArrayList<>();
    }

    public Detector(String name)
    {
        this();
        m_Name = name.intern();
    }

    public String getName()
    {
        return m_Name;
    }

    public void setName(String name)
    {
        m_Name = name.intern();
    }

    public void addRecord(AnomalyRecord record)
    {
        m_Records.add(record);
    }

    public List<AnomalyRecord> getRecords()
    {
        return m_Records;
    }
}
