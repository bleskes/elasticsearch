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

import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

/**
 *
 */
public class HasherTests extends ElasticsearchTestCase {

    @Test
    public void testHtpasswd_ToolGenerated() throws Exception {
        Hasher hasher = Hasher.HTPASSWD;
        SecuredString passwd = SecuredStringTests.build("test123");
        assertTrue(hasher.verify(passwd, "$2a$05$zxnP0vdREMxnEpkLCDI2OuSaSk/QEKA2.A42iOpI6U2u.RLLOWm1e".toCharArray()));
        assertTrue(hasher.verify(passwd, "$2a$10$vNMk6GyVUU./7YSZB6BGPuozm921GVPw/Pdukzd09s.sL2rIWROU6".toCharArray()));
        assertTrue(hasher.verify(passwd, "$apr1$R3DdqiAZ$aljIkaIVPSarmDMlJUBBP.".toCharArray()));
        if (!Hasher.CRYPT_SUPPORTED) {
            assertTrue(hasher.verify(passwd, "test123".toCharArray()));
        } else {
            assertTrue(hasher.verify(passwd, "hsP1PYSLsEEvs".toCharArray()));
        }
        assertTrue(hasher.verify(passwd, "{plain}test123".toCharArray()));
        assertTrue(hasher.verify(passwd, "{SHA}cojt0Pw//L6ToM8G41aOKFIWh7w=".toCharArray()));
        assertTrue(hasher.verify(passwd, "$5$RsqcsPiF$51tIIXf6oZb3Awox6FWNhITVlM/aW3oa8uN2eptIf54".toCharArray()));
    }

    @Test
    public void testHtpasswd_SelfGenerated() throws Exception {
        testHasherSelfGenerated(Hasher.HTPASSWD);
    }

    @Test
    public void testBcrypt_SelfGenerated() throws Exception {
        testHasherSelfGenerated(Hasher.BCRYPT);
    }

    @Test
    public void testMd5_SelfGenerated() throws Exception {
        testHasherSelfGenerated(Hasher.MD5);
    }

    @Test
    public void testSha1_SelfGenerated() throws Exception {
        testHasherSelfGenerated(Hasher.SHA1);
    }

    @Test
    public void testSha2_SelfGenerated() throws Exception {
        testHasherSelfGenerated(Hasher.SHA2);
    }

    public void testHasherSelfGenerated(Hasher hasher) throws Exception {
        SecuredString passwd = SecuredStringTests.build("test123");
        assertTrue(hasher.verify(passwd, hasher.hash(passwd)));
    }

}
