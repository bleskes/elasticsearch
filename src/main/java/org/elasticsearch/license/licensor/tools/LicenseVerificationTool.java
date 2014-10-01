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

package org.elasticsearch.license.licensor.tools;

import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicenseUtils;
import org.elasticsearch.license.manager.ESLicenseManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LicenseVerificationTool {

    static class Options {
        private final Set<File> licensesFiles;
        private final String publicKeyFilePath;
        private final String keyPass;

        Options(Set<File> licensesFiles, String publicKeyFilePath, String keyPass) {
            this.licensesFiles = licensesFiles;
            this.publicKeyFilePath = publicKeyFilePath;
            this.keyPass = keyPass;
        }

        static Set<File> asFiles(Set<String> filePaths) {
            Set<File> files = new HashSet<>(filePaths.size());
            for (String filePath : filePaths) {
                final File file = new File(filePath);
                if (file.exists()) {
                    files.add(file);
                } else {
                    throw new IllegalArgumentException(file.getAbsolutePath() + " does not exist!");
                }
            }
            return files;
        }
    }

    private static Options parse(String[] args) {
        Set<String> licenseFilePaths = null;
        Set<File> licenseFiles = null;
        String publicKeyPath = null;
        String keyPass = null;

        for (int i = 0; i < args.length; i++) {
            String command = args[i];
            switch (command) {
                case "--licensesFiles":
                    licenseFilePaths = new HashSet<>();
                    licenseFilePaths.addAll(Arrays.asList(args[++i].split(":")));
                    break;
                case "--publicKeyPath":
                    publicKeyPath = args[++i];
                    break;
                case "--keyPass":
                    keyPass = args[++i];
                    break;
            }
        }
        if (licenseFilePaths == null) {
            throw new IllegalArgumentException("mandatory option '--licensesFiles' is missing");
        } else {
            licenseFiles = Options.asFiles(licenseFilePaths);
            if (licenseFiles.size() == 0) {
                throw new IllegalArgumentException("no license file found for provided license files");
            }
        }
        if (publicKeyPath == null) {
            throw new IllegalArgumentException("mandatory option '--publicKeyPath' is missing");
        }
        if (keyPass == null) {
            throw new IllegalArgumentException("mandatory option '--keyPass' is missing");
        }
        return new Options(licenseFiles, publicKeyPath, keyPass);
    }

    public static void main(String[] args) throws IOException {
        run(args, System.out);
    }

    public static void run(String[] args, OutputStream out) throws IOException {
        Options options = parse(args);

        // read licenses
        Set<ESLicenses> esLicensesSet = LicenseUtils.readLicensesFromFiles(options.licensesFiles);

        // verify licenses
        ESLicenseManager licenseManager = new ESLicenseManager(esLicensesSet, options.publicKeyFilePath, options.keyPass);
        licenseManager.verifyLicenses();

        // dump effective licences
        LicenseUtils.dumpLicenseAsJson(licenseManager.getEffectiveLicenses(), out);
    }

}
