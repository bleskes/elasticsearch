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
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.test.ESTestCase;

import java.util.function.Supplier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link HttpResource}.
 */
public class HttpResourceTests extends ESTestCase {

    private final String owner = getTestName();
    private final RestClient client = mock(RestClient.class);

    public void testConstructorRequiresOwner() {
        expectThrows(NullPointerException.class, () -> new HttpResource(null) {
            @Override
            protected boolean doCheckAndPublish(RestClient client) {
                return false;
            }
        });
    }

    public void testConstructor() {
        final HttpResource resource = new HttpResource(owner) {
            @Override
            protected boolean doCheckAndPublish(RestClient client) {
                return false;
            }
        };

        assertSame(owner, resource.resourceOwnerName);
        assertTrue(resource.isDirty());
    }

    public void testConstructorDirtiness() {
        final boolean dirty = randomBoolean();
        final HttpResource resource = new HttpResource(owner, dirty) {
            @Override
            protected boolean doCheckAndPublish(RestClient client) {
                return false;
            }
        };

        assertSame(owner, resource.resourceOwnerName);
        assertEquals(dirty, resource.isDirty());
    }

    public void testDirtiness() {
        // MockHttpResponse always succeeds for checkAndPublish
        final HttpResource resource = new MockHttpResource(owner);

        assertTrue(resource.isDirty());

        resource.markDirty();

        assertTrue(resource.isDirty());

        // if this fails, then the mocked resource needs to be fixed
        assertTrue(resource.checkAndPublish(client));

        assertFalse(resource.isDirty());
    }

    public void testCheckAndPublish() {
        final boolean expected = randomBoolean();
        // the default dirtiness should be irrelevant; it should always be run!
        final HttpResource resource = new HttpResource(owner) {
            @Override
            protected boolean doCheckAndPublish(final RestClient client) {
            return expected;
        }
        };

        assertEquals(expected, resource.checkAndPublish(client));
    }

    public void testCheckAndPublishEvenWhenDirty() {
        final Supplier<Boolean> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(true, false);

        final HttpResource resource = new HttpResource(owner) {
            @Override
            protected boolean doCheckAndPublish(final RestClient client) {
                return supplier.get();
            }
        };

        assertTrue(resource.isDirty());
        assertTrue(resource.checkAndPublish(client));
        assertFalse(resource.isDirty());
        assertFalse(resource.checkAndPublish(client));

        verify(supplier, times(2)).get();
    }

    public void testCheckAndPublishIfDirty() {
        @SuppressWarnings("unchecked")
        final Supplier<Boolean> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(true, false);

        final HttpResource resource = new HttpResource(owner) {
            @Override
            protected boolean doCheckAndPublish(final RestClient client) {
                return supplier.get();
            }
        };

        assertTrue(resource.isDirty());
        assertTrue(resource.checkAndPublishIfDirty(client));
        assertFalse(resource.isDirty());
        assertTrue(resource.checkAndPublishIfDirty(client));

        // once is the default!
        verify(supplier).get();
    }

}
