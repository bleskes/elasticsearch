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

package com.prelert.job.persistence;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class JobDataPersisterTest
{

    @Test
    public void testSetFieldMappings()
    {
        {
            List<String> fields = Arrays.asList("field1", "field2");
            List<String> byFields = Arrays.asList("byfield1");
            List<String> overFields = Arrays.asList("overfield1");
            List<String> partitionFields = Arrays.asList("partitionfield1");


            // example input
            // String input [] = {"field1", "byfield1", "partitionfield1", "overfield1", "field2"};
            Map<String, Integer> fieldMap = new HashMap<>();
            fieldMap.put("field1", 0);
            fieldMap.put("byfield1", 1);
            fieldMap.put("partitionfield1", 2);
            fieldMap.put("overfield1", 3);
            fieldMap.put("field2", 4);

            DummyJobDataPersister persister = new DummyJobDataPersister();
            persister.setFieldMappings(fields, byFields, overFields, partitionFields, fieldMap);

            assertArrayEquals(fields.toArray(new String[0]), persister.getFieldNames());
            assertArrayEquals(new int [] {0, 4}, persister.getFieldMappings());
            assertArrayEquals(new int [] {1}, persister.getByFieldMappings());
            assertArrayEquals(new int [] {3}, persister.getOverFieldMappings());
            assertArrayEquals(new int [] {2}, persister.getPartitionFieldMappings());
        }

        {
            List<String> fields = Arrays.asList("field1");
            List<String> byFields = Arrays.asList("byfield1", "byfield2");
            List<String> overFields = Arrays.asList("overfield1");
            List<String> partitionFields = Arrays.asList("partitionfield1", "partitionfield2");


            // example input
            // String input [] = {"byfield1", "field1", "partitionfield1", "overfield1", "byfield2", "partitionfield2"};
            Map<String, Integer> fieldMap = new HashMap<>();
            fieldMap.put("byfield1", 0);
            fieldMap.put("field1", 1);
            fieldMap.put("partitionfield1", 2);
            fieldMap.put("overfield1", 3);
            fieldMap.put("byfield2", 4);
            fieldMap.put("partitionfield2", 5);

            DummyJobDataPersister persister = new DummyJobDataPersister();
            persister.setFieldMappings(fields, byFields, overFields, partitionFields, fieldMap);

            assertArrayEquals(fields.toArray(new String[0]), persister.getFieldNames());
            assertArrayEquals(new int [] {1}, persister.getFieldMappings());
            assertArrayEquals(new int [] {0, 4}, persister.getByFieldMappings());
            assertArrayEquals(new int [] {3}, persister.getOverFieldMappings());
            assertArrayEquals(new int [] {2, 5}, persister.getPartitionFieldMappings());
        }
    }

    /**
     * field2 in the input field names isn't in the input map
     */
    @Test
    public void testSetFieldMappings_MissingField()
    {
        {
            List<String> fields = Arrays.asList("field1", "field2");
            List<String> byFields = Arrays.asList("byfield1");
            List<String> overFields = Arrays.asList("overfield1");
            List<String> partitionFields = Arrays.asList("partitionfield1");


            // example input
            // String input [] = {"field1", "byfield1", "partitionfield1", "overfield1", "field2"};

            Map<String, Integer> fieldMap = new HashMap<>();
            fieldMap.put("field1", 0);
            fieldMap.put("byfield1", 1);
            fieldMap.put("partitionfield1", 2);
            fieldMap.put("overfield1", 3);

            DummyJobDataPersister persister = new DummyJobDataPersister();
            persister.setFieldMappings(fields, byFields, overFields, partitionFields, fieldMap);

            assertArrayEquals(fields.toArray(new String[0]), persister.getFieldNames());
            assertArrayEquals(new int [] {0}, persister.getFieldMappings());
            assertArrayEquals(new int [] {1}, persister.getByFieldMappings());
            assertArrayEquals(new int [] {3}, persister.getOverFieldMappings());
            assertArrayEquals(new int [] {2}, persister.getPartitionFieldMappings());
        }

        {
            List<String> fields = Arrays.asList("field1", "field2");
            List<String> byFields = Arrays.asList("byfield1");
            List<String> overFields = Arrays.asList("overfield1");
            List<String> partitionFields = Arrays.asList("partitionfield1");


            // example input
            // String input [] = {"byfield1", "field1", "partitionfield1", "overfield1", "field2"};

            // field2 isn't in the input map
            Map<String, Integer> fieldMap = new HashMap<>();
            fieldMap.put("byfield1", 0);
            fieldMap.put("field1", 1);
            fieldMap.put("partitionfield1", 2);
            fieldMap.put("overfield1", 3);

            DummyJobDataPersister persister = new DummyJobDataPersister();
            persister.setFieldMappings(fields, byFields, overFields, partitionFields, fieldMap);

            assertArrayEquals(fields.toArray(new String[0]), persister.getFieldNames());
            assertArrayEquals(new int [] {1}, persister.getFieldMappings());
            assertArrayEquals(new int [] {0}, persister.getByFieldMappings());
            assertArrayEquals(new int [] {3}, persister.getOverFieldMappings());
            assertArrayEquals(new int [] {2}, persister.getPartitionFieldMappings());
        }
    }



}
