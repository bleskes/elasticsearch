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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.elasticsearch.cli.Command;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.UserError;
import org.elasticsearch.cli.Terminal;

import static org.elasticsearch.license.core.CryptUtils.writeEncryptedPrivateKey;
import static org.elasticsearch.license.core.CryptUtils.writeEncryptedPublicKey;

public class KeyPairGeneratorTool extends Command {

    private final OptionSpec<File> publicKeyPathOption;
    private final OptionSpec<File> privateKeyPathOption;

    public KeyPairGeneratorTool() {
        super("Generates a key pair with RSA 2048-bit security");
        // TODO: in jopt-simple 5.0 we can use a PathConverter to take Path instead of File
        this.publicKeyPathOption = parser.accepts("publicKeyPath", "public key path")
            .withRequiredArg().ofType(File.class).required();
        this.privateKeyPathOption = parser.accepts("privateKeyPath", "private key path")
            .withRequiredArg().ofType(File.class).required();
    }

    public static void main(String[] args) throws Exception {
        exit(new KeyPairGeneratorTool().main(args, Terminal.DEFAULT));
    }

    @Override
    protected void printAdditionalHelp(Terminal terminal) {
        terminal.println("This tool generates and saves a key pair to the provided publicKeyPath");
        terminal.println("and privateKeyPath. The tool checks the existence of the provided key paths");
        terminal.println("and will not override if any existing keys are found.");
        terminal.println("");
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options) throws Exception {
        File publicKeyPath = publicKeyPathOption.value(options);
        File privateKeyPath = privateKeyPathOption.value(options);
        execute(terminal, publicKeyPath.toPath(), privateKeyPath.toPath());
    }

    // pkg private for tests
    void execute(Terminal terminal, Path publicKeyPath, Path privateKeyPath) throws Exception {
        if (Files.exists(privateKeyPath)) {
            throw new UserError(ExitCodes.USAGE, privateKeyPath + " already exists");
        } else if (Files.exists(publicKeyPath)) {
            throw new UserError(ExitCodes.USAGE, publicKeyPath + " already exists");
        }

        SecureRandom random = new SecureRandom();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, random);
        KeyPair keyPair = keyGen.generateKeyPair();

        Files.write(privateKeyPath, writeEncryptedPrivateKey(keyPair.getPrivate()));
        Files.write(publicKeyPath, writeEncryptedPublicKey(keyPair.getPublic()));

        terminal.println(Terminal.Verbosity.VERBOSE, "generating key pair [public key: " + publicKeyPath + ", private key: "
            + privateKeyPath + "]");
    }
}
