package org.elasticsearch.shield.test;

import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.SecurityException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ShieldAssertions {

    public static void assertContainsWWWAuthenticateHeader(org.elasticsearch.shield.SecurityException e) {
        assertThat(e.status(), is(RestStatus.UNAUTHORIZED));
        assertThat(e.getHeaders(), hasKey("WWW-Authenticate"));
        assertThat(e.getHeaders().get("WWW-Authenticate"), hasSize(1));
        assertThat(e.getHeaders().get("WWW-Authenticate").get(0), is(SecurityException.HEADERS.get("WWW-Authenticate").get(0)));
    }
}
