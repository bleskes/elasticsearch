/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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

import org.apache.commons.cli.MissingOptionException;
import org.elasticsearch.common.cli.CliToolTestCase;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.license.licensor.tools.KeyPairGeneratorTool.KeyGenerator;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.elasticsearch.common.cli.CliTool.Command;
import static org.elasticsearch.common.cli.CliTool.ExitStatus;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;

public class KeyPairGenerationToolTests extends CliToolTestCase {

    @Test
    public void testParsingMissingPath() throws Exception {
        KeyPairGeneratorTool keyPairGeneratorTool = new KeyPairGeneratorTool();
        Path tempFile = createTempFile();
        try {
            keyPairGeneratorTool.parse(KeyPairGeneratorTool.NAME, args(
                     "--privateKeyPath " + tempFile.toAbsolutePath()));
            fail("no public key path provided");
        } catch (MissingOptionException e) {
            assertThat(e.getMessage(), containsString("pub"));
        }
        try {
            keyPairGeneratorTool.parse(KeyPairGeneratorTool.NAME, args(
                    "--publicKeyPath " + tempFile.toAbsolutePath()));
            fail("no private key path provided");
        } catch (MissingOptionException e) {
            assertThat(e.getMessage(), containsString("pri"));
        }
    }

    @Test
    public void testParsingNeverOverrideKey() throws Exception {
        KeyPairGeneratorTool keyPairGeneratorTool = new KeyPairGeneratorTool();
        Path tempFile = createTempFile();
        Path tempFile2 = createTempFile();
        String nonExistentFilePath = tempFile2.toAbsolutePath().toString();
        Files.delete(tempFile2);
        assertThat(Files.exists(tempFile2), equalTo(false));

        Command command = keyPairGeneratorTool.parse(KeyPairGeneratorTool.NAME, new String[] {"--privateKeyPath", tempFile.toAbsolutePath().toString(),
                    "--publicKeyPath", nonExistentFilePath });

        assertThat(command, instanceOf(Command.Exit.class));
        Command.Exit exitCommand = (Command.Exit) command;
        assertThat(exitCommand.status(), equalTo(ExitStatus.USAGE));

        command = keyPairGeneratorTool.parse(KeyPairGeneratorTool.NAME, new String[] {"--publicKeyPath", tempFile.toAbsolutePath().toString(),
                "--privateKeyPath", nonExistentFilePath });

        assertThat(command, instanceOf(Command.Exit.class));
        exitCommand = (Command.Exit) command;
        assertThat(exitCommand.status(), equalTo(ExitStatus.USAGE));
    }

    @Test
    public void testToolSimple() throws Exception {
        KeyPairGeneratorTool keyPairGeneratorTool = new KeyPairGeneratorTool();
        Path publicKeyFilePath = createTempFile().toAbsolutePath();
        Path privateKeyFilePath = createTempFile().toAbsolutePath();
        Settings settings = Settings.builder().put("path.home", createTempDir("KeyPairGenerationToolTests")).build();

        Files.delete(publicKeyFilePath);
        Files.delete(privateKeyFilePath);
        assertThat(Files.exists(publicKeyFilePath), equalTo(false));
        assertThat(Files.exists(privateKeyFilePath), equalTo(false));

        Command command = keyPairGeneratorTool.parse(KeyPairGeneratorTool.NAME, new String[] { "--privateKeyPath", privateKeyFilePath.toString(),
                "--publicKeyPath", publicKeyFilePath.toString() });

        assertThat(command, instanceOf(KeyGenerator.class));
        KeyGenerator keyGenerator = (KeyGenerator) command;
        assertThat(keyGenerator.privateKeyPath, equalTo(privateKeyFilePath));
        assertThat(keyGenerator.publicKeyPath, equalTo(publicKeyFilePath));

        assertThat(Files.exists(publicKeyFilePath), equalTo(false));
        assertThat(Files.exists(privateKeyFilePath), equalTo(false));

        assertThat(keyGenerator.execute(settings, new Environment(settings)), equalTo(ExitStatus.OK));
        assertThat(Files.exists(publicKeyFilePath), equalTo(true));
        assertThat(Files.exists(privateKeyFilePath), equalTo(true));

        Files.delete(publicKeyFilePath);
        Files.delete(privateKeyFilePath);
    }
}
