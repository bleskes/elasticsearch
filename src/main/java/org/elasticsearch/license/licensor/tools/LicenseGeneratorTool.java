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

import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicensesCharset;
import org.elasticsearch.license.licensor.ESLicenseSigner;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

public class LicenseGeneratorTool {

    static class Options {
        private final Set<ESLicense> licenseSpecs;
        private final String publicKeyFilePath;
        private final String privateKeyFilePath;

        Options(Set<ESLicense> licenseSpecs, String publicKeyFilePath, String privateKeyFilePath) {
            this.licenseSpecs = licenseSpecs;
            this.publicKeyFilePath = publicKeyFilePath;
            this.privateKeyFilePath = privateKeyFilePath;
        }
    }

    private static Options parse(String[] args) throws IOException, ParseException {
        Set<ESLicense> licenseSpecs = new HashSet<>();
        String privateKeyPath = null;
        String publicKeyPath = null;

        for (int i = 0; i < args.length; i++) {
            String command = args[i].trim();
            switch (command) {
                case "--license":
                    String licenseInput = args[++i];
                    licenseSpecs.addAll(ESLicenses.fromSource(licenseInput.getBytes(LicensesCharset.UTF_8), false));
                    break;
                case "--licenseFile":
                    File licenseFile = new File(args[++i]);
                    if (licenseFile.exists()) {
                        final byte[] bytes = Files.readAllBytes(Paths.get(licenseFile.getAbsolutePath()));
                        licenseSpecs.addAll(ESLicenses.fromSource(bytes, false));
                    } else {
                        throw new IllegalArgumentException(licenseFile.getAbsolutePath() + " does not exist!");
                    }
                    break;
                case "--publicKeyPath":
                    publicKeyPath = args[++i];
                    break;
                case "--privateKeyPath":
                    privateKeyPath = args[++i];
                    break;
            }
        }

        if (licenseSpecs.size() == 0) {
            throw new IllegalArgumentException("at least one of '--license' or '--licenseFile' has to be provided");
        }
        if (publicKeyPath == null) {
            throw new IllegalArgumentException("mandatory option '--publicKeyPath' is missing");
        } else if (!Paths.get(publicKeyPath).toFile().exists()) {
            throw new IllegalArgumentException("Public key file: " + publicKeyPath + " does not exist!");
        }
        if (privateKeyPath == null) {
            throw new IllegalArgumentException("mandatory option '--privateKeyPath' is missing");
        } else if (!Paths.get(privateKeyPath).toFile().exists()) {
            throw new IllegalArgumentException("Private key file: " + privateKeyPath + " does not exist!");
        }

        return new Options(licenseSpecs, publicKeyPath, privateKeyPath);
    }

    public static void main(String[] args) throws IOException, ParseException {
        run(args, System.out);
    }

    public static void run(String[] args, OutputStream out) throws IOException, ParseException {
        Options options = parse(args);

        ESLicenseSigner signer = new ESLicenseSigner(options.privateKeyFilePath, options.publicKeyFilePath);
        ImmutableSet<ESLicense> signedLicences = signer.sign(options.licenseSpecs);

        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON, out);

        ESLicenses.toXContent(signedLicences, builder, ToXContent.EMPTY_PARAMS);

        builder.flush();
    }

}
