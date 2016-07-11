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

package org.elasticsearch.xpack.common.secret;

import java.io.IOException;
import java.util.Arrays;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.security.crypto.CryptoService;

/**
 *
 */
public class Secret implements ToXContent {

    protected final char[] text;

    public Secret(char[] text) {
        this.text = text;
    }

    public char[] text(CryptoService service) {
        if (service == null) {
            return text;
        }
        return service.decrypt(text);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(new String(text));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Secret secret = (Secret) o;

        return Arrays.equals(text, secret.text);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(text);
    }

}
