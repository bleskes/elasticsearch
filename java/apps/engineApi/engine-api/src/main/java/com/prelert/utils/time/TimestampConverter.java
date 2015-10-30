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

package com.prelert.utils.time;

import java.time.format.DateTimeParseException;

/**
 * A converter that enables conversions of textual timestamps to epoch seconds
 * or milliseconds according to a given pattern.
 */
public interface TimestampConverter
{
    /**
     * Converts the a textual timestamp into an epoch in seconds
     *
     * @param timestamp the timestamp to convert, not null. The timestamp is expected to
     * be formatted according to the pattern of the formatter. In addition, the pattern is
     * assumed to contain both date and time information.
     * @return the epoch in seconds for the given timestamp
     * @throws DateTimeParseException if unable to parse the given timestamp
     */
    long toEpochSeconds(String timestamp);

    /**
     * Converts the a textual timestamp into an epoch in milliseconds
     *
     * @param timestamp the timestamp to convert, not null. The timestamp is expected to
     * be formatted according to the pattern of the formatter. In addition, the pattern is
     * assumed to contain both date and time information.
     * @return the epoch in milliseconds for the given timestamp
     * @throws DateTimeParseException if unable to parse the given timestamp
     */
    long toEpochMillis(String timestamp);
}
