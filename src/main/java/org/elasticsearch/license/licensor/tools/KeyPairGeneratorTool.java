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

import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import net.nicholaswilliams.java.licensing.exception.AlgorithmNotSupportedException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeyException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeySpecificationException;
import net.nicholaswilliams.java.licensing.exception.RSA2048NotSupportedException;

import java.io.*;
import java.security.KeyPair;

public class KeyPairGeneratorTool {

    static class Options {
        private final String publicKeyFilePath;
        private final String privateKeyFilePath;
        private final String keyPass;

        Options(String publicKeyFilePath, String privateKeyFilePath, String keyPass) {
            this.publicKeyFilePath = publicKeyFilePath;
            this.privateKeyFilePath = privateKeyFilePath;
            this.keyPass = keyPass;
        }
    }

    private static Options parse(String[] args) {
        String privateKeyPath = null;
        String publicKeyPath = null;
        String keyPass = null;

        for (int i = 0; i < args.length; i++) {
            String command = args[i];
            switch (command) {
                case "--publicKeyPath":
                    publicKeyPath = args[++i];
                    break;
                case "--privateKeyPath":
                    privateKeyPath = args[++i];
                    break;
                case "--keyPass":
                    keyPass = args[++i];
                    break;
            }
        }

        if (publicKeyPath == null) {
            throw new IllegalArgumentException("mandatory option '--publicKeyPath' is missing");
        }
        if (privateKeyPath == null) {
            throw new IllegalArgumentException("mandatory option '--privateKeyPath' is missing");
        }
        if (keyPass == null) {
            throw new IllegalArgumentException("mandatory option '--keyPass' is missing");
        }

        return new Options(publicKeyPath, privateKeyPath, keyPass);
    }

    public static void main(String[] args) throws IOException {
        run(args, System.out);
    }

    public static void run(String[] args, OutputStream out) throws IOException {
        PrintStream printStream = new PrintStream(out);

        Options options = parse(args);

        if (exists(options.privateKeyFilePath)) {
            throw new IllegalArgumentException("private key already exists in " + options.privateKeyFilePath);
        } else if (exists(options.publicKeyFilePath)) {
            throw new IllegalArgumentException("public key already exists in " + options.publicKeyFilePath);
        }

        KeyPair keyPair = generateKeyPair(options.privateKeyFilePath, options.publicKeyFilePath, options.keyPass);
        if (keyPair != null) {
            printStream.println("Successfully generated new keyPair [publicKey: " + options.publicKeyFilePath + ", privateKey: " + options.privateKeyFilePath + "]");
            printStream.flush();
        }
    }

    private static boolean exists(String filePath) {
        return new File(filePath).exists();
    }


    private static KeyPair generateKeyPair(String privateKeyFileName, String publicKeyFileName, String password) {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

        KeyPair keyPair;
        try {
            keyPair = generator.generateKeyPair();
        } catch (RSA2048NotSupportedException e) {
            throw new IllegalStateException(e);
        }

        try {
            generator.saveKeyPairToFiles(keyPair, privateKeyFileName, publicKeyFileName, password.toCharArray());
        } catch (IOException | AlgorithmNotSupportedException | InappropriateKeyException | InappropriateKeySpecificationException e) {
            throw new IllegalStateException(e);
        }
        return keyPair;
    }
}
