/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.security.authz.permission;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.hamcrest.Matchers.containsString;

public class FieldPermissionTests extends ESTestCase {

    public void testParseFieldPermissions() throws Exception {
        String q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": [\"f1\", \"f2\", \"f3\", \"f4\"]," +
                "\"except\": [\"f3\",\"f4\"]" +
                "}}]}";
        RoleDescriptor rd = RoleDescriptor.parse("test", new BytesArray(q), false);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[] { "f1", "f2", "f3", "f4" });
        assertArrayEquals(rd.getIndicesPrivileges()[0].getDeniedFields(), new String[] { "f3", "f4" });

        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"except\": [\"f3\",\"f4\"]," +
                "\"grant\": [\"f1\", \"f2\", \"f3\", \"f4\"]" +
                "}}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), false);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[] { "f1", "f2", "f3", "f4" });
        assertArrayEquals(rd.getIndicesPrivileges()[0].getDeniedFields(), new String[] { "f3", "f4" });

        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": [\"f1\", \"f2\"]" +
                "}}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), false);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[] { "f1", "f2" });
        assertNull(rd.getIndicesPrivileges()[0].getDeniedFields());

        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": []" +
                "}}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), false);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[] {});
        assertNull(rd.getIndicesPrivileges()[0].getDeniedFields());

        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"except\": []," +
                "\"grant\": []" +
                "}}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), false);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[] {});
        assertArrayEquals(rd.getIndicesPrivileges()[0].getDeniedFields(), new String[] {});

        final String exceptWithoutGrant = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"except\": [\"f1\"]" +
                "}}]}";
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (exceptWithoutGrant), false));
        assertThat(e.getDetailedMessage(), containsString("failed to parse indices privileges for role [test]. field_security requires " +
                "grant if except is given"));

        final String grantNull = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": null" +
                "}}]}";
        e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (grantNull), false));
        assertThat(e.getDetailedMessage(), containsString("failed to parse indices privileges for role [test]. grant must not be null."));

        final String exceptNull = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": [\"*\"]," +
                "\"except\": null" +
                "}}]}";
        e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (exceptNull), false));
        assertThat(e.getDetailedMessage(), containsString("failed to parse indices privileges for role [test]. except must not be null."));

        final String exceptGrantNull = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": null," +
                "\"except\": null" +
                "}}]}";
        e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (exceptGrantNull), false));
        assertThat(e.getDetailedMessage(), containsString("failed to parse indices privileges for role [test]. grant must not be null."));

        final String bothFieldsMissing = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "}}]}";
        e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (bothFieldsMissing), false));
        assertThat(e.getDetailedMessage(), containsString("failed to parse indices privileges for role [test]. \"field_security\" " +
                "must not be empty."));

        // try with two indices and mix order a little
        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"field_security\": {" +
                "\"grant\": []" +
                "}}," +
                "{\"names\": \"idx3\",\n" +
                " \"field_security\": {\n" +
                " \"grant\": [\"*\"], \n" +
                " \"except\": [\"f2\"]}," +
                "\"privileges\": [\"p3\"]}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), false);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[] {});
        assertNull(rd.getIndicesPrivileges()[0].getDeniedFields());
        assertArrayEquals(rd.getIndicesPrivileges()[1].getGrantedFields(), new String[] {"*"});
        assertArrayEquals(rd.getIndicesPrivileges()[1].getDeniedFields(), new String[] {"f2"});
    }

    // test old syntax for field permissions
    public void testBWCFieldPermissions() throws Exception {
        String q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"fields\": [\"f1\", \"f2\"]" +
                "}]}";
        RoleDescriptor rd = RoleDescriptor.parse("test", new BytesArray(q), true);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[]{"f1", "f2"});
        assertNull(rd.getIndicesPrivileges()[0].getDeniedFields());

        final String failingQuery = q;
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (failingQuery), false));
        assertThat(e.getDetailedMessage(), containsString("[\"fields\": [...]] format has changed for field permissions in role [test]" +
                ", use [\"field_security\": {\"grant\":[...],\"except\":[...]}] instead"));

        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"fields\": []" +
                "}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), true);
        assertArrayEquals(rd.getIndicesPrivileges()[0].getGrantedFields(), new String[]{});
        assertNull(rd.getIndicesPrivileges()[0].getDeniedFields());
        final String failingQuery2 = q;
        e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray
                (failingQuery2), false));
        assertThat(e.getDetailedMessage(), containsString("[\"fields\": [...]] format has changed for field permissions in role [test]" +
                ", use [\"field_security\": {\"grant\":[...],\"except\":[...]}] instead"));

        q = "{\"indices\": [ {\"names\": \"idx2\", \"privileges\": [\"p3\"], " +
                "\"fields\": null" +
                "}]}";
        rd = RoleDescriptor.parse("test", new BytesArray(q), true);
        assertNull(rd.getIndicesPrivileges()[0].getGrantedFields());
        assertNull(rd.getIndicesPrivileges()[0].getDeniedFields());
        final String failingQuery3 = q;
        e = expectThrows(ElasticsearchParseException.class, () -> RoleDescriptor.parse("test", new BytesArray(failingQuery3), false));
        assertThat(e.getDetailedMessage(), containsString("[\"fields\": [...]] format has changed for field permissions in role [test]" +
                ", use [\"field_security\": {\"grant\":[...],\"except\":[...]}] instead"));
    }

    public void testFieldPermissionsStreaming() throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        String[] allowed = new String[]{randomAsciiOfLength(5) + "*", randomAsciiOfLength(5) + "*", randomAsciiOfLength(5) + "*"};
        String[] denied = new String[]{allowed[0] + randomAsciiOfLength(5), allowed[1] + randomAsciiOfLength(5),
                allowed[2] + randomAsciiOfLength(5)};
        FieldPermissions fieldPermissions = new FieldPermissions(allowed, denied);
        out.writeOptionalWriteable(fieldPermissions);
        out.close();
        StreamInput in = out.bytes().streamInput();
        FieldPermissions readFieldPermissions = in.readOptionalWriteable(FieldPermissions::new);
        // order should be preserved in any case
        assertEquals(readFieldPermissions, fieldPermissions);
    }

    public void testFieldPermissionsHashCodeThreadSafe() throws Exception {
        final int numThreads = scaledRandomIntBetween(4, 16);
        final FieldPermissions fieldPermissions = new FieldPermissions(new String[] { "*" }, new String[] { "foo" });
        final CountDownLatch latch = new CountDownLatch(numThreads + 1);
        final AtomicReferenceArray<Integer> hashCodes = new AtomicReferenceArray<>(numThreads);
        List<Thread> threads = new ArrayList<>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            threads.add(new Thread(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                final int hashCode = fieldPermissions.hashCode();
                hashCodes.set(threadNum, hashCode);
            }));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        latch.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        final int hashCode = fieldPermissions.hashCode();
        for (int i = 0; i < numThreads; i++) {
            assertEquals((Integer) hashCode, hashCodes.get(i));
        }
    }
}
