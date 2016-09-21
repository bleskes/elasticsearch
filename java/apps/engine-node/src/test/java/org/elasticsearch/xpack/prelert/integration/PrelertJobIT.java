package org.elasticsearch.xpack.prelert.integration;

import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.xpack.prelert.integration.hack.ESRestTestCase;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class PrelertJobIT extends ESRestTestCase {

    @Test
    public void test() throws Exception {
        String job = "{\n" +
                "    \"id\":\"farequote\",\n" +
                "    \"description\":\"Analysis of response time by airline\",\n" +
                "    \"analysisConfig\" : {\n" +
                "        \"bucketSpan\":3600,\n" +
                "        \"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]\n" +
                "    },\n" +
                "    \"dataDescription\" : {\n" +
                "        \"fieldDelimiter\":\",\",\n" +
                "        \"timeField\":\"time\",\n" +
                "        \"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\"\n" +
                "    }\n" +
                "}";
        Response response = client().performRequest("post", "engine/v2/jobs", Collections.emptyMap(), new StringEntity(job));
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        assertThat(reader.readLine(), containsString("\"status\":\"CLOSED\""));
    }

}
