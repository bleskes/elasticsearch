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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.util.List;

/**
 * Interface for classes that write arrays of strings to the
 * Prelert analytics processes.
 */
public interface RecordWriter
{
    /**
     * Value must match api::CAnomalyDetector::CONTROL_FIELD_NAME in the C++
     * code.
     */
    public static final String CONTROL_FIELD_NAME = ".";

    /**
     * Write each String in the record array
     * @param record
     * @throws IOException
     * @see {@link #writeRecord(List)}
     */
    public abstract void writeRecord(String[] record) throws IOException;

    /**
     * Write each String in the record list
     *
     * @param record
     * @throws IOException
     * @see {@link #writeRecord(String[])}
     */
    public abstract void writeRecord(List<String> record) throws IOException;

    /**
     * Flush the output stream.
     * @throws IOException
     */
    public abstract void flush() throws IOException;

}