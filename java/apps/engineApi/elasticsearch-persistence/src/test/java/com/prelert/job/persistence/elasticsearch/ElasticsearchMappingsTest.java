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
import static org.junit.Assert.assertTrue;

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
import com.prelert.job.results.ReservedFieldNames;
import com.prelert.job.usage.Usage;

public class ElasticsearchMappingsTest
{
    private void parseJson(JsonParser parser, Set<String> actual)
    {
        try
        {
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.END_OBJECT)
            {
                switch (token)
                {
                    case START_OBJECT:
                        parseJson(parser, actual);
                        break;
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        //System.out.println("Field: " + fieldName);
                        actual.add(fieldName);
                        break;
                    default:
                        break;
                }
                token = parser.nextToken();
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReservedFields() throws IOException
    {
        Set<String> reserved = ReservedFieldNames.RESERVED_FIELD_NAMES;

        Set<String> overridden = new HashSet<>();
        overridden.add(JobDetails.DESCRIPTION);
        overridden.add(Usage.TYPE);
        overridden.add(CategorizerState.TYPE);
        overridden.add(ElasticsearchMappings.TYPE);
        overridden.add(ElasticsearchMappings.ALL);
        overridden.add(ElasticsearchMappings.ENABLED);
        overridden.add(ElasticsearchMappings.NOT_ANALYZED);
        overridden.add(ElasticsearchMappings.INDEX);
        overridden.add(ElasticsearchMappings.NO);
        overridden.add(ElasticsearchMappings.ALL);
        overridden.add(ElasticsearchMappings.ENABLED);
        overridden.add(ElasticsearchMappings.ANALYZER);
        overridden.add(ElasticsearchMappings.WHITESPACE);
        overridden.add(ElasticsearchMappings.INCLUDE_IN_ALL);
        overridden.add(ElasticsearchMappings.NESTED);
        overridden.add(ElasticsearchMappings.COPY_TO);
        overridden.add(ElasticsearchMappings.PARENT);
        overridden.add(ElasticsearchMappings.PROPERTIES);
        overridden.add(ElasticsearchMappings.TYPE);
        overridden.add(ElasticsearchMappings.DYNAMIC);

        // FIXME! Check these strings!
        overridden.add("influencer");
        overridden.add("record");
        overridden.add("bucketInfluencer");
        overridden.add("modelSnapshot");
        overridden.add("modelState");
        overridden.add("saved-data");
        overridden.add("auditMessage");
        overridden.add("bucket");
        overridden.add("quantiles");
        overridden.add("modelDebugOutput");
        overridden.add("job");
        overridden.add("categoryDefinition");
        overridden.add("categoryId");
        overridden.add("status");
        overridden.add("partitionFields");
        overridden.add("overFields");
        overridden.add("byFields");
        overridden.add("schedulerConfig");
        overridden.add("fields");
        overridden.add("dataDescription");
        overridden.add("username");
        overridden.add("inputRecordCount");
        overridden.add("arguments");


        Set<String> actual = new HashSet<>();

        try
        {
            Class<?> c = Class.forName("com.prelert.job.persistence.elasticsearch.ElasticsearchMappings");
            Class<?> contentBuilder = Class.forName("org.elasticsearch.common.xcontent.XContentBuilder");

            Method[] allMethods = c.getDeclaredMethods();
            for (Method m : allMethods)
            {
                if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(contentBuilder))
                {
                    String mname = m.getName();
                    //System.out.println("Found static method: " + mname + ", type " +  m.getReturnType());
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
                    parseJson(parser, actual);
                }
            }

            for (String s : actual)
            {
                if (overridden.contains(s))
                {
                    continue;
                }
                //System.out.println("Testing string " + s);
                assertTrue(reserved.contains(s));
            }
        }
        catch (ClassNotFoundException x)
        {
            x.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
                        "        \"type\": \"string\"," +
                        "        \"index\": \"not_analyzed\"" +
                        "      }," +
                        "      \"description\": {" +
                        "        \"type\": \"string\"" +
                        "      }," +
                        "      \"status\": {" +
                        "        \"type\": \"string\"" +
                        "      }," +
                        "      \"schedulerStatus\": {" +
                        "        \"type\": \"string\"," +
                        "        \"index\": \"not_analyzed\"" +
                        "      }," +
                        "      \"createTime\": {" +
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
                        "        \"type\": \"string\"," +
                        "        \"index\": \"not_analyzed\"" +
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
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"categorizationFieldName\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"detectors\": {" +
                        "            \"properties\": {" +
                        "              \"detectorDescription\": {" +
                        "                \"type\": \"string\"," +
                        "                \"index\": \"not_analyzed\"" +
                        "              }," +
                        "              \"function\": {" +
                        "                \"type\": \"string\"," +
                        "                \"index\": \"not_analyzed\"" +
                        "              }," +
                        "              \"fieldName\": {" +
                        "                \"type\": \"string\"," +
                        "                \"index\": \"not_analyzed\"" +
                        "              }," +
                        "              \"byFieldName\": {" +
                        "                \"type\": \"string\"," +
                        "                \"index\": \"not_analyzed\"" +
                        "              }," +
                        "              \"overFieldName\": {" +
                        "                \"type\": \"string\"," +
                        "                \"index\": \"not_analyzed\"" +
                        "              }," +
                        "              \"partitionFieldName\": {" +
                        "                \"type\": \"string\"," +
                        "                \"index\": \"not_analyzed\"" +
                        "              }," +
                        "              \"useNull\": {" +
                        "                \"type\": \"boolean\"" +
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
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"timeField\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"timeFormat\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"fieldDelimiter\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"quoteCharacter\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"transforms\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"transform\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"arguments\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"inputs\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"outputs\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }" +
                        "        }" +
                        "      }," +
                        "      \"schedulerConfig\": {" +
                        "        \"type\": \"object\"," +
                        "        \"properties\": {" +
                        "          \"dataSource\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"queryDelay\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"frequency\": {" +
                        "            \"type\": \"long\"" +
                        "          }," +
                        "          \"filePath\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"tailFile\": {" +
                        "            \"type\": \"boolean\"" +
                        "          }," +
                        "          \"baseUrl\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"username\": {" +
                        "            \"type\": \"string\"" +
                        "          }," +
                        "          \"encryptedPassword\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"indexes\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
                        "          }," +
                        "          \"types\": {" +
                        "            \"type\": \"string\"," +
                        "            \"index\": \"not_analyzed\"" +
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
                        "            \"type\": \"string\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"boundsPercentile\": {" +
                        "            \"type\": \"double\"," +
                        "            \"index\": \"no\"" +
                        "          }," +
                        "          \"terms\": {" +
                        "            \"type\": \"string\"," +
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
