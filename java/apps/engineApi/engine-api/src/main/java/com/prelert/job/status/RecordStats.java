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

package com.prelert.job.status;

public class RecordStats
{
    protected long m_RecordsWritten = 0;
    protected long m_InputFieldCount = 0;
    protected long m_BytesRead = 0;
    protected long m_DateParseErrorsCount = 0;
    protected long m_MissingFieldErrorCount = 0;
    protected long m_OutOfOrderRecordCount = 0;
    protected long m_FailedTransformCount = 0;

    public long getRecordsWritten()
    {
        return m_RecordsWritten;
    }

    public void setRecordsWritten(long m_RecordsWritten)
    {
        this.m_RecordsWritten = m_RecordsWritten;
    }

    public long getDateParseErrorsCount()
    {
        return m_DateParseErrorsCount;
    }

    public void setDateParseErrorsCount(long m_DateParseErrorsCount)
    {
        this.m_DateParseErrorsCount = m_DateParseErrorsCount;
    }

    public long getMissingFieldErrorCount()
    {
        return m_MissingFieldErrorCount;
    }

    public void setMissingFieldErrorCount(long m_MissingFieldErrorCount)
    {
        this.m_MissingFieldErrorCount = m_MissingFieldErrorCount;
    }

    public long getOutOfOrderRecordCount()
    {
        return m_OutOfOrderRecordCount;
    }

    public void setOutOfOrderRecordCount(long m_OutOfOrderRecordCount)
    {
        this.m_OutOfOrderRecordCount = m_OutOfOrderRecordCount;
    }

    public long getBytesRead()
    {
        return m_BytesRead;
    }

    public void setBytesRead(long m_BytesRead)
    {
        this.m_BytesRead = m_BytesRead;
    }

    public long getFailedTransformCount()
    {
        return m_FailedTransformCount;
    }

    public void setFailedTransformCount(long m_FailedTransformCount)
    {
        this.m_FailedTransformCount = m_FailedTransformCount;
    }

    public long getInputFieldCount()
    {
        return m_InputFieldCount;
    }

    public void setInputFieldCount(long m_InputFieldCount)
    {
        this.m_InputFieldCount = m_InputFieldCount;
    }
}
