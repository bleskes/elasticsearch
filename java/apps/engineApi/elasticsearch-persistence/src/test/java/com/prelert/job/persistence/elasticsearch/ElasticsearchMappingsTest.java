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

import java.io.IOException;

import org.junit.Test;

public class ElasticsearchMappingsTest
{
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
                        "            \"type\": \"object\"" +
                        "          }," +
                        "          \"aggregations\": {" +
                        "            \"type\": \"object\"" +
                        "          }," +
                        "          \"aggs\": {" +
                        "            \"type\": \"object\"" +
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
                        "        \"type\": \"object\"" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}");

        assertEquals(expected.toString().replaceAll("\\s", ""),
                ElasticsearchMappings.jobMapping().string());
    }
}
