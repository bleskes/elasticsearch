/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.manager;

import net.nicholaswilliams.java.licensing.ObjectSerializer;
import net.nicholaswilliams.java.licensing.SignedLicense;
import org.apache.commons.codec.binary.Base64;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.license.core.ESLicense;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Utils {
    /**
     * Extract a signedLicense (SIGNED_LICENSE_CONTENT) from the signature.
     * Validates the public key used to decrypt the license by comparing their hashes
     * <p/>
     * Signature structure:
     * | MAGIC | HEADER_LENGTH | VERSION | PUB_KEY_DIGEST | SIGNED_LICENSE_CONTENT |
     *
     * @param signature of a single license
     * @return signed license content for the license
     */
    public static SignedLicense extractSignedLicence(String signature) {
        byte[] signatureBytes = Base64.decodeBase64(signature);
        ByteBuffer byteBuffer = ByteBuffer.wrap(signatureBytes);
        byteBuffer = (ByteBuffer) byteBuffer.position(13);
        int start = byteBuffer.getInt();
        int version = byteBuffer.getInt();
        return new ObjectSerializer().readObject(SignedLicense.class, Arrays.copyOfRange(signatureBytes, start, signatureBytes.length));
    }


    public static ImmutableMap<String, ESLicense> reduceAndMap(Set<ESLicense> esLicensesSet) {
        Map<String, ESLicense> map = new HashMap<>(esLicensesSet.size());
        for (ESLicense license : esLicensesSet) {
            putIfAppropriate(map, license);
        }
        return ImmutableMap.copyOf(map);
    }


    private static void putIfAppropriate(Map<String, ESLicense> licenseMap, ESLicense license) {
        final String featureType = license.feature();
        if (licenseMap.containsKey(featureType)) {
            final ESLicense previousLicense = licenseMap.get(featureType);
            if (license.expiryDate() > previousLicense.expiryDate()) {
                licenseMap.put(featureType, license);
            }
        } else if (license.expiryDate() > System.currentTimeMillis()) {
            licenseMap.put(featureType, license);
        }
    }
}
