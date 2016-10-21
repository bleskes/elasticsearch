
package org.elasticsearch.xpack.prelert.job.persistence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.exceptions.CannotMapJobFromJson;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JsonViews;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.results.ReservedFieldNames;

public class ElasticsearchJobDetailsMapperTest extends ESTestCase {
    @Mock
    private Client client;
    private ObjectMapper objectMapper;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.setConfig(objectMapper.getSerializationConfig().withView(
                JsonViews.DatastoreView.class));
    }

    public void testMap_GivenJobSourceCannotBeParsed() {
        Map<String, Object> source = new HashMap<>();
        source.put("invalidKey", true);

        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(false);
        GetRequestBuilder getRequestBuilder = mock(GetRequestBuilder.class);
        when(getRequestBuilder.get()).thenReturn(getResponse);
        when(client.prepareGet("prelertresults-foo", ModelSizeStats.TYPE.getPreferredName(), ModelSizeStats.TYPE.getPreferredName()))
        .thenReturn(getRequestBuilder);

        ElasticsearchJobDetailsMapper mapper = new ElasticsearchJobDetailsMapper(client, objectMapper);

        ESTestCase.expectThrows(CannotMapJobFromJson.class, () -> mapper.map(source));
    }

    public void testMap_GivenModelSizeStatsExists() {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        modelSizeStats.setModelBytes(42L);
        Date now = new Date();
        modelSizeStats.setTimestamp(now);

        JobDetails originalJob = new JobConfiguration().build();
        originalJob.setId("foo");

        Map<String, Object> source = objectMapper.convertValue(originalJob,
                new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> modelSizeStatsSource = objectMapper.convertValue(modelSizeStats,
                new TypeReference<Map<String, Object>>() {
        });
        Object timestamp = modelSizeStatsSource.remove(ModelSizeStats.TIMESTAMP_FIELD.getPreferredName());
        modelSizeStatsSource.put(ElasticsearchMappings.ES_TIMESTAMP, timestamp);

        GetResponse getModelSizeResponse = mock(GetResponse.class);
        when(getModelSizeResponse.isExists()).thenReturn(true);
        when(getModelSizeResponse.getSource()).thenReturn(modelSizeStatsSource);
        GetRequestBuilder getModelSizeRequestBuilder = mock(GetRequestBuilder.class);
        when(getModelSizeRequestBuilder.get()).thenReturn(getModelSizeResponse);
        when(client.prepareGet("prelertresults-foo", ModelSizeStats.TYPE.getPreferredName(), ModelSizeStats.TYPE.getPreferredName()))
        .thenReturn(getModelSizeRequestBuilder);


        Map<String, Object> procTimeSource = new HashMap<>();
        procTimeSource.put(ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS, 20.2);

        GetResponse getProcTimeResponse = mock(GetResponse.class);
        when(getProcTimeResponse.isExists()).thenReturn(true);
        when(getProcTimeResponse.getSource()).thenReturn(procTimeSource);
        GetRequestBuilder getProcTimeRequestBuilder = mock(GetRequestBuilder.class);
        when(getProcTimeRequestBuilder.get()).thenReturn(getProcTimeResponse);
        when(client.prepareGet("prelertresults-foo", ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE,
                ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS))
        .thenReturn(getProcTimeRequestBuilder);


        ElasticsearchJobDetailsMapper mapper = new ElasticsearchJobDetailsMapper(client, objectMapper);

        JobDetails mappedJob = mapper.map(source);

        assertEquals("foo", mappedJob.getId());
        assertEquals(42L, mappedJob.getModelSizeStats().getModelBytes());
        assertEquals(now, mappedJob.getModelSizeStats().getTimestamp());
        assertEquals(20.2, mappedJob.getAverageBucketProcessingTimeMs(), 0.0001);
    }

    public void testMap_GivenModelSizeStatsDoesNotExist() {
        JobDetails originalJob = new JobConfiguration().build();
        originalJob.setId("foo");

        Map<String, Object> source = objectMapper.convertValue(originalJob,
                new TypeReference<Map<String, Object>>() {
        });

        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(false);
        GetRequestBuilder getRequestBuilder = mock(GetRequestBuilder.class);
        when(getRequestBuilder.get()).thenReturn(getResponse);
        when(client.prepareGet("prelertresults-foo", ModelSizeStats.TYPE.getPreferredName(), ModelSizeStats.TYPE.getPreferredName()))
                .thenReturn(getRequestBuilder);


        GetResponse getProcTimeResponse = mock(GetResponse.class);
        when(getProcTimeResponse.isExists()).thenReturn(false);
        GetRequestBuilder getProcTimeRequestBuilder = mock(GetRequestBuilder.class);
        when(getProcTimeRequestBuilder.get()).thenReturn(getProcTimeResponse);
        when(client.prepareGet("prelertresults-foo", ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE,
                ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS))
        .thenReturn(getProcTimeRequestBuilder);

        ElasticsearchJobDetailsMapper mapper = new ElasticsearchJobDetailsMapper(client, objectMapper);

        JobDetails mappedJob = mapper.map(source);

        assertEquals("foo", mappedJob.getId());
        assertNull(mappedJob.getModelSizeStats());
        assertNull(mappedJob.getAverageBucketProcessingTimeMs());
    }
}
