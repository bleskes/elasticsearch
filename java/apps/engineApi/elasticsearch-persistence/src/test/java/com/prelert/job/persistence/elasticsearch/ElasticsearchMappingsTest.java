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

package com.prelert.job.persistence.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.CategorizerState;
import com.prelert.job.JobDetails;
import com.prelert.job.ListDocument;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.ModelState;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.audit.AuditActivity;
import com.prelert.job.audit.AuditMessage;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.BucketProcessingTime;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;
import com.prelert.job.results.ReservedFieldNames;
import com.prelert.job.usage.Usage;


public class ElasticsearchMappingsTest
{
    private void parseJson(JsonParser parser, Set<String> expected)
            throws IOException
    {
        try
        {
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.END_OBJECT)
            {
                switch (token)
                {
                    case START_OBJECT:
                        parseJson(parser, expected);
                        break;
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        expected.add(fieldName);
                        break;
                    default:
                        break;
                }
                token = parser.nextToken();
            }
        }
        catch (JsonParseException e)
        {
            fail("Cannot parse JSON: " + e);
        }
    }

    @Test
    public void testReservedFields()
            throws IOException, ClassNotFoundException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException
    {
        Set<String> overridden = new HashSet<>();

        // These are not reserved because they're Elasticsearch keywords, not field names
        overridden.add(ElasticsearchMappings.ALL);
        overridden.add(ElasticsearchMappings.ANALYZER);
        overridden.add(ElasticsearchMappings.COPY_TO);
        overridden.add(ElasticsearchMappings.DYNAMIC);
        overridden.add(ElasticsearchMappings.ENABLED);
        overridden.add(ElasticsearchMappings.FIELDS);
        overridden.add(ElasticsearchMappings.INCLUDE_IN_ALL);
        overridden.add(ElasticsearchMappings.INDEX);
        overridden.add(ElasticsearchMappings.NESTED);
        overridden.add(ElasticsearchMappings.NO);
        overridden.add(ElasticsearchMappings.PARENT);
        overridden.add(ElasticsearchMappings.PROPERTIES);
        overridden.add(ElasticsearchMappings.RAW);
        overridden.add(ElasticsearchMappings.TYPE);
        overridden.add(ElasticsearchMappings.WHITESPACE);

        // These are not reserved because they're data types, not field names
        overridden.add(AnomalyRecord.TYPE);
        overridden.add(AuditActivity.TYPE);
        overridden.add(AuditMessage.TYPE);
        overridden.add(Bucket.TYPE);
        overridden.add(BucketProcessingTime.TYPE);
        overridden.add(BucketInfluencer.TYPE);
        overridden.add(CategorizerState.TYPE);
        overridden.add(CategoryDefinition.TYPE);
        overridden.add(ElasticsearchJobDataPersister.TYPE);
        overridden.add(Influencer.TYPE);
        overridden.add(JobDetails.TYPE);
        overridden.add(ListDocument.TYPE);
        overridden.add(ModelDebugOutput.TYPE);
        overridden.add(ModelState.TYPE);
        overridden.add(ModelSnapshot.TYPE);
        overridden.add(ModelSizeStats.TYPE);
        overridden.add(Quantiles.TYPE);
        overridden.add(Usage.TYPE);

        // These are not reserved because they're in the prelertinput-* index, not prelertresults-*
        overridden.add(ElasticsearchJobDataPersister.FIELDS);
        overridden.add(ElasticsearchJobDataPersister.BY_FIELDS);
        overridden.add(ElasticsearchJobDataPersister.OVER_FIELDS);
        overridden.add(ElasticsearchJobDataPersister.PARTITION_FIELDS);

        // These are not reserved because they're in the prelert-int index, not prelertresults-*
        overridden.add(AuditActivity.RUNNING_DETECTORS);
        overridden.add(AuditActivity.RUNNING_JOBS);
        overridden.add(AuditActivity.TOTAL_DETECTORS);
        overridden.add(AuditActivity.TOTAL_JOBS);
        overridden.add(AuditMessage.LEVEL);
        overridden.add(AuditMessage.MESSAGE);
        overridden.add(ListDocument.ID);
        overridden.add(ListDocument.ITEMS);

        // These are not reserved because they're analyzed strings, i.e. the same type as user-specified fields
        overridden.add(JobDetails.DESCRIPTION);
        overridden.add(JobDetails.STATUS);
        overridden.add(ModelSnapshot.DESCRIPTION);
        overridden.add(SchedulerConfig.USERNAME);

        Set<String> expected = new HashSet<>();

        Class<?> c = Class.forName("com.prelert.job.persistence.elasticsearch.ElasticsearchMappings");
        Class<?> contentBuilder = Class.forName("org.elasticsearch.common.xcontent.XContentBuilder");

        Method[] allMethods = c.getDeclaredMethods();
        for (Method m : allMethods)
        {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(contentBuilder))
            {
                XContentBuilder builder;
                if (m.getParameterCount() == 0)
                {
                    builder = (XContentBuilder) m.invoke(null);
                }
                else if (m.getParameterCount() == 1)
                {
                    List<Object> args = new ArrayList<>();
                    args.add(null);

                    builder = (XContentBuilder) m.invoke(null, args);
                }
                else
                {
                    continue;
                }
                BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(
                        builder.string().getBytes(StandardCharsets.UTF_8)));

                JsonParser parser = new JsonFactory().createParser(inputStream);
                parseJson(parser, expected);
            }
        }

        expected.removeAll(overridden);
        for (String s : expected)
        {
            // By comparing like this the failure messages say which string is missing
            String reserved = ReservedFieldNames.RESERVED_FIELD_NAMES.contains(s) ? s : null;
            assertEquals(s, reserved);
        }
    }

    @Test
    public void testJobMapping() throws IOException
    {
        StringBuilder expected = new StringBuilder();
        expected.append("{" +
                        "  \"job\": {" +
                        "    \"_all\": {" +
                        "      \"enabled\": false," +
                        "      \"analyzer\":\"whitespace\"" +
                        "    }," +
                        "    \"properties\": {" +
                        "      \"jobId\": {" +
                        "        \"type\": \"keyword\"" +
                        "      }," +
                        "      \"description\": {" +
                        "        \"type\": \"text\"" +
                        "      }," +
                        "      \"status\": {" +
                        "        \"type\": \"text\"" +
                        "      }," +
                        "      \"schedulerStatus\": {" +
                        "        \"type\": \"keyword\"" +
                        "      }," +
                        "      \"@timestamp\": {" +
                        "        \"type\": \"date\"" +
                        "      }," +
                        "      \"finishedTime\": {" +
                        "        \"type\": \"date\"" +
                        "      }," +
                        "      \"lastDataTime\": {" +
                        "        \"type\": \"date\"" +
                        "      }," +
                        "      \"counts\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"bucketCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"processedRecordCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"processedFieldCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"inputBytes\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"inputRecordCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"inputFieldCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"invalidDateCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"missingFieldCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"outOfOrderTimeStampCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"failedTransformCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"excludedRecordCount\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"latestRecordTimeStamp\": {" +
                        "            \"type\": \"date\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"ignoreDowntime\": {" +
                        "        \"type\": \"keyword\"" +
                        "      }," +
                        "      \"timeout\": {" +
                        "        \"type\": \"long\"," +
                        "        \"index\": \"no\"" +
                        "      }," +
                        "      \"renormalizationWindowDays\": {" +
                        "        \"type\": \"long\"," +
                        "        \"index\": \"no\"" +
                        "      }," +
                        "      \"backgroundPersistInterval\": {" +
                        "        \"type\": \"long\"," +
                        "        \"index\": \"no\"" +
                        "      }," +
                        "      \"modelSnapshotRetentionDays\": {" +
                        "        \"type\": \"long\"," +
                        "        \"index\": \"no\"" +
                        "      }," +
                        "      \"resultsRetentionDays\": {" +
                        "        \"type\": \"long\"," +
                        "        \"index\": \"no\"" +
                        "      }," +
                        "      \"analysisConfig\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"bucketSpan\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"batchSpan\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"latency\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"period\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"summaryCountFieldName\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"categorizationFieldName\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"categorizationFilters\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"detectors\": {" +
                        "            \"properties\": {" +
                        "              \"detectorDescription\": {" +
                        "                \"type\": \"keyword\"" +
                        "              }," +
                        "              \"function\": {" +
                        "                \"type\": \"keyword\"" +
                        "              }," +
                        "              \"fieldName\": {" +
                        "                \"type\": \"keyword\"" +
                        "              }," +
                        "              \"byFieldName\": {" +
                        "                \"type\": \"keyword\"" +
                        "              }," +
                        "              \"overFieldName\": {" +
                        "                \"type\": \"keyword\"" +
                        "              }," +
                        "              \"partitionFieldName\": {" +
                        "                \"type\": \"keyword\"" +
                        "              }," +
                        "              \"useNull\": {" +
                        "                \"type\": \"boolean\"" +
                        "              }," +
                        "              \"detectorRules\": {" +
                        "                \"type\": \"object\"," +
                        "                \"properties\": {" +
                        "                  \"ruleAction\": {" +
                        "                    \"type\":\"keyword\"" +
                        "                  }," +
                        "                  \"targetFieldName\": {" +
                        "                    \"type\":\"keyword\"" +
                        "                  }," +
                        "                  \"targetFieldValue\": {" +
                        "                    \"type\":\"keyword\"" +
                        "                  }," +
                        "                  \"conditionsConnective\": {" +
                        "                    \"type\":\"keyword\"" +
                        "                  }," +
                        "                  \"ruleConditions\": {" +
                        "                    \"type\":\"object\"," +
                        "                    \"properties\": {" +
                        "                      \"conditionType\": {" +
                        "                        \"type\":\"keyword\"" +
                        "                      }," +
                        "                      \"fieldName\": {" +
                        "                        \"type\":\"keyword\"" +
                        "                      }," +
                        "                      \"fieldValue\": {" +
                        "                        \"type\":\"keyword\"" +
                        "                      }," +
                        "                      \"valueList\": {" +
                        "                        \"type\":\"keyword\"" +
                        "                      }" +
                        "                    }" +
                        "                  }" +
                        "                }" +
                        "              }" +
                        "            }" +
                        "          }," +
                        "          \"overlappingBuckets\": {" +
                        "            \"type\": \"boolean\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"resultFinalizationWindow\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"multivariateByFields\": {" +
                        "            \"type\": \"boolean\"," +
                        "            \"index\": \"no\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"analysisLimits\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"modelMemoryLimit\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"categorizationExamplesLimit\": {" +
                        "            \"type\": \"long\"," +
                        "            \"index\": \"no\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"dataDescription\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"format\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"timeField\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"timeFormat\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"fieldDelimiter\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"quoteCharacter\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"transforms\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"transform\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"arguments\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"inputs\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"outputs\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"schedulerConfig\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"dataSource\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"dataSourceCompatibility\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"queryDelay\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"frequency\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"filePath\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"tailFile\": {" +
                        "            \"type\": \"boolean\"" +
                        "          }," +
                        "          \"baseUrl\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"username\": {" +
                        "            \"type\": \"text\"" +
                        "          }," +
                        "          \"encryptedPassword\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"indexes\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"types\": {" +
                        "            \"type\": \"keyword\"" +
                        "          }," +
                        "          \"retrieveWholeSource\": {" +
                        "            \"type\": \"boolean\"" +
                        "          }," +
                        "          \"query\": {" +
                        "            \"type\": \"object\"," +
                        "            \"dynamic\": false" +
                        "          }," +
                        "          \"aggregations\": {" +
                        "            \"type\": \"object\"," +
                        "            \"dynamic\": false" +
                        "          }," +
                        "          \"aggs\": {" +
                        "            \"type\": \"object\"," +
                        "            \"dynamic\": false" +
                        "          }," +
                        "          \"script_fields\": {" +
                        "            \"type\": \"object\"," +
                        "            \"dynamic\": false" +
                        "          }," +
                        "          \"scrollSize\": {" +
                        "            \"type\": \"integer\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"modelDebugConfig\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"writeTo\": {" +
                        "            \"type\": \"text\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"boundsPercentile\": {" +
                        "            \"type\": \"double\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"terms\": {" +
                        "            \"type\": \"text\"," +
                        "            \"index\": \"no\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"customSettings\": {" +
                        "        \"type\": \"object\"," +
                        "        \"dynamic\": false" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}");

        assertEquals(expected.toString().replaceAll("\\s", ""),
                ElasticsearchMappings.jobMapping().string());
    }
}
