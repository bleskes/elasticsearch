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

package org.elasticsearch.license.core;

import net.nicholaswilliams.java.licensing.encryption.PublicKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;
import org.elasticsearch.common.io.Streams;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class ResourcePublicKeyDataProvider implements PublicKeyDataProvider {

    private final String resource;

    public ResourcePublicKeyDataProvider(String resource) {
        this.resource = resource;
    }

    @Override
    public byte[] getEncryptedPublicKeyData() throws KeyNotFoundException {
        try(InputStream inputStream = this.getClass().getResourceAsStream(resource)) {
            return Streams.copyToByteArray(inputStream);
        } catch (IOException ex) {
            throw new KeyNotFoundException(ex);
        }
    }
}
