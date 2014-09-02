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

package org.elasticsearch.shield.authc.support;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.authc.AuthenticationException;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.transport.TransportRequest;
import org.junit.Test;

import static org.elasticsearch.shield.test.ShieldAssertions.assertContainsWWWAuthenticateHeader;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class UsernamePasswordTokenTests extends ElasticsearchTestCase {

    @Test
    public void testPutToken() throws Exception {
        TransportRequest request = new TransportRequest() {};
        UsernamePasswordToken.putTokenHeader(request, new UsernamePasswordToken("user1", "test123".toCharArray()));
        String header = request.getHeader(UsernamePasswordToken.BASIC_AUTH_HEADER);
        assertThat(header, notNullValue());
        assertTrue(header.startsWith("Basic "));
        String token = header.substring("Basic ".length());
        token = new String(Base64.decodeBase64(token), Charsets.UTF_8);
        int i = token.indexOf(":");
        assertTrue(i > 0);
        String username = token.substring(0, i);
        String password = token.substring(i + 1);
        assertThat(username, equalTo("user1"));
        assertThat(password, equalTo("test123"));
    }

    @Test
    public void testExtractToken() throws Exception {
        TransportRequest request = new TransportRequest() {};
        String header = "Basic " + new String(Base64.encodeBase64("user1:test123".getBytes(Charsets.UTF_8)), Charsets.UTF_8);
        request.putHeader(UsernamePasswordToken.BASIC_AUTH_HEADER, header);
        UsernamePasswordToken token = UsernamePasswordToken.extractToken(request, null);
        assertThat(token, notNullValue());
        assertThat(token.principal(), equalTo("user1"));
        assertThat(new String(token.credentials()), equalTo("test123"));

        // making sure that indeed, once resolved the instance is reused across multiple resolve calls
        UsernamePasswordToken token2 = UsernamePasswordToken.extractToken(request, null);
        assertThat(token, is(token2));
    }

    @Test
    public void testExtractToken_Invalid() throws Exception {
        String[] invalidValues = { "Basic", "Basic ", "Basic f" };
        for (String value : invalidValues) {
            TransportRequest request = new TransportRequest() {};
            request.putHeader(UsernamePasswordToken.BASIC_AUTH_HEADER, value);
            try {
                UsernamePasswordToken.extractToken(request, null);
                fail("Expected an authentication exception for invalid basic auth token [" + value + "]");
            } catch (AuthenticationException ae) {
                // expected
            }
        }
    }

    @Test
    public void testThatAuthorizationExceptionContainsResponseHeaders() {
        TransportRequest request = new TransportRequest() {};
        String header = "BasicBroken";
        request.putHeader(UsernamePasswordToken.BASIC_AUTH_HEADER, header);
        try {
            UsernamePasswordToken.extractToken(request, null);
            fail("Expected exception but did not happen");
        } catch (AuthenticationException e) {
            assertContainsWWWAuthenticateHeader(e);
        }
    }

    @Test
    public void testHasToken() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(request.header(UsernamePasswordToken.BASIC_AUTH_HEADER)).thenReturn("Basic foobar");
        assertThat(UsernamePasswordToken.hasToken(request), is(true));
    }

    @Test
    public void testHasToken_Missing() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(request.header(UsernamePasswordToken.BASIC_AUTH_HEADER)).thenReturn(null);
        assertThat(UsernamePasswordToken.hasToken(request), is(false));
    }

    @Test
    public void testHasToken_WithInvalidToken() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(request.header(UsernamePasswordToken.BASIC_AUTH_HEADER)).thenReturn("invalid");
        assertThat(UsernamePasswordToken.hasToken(request), is(false));
        when(request.header(UsernamePasswordToken.BASIC_AUTH_HEADER)).thenReturn("Basic");
        assertThat(UsernamePasswordToken.hasToken(request), is(false));
    }
}
