/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.master.superjob;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;


public class DataForker
{
    private static final Logger LOGGER = Logger.getLogger(DataForker.class);

    private static final int MAX_LINES_PER_RECORD = 100;

    private final String m_PartitionField;
    private final JobRouter m_JobRouter;

    public DataForker(String partitionField, JobRouter router)
    {
        m_PartitionField = partitionField;
        m_JobRouter = router;
    }

    public void forkData(InputStream stream) throws IOException
    {
        CsvPreference csvPref = new CsvPreference.Builder('"', ',', "\n")
                .maxLinesPerRow(MAX_LINES_PER_RECORD).build();


        try (CsvListReader csvReader = new CsvListReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                csvPref))
        {
            String[] header = csvReader.getHeader(true);
            if (header == null) // null if EoF
            {
                LOGGER.warn("No data");
                return;
            }

            List<String> headerList = Arrays.asList(header);
            int partitionFieldIndex = headerList.indexOf(m_PartitionField);
            if (partitionFieldIndex < 0)
            {
                LOGGER.warn(String.format("Partition field %s not found in header %s",
                                    m_PartitionField, headerList));
                return;
            }

            // Write the header to all destinations
            m_JobRouter.routeToAll(String.join(",", header) + '\n');

            List<String> line;
            while ((line = csvReader.read()) != null)
            {
                String partitionFieldValue = line.get(partitionFieldIndex);
                m_JobRouter.routeToJob(partitionFieldValue, String.join(",", line) + '\n');
            }

        }
    }
}
