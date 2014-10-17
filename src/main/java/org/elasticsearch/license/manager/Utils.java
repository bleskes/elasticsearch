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
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicenseBuilders;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

import static org.elasticsearch.license.core.ESLicenses.ESLicense;

public class Utils {



    public static ESLicense getESLicenseFromSignature(String signature) {
        byte[] signatureBytes = Base64.decodeBase64(signature);
        ByteBuffer byteBuffer = ByteBuffer.wrap(signatureBytes);
        byteBuffer = (ByteBuffer) byteBuffer.position(13);
        int start = byteBuffer.getInt();
        SignedLicense signedLicense = new ObjectSerializer()
                .readObject(SignedLicense.class, Arrays.copyOfRange(signatureBytes, start, signatureBytes.length));
        return ESLicenseManager.getInstance().decryptAndVerifyESLicense(signedLicense, signature);
    }

    public static boolean isSame(ESLicenses firstLicenses, ESLicenses secondLicenses) {

        // we do the build to make sure we weed out any expired licenses
        final ESLicenses licenses1 = LicenseBuilders.licensesBuilder().licenses(firstLicenses).build();
        final ESLicenses licenses2 = LicenseBuilders.licensesBuilder().licenses(secondLicenses).build();

        // check if the effective licenses have the same feature set
        if (!licenses1.features().equals(licenses2.features())) {
            return false;
        }

        // for every feature license, check if all the attributes are the same
        for (ESLicenses.FeatureType featureType : licenses1.features()) {
            ESLicense license1 = licenses1.get(featureType);
            ESLicense license2 = licenses2.get(featureType);

            if (!license1.uid().equals(license2.uid())
                    || license1.feature() != license2.feature()
                    || license1.subscriptionType() != license2.subscriptionType()
                    || license1.type() != license2.type()
                    || license1.expiryDate() != license2.expiryDate()
                    || license1.issueDate() != license2.issueDate()
                    || !license1.issuedTo().equals(license2.issuedTo())
                    || license1.maxNodes() != license2.maxNodes()
                    || license1.signature().equals(license2.signature())) {
                return false;
            }
        }
        return true;
    }

    public static ESLicenses fromSignatures(final Set<String> signatures) {
        final LicenseBuilders.LicensesBuilder licensesBuilder = LicenseBuilders.licensesBuilder();
        for (String signature : signatures) {
            licensesBuilder.license(getESLicenseFromSignature(signature));
        }
        return licensesBuilder.build();
    }
}
