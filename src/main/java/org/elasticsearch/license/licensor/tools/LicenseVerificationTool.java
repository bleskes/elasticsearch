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

package org.elasticsearch.license.licensor.tools;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.manager.ESLicenseManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class LicenseVerificationTool {

    private static Set<ESLicense> parse(String[] args) throws IOException {
        Set<ESLicense> licenses = new HashSet<>();

        for (int i = 0; i < args.length; i++) {
            String command = args[i];
            switch (command) {
                case "--licensesFiles":
                    for (String filePath : args[++i].split(":")) {
                        File file = new File(filePath);
                        if (file.exists()) {
                            licenses.addAll(ESLicenses.fromSource(Files.readAllBytes(Paths.get(file.getAbsolutePath()))));
                        } else {
                            throw new IllegalArgumentException(file.getAbsolutePath() + " does not exist!");
                        }
                    }
                    break;
                case "--licenses":
                    licenses.addAll(ESLicenses.fromSource(args[++i]));
                    break;
            }
        }
        if (licenses.size() == 0) {
            throw new IllegalArgumentException("mandatory option '--licensesFiles' or '--licenses' is missing");
        }
        return licenses;
    }

    public static void main(String[] args) throws IOException {
        run(args, System.out);
    }

    public static void run(String[] args, OutputStream out) throws IOException {
        Set<ESLicense> licenses = parse(args);

        // reduce & verify licenses
        ImmutableMap<String, ESLicense> effectiveLicenses = ESLicenses.reduceAndMap(licenses);
        ESLicenseManager licenseManager = new ESLicenseManager();
        licenseManager.verifyLicenses(effectiveLicenses);

        // dump effective licences
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON, out);
        ESLicenses.toXContent(effectiveLicenses.values(), builder, ToXContent.EMPTY_PARAMS);
        builder.flush();
    }

}
