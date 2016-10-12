
package org.elasticsearch.xpack.prelert.job.persistence;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.audit.AuditActivity;
import org.elasticsearch.xpack.prelert.job.audit.AuditMessage;
import org.elasticsearch.xpack.prelert.job.audit.Level;

public class ElasticsearchAuditorTest extends ESTestCase {
    @Mock
    private Client client;
    @Mock
    private ListenableActionFuture<IndexResponse> indexResponse;
    @Captor
    private ArgumentCaptor<String> indexCaptor;
    @Captor
    private ArgumentCaptor<XContentBuilder> jsonCaptor;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }

    
    public void testInfo() {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(client, "prelert-int", "foo");

        auditor.info("Here is my info");

        assertEquals("prelert-int", indexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("foo", auditMessage.getJobId());
        assertEquals("Here is my info", auditMessage.getMessage());
        assertEquals(Level.INFO, auditMessage.getLevel());
    }

    
    public void testWarning() {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(client, "someIndex", "bar");

        auditor.warning("Here is my warning");

        assertEquals("someIndex", indexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("bar", auditMessage.getJobId());
        assertEquals("Here is my warning", auditMessage.getMessage());
        assertEquals(Level.WARNING, auditMessage.getLevel());
    }

    
    public void testError() {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(client, "someIndex", "foobar");

        auditor.error("Here is my error");

        assertEquals("someIndex", indexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("foobar", auditMessage.getJobId());
        assertEquals("Here is my error", auditMessage.getMessage());
        assertEquals(Level.ERROR, auditMessage.getLevel());
    }

    
    public void testActivity_GivenString() {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(client, "someIndex", "");

        auditor.activity("Here is my activity");

        assertEquals("someIndex", indexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("", auditMessage.getJobId());
        assertEquals("Here is my activity", auditMessage.getMessage());
        assertEquals(Level.ACTIVITY, auditMessage.getLevel());
    }

    
    public void testActivity_GivenNumbers() {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(client, "someIndex", "");

        auditor.activity(10, 100, 5, 50);

        assertEquals("someIndex", indexCaptor.getValue());
        AuditActivity auditActivity = parseAuditActivity();
        assertEquals(10, auditActivity.getTotalJobs());
        assertEquals(100, auditActivity.getTotalDetectors());
        assertEquals(5, auditActivity.getRunningJobs());
        assertEquals(50, auditActivity.getRunningDetectors());
    }

    
    public void testError_GivenNoSuchIndex() {
        when(client.prepareIndex("someIndex", "auditMessage"))
                .thenThrow(new IndexNotFoundException("someIndex"));

        ElasticsearchAuditor auditor = new ElasticsearchAuditor(client, "someIndex", "foobar");

        auditor.error("Here is my error");
    }

    private void givenClientPersistsSuccessfully() {
        IndexRequestBuilder indexRequestBuilder = mock(IndexRequestBuilder.class);
        when(indexRequestBuilder.setSource(jsonCaptor.capture())).thenReturn(indexRequestBuilder);
        when(indexRequestBuilder.execute()).thenReturn(indexResponse);
        when(client.prepareIndex(indexCaptor.capture(), eq("auditMessage")))
                .thenReturn(indexRequestBuilder);
        when(client.prepareIndex(indexCaptor.capture(), eq("auditActivity")))
                .thenReturn(indexRequestBuilder);
    }

    private AuditMessage parseAuditMessage() {
        try {
            String json = jsonCaptor.getValue().string();
            json = json.replace("@timestamp", "timestamp");
            return new ObjectMapper().readValue(json, AuditMessage.class);
        } catch (IOException e) {
            return new AuditMessage();
        }
    }

    private AuditActivity parseAuditActivity() {
        try {
            String json = jsonCaptor.getValue().string();
            json = json.replace("@timestamp", "timestamp");
            return new ObjectMapper().readValue(json, AuditActivity.class);
        } catch (IOException e) {
            return new AuditActivity();
        }
    }
}
