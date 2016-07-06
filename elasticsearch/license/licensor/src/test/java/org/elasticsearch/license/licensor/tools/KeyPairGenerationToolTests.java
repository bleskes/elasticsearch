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

import java.nio.file.Files;
import java.nio.file.Path;

import org.elasticsearch.cli.Command;
import org.elasticsearch.cli.CommandTestCase;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.UserException;

import static org.hamcrest.CoreMatchers.containsString;

public class KeyPairGenerationToolTests extends CommandTestCase {

    @Override
    protected Command newCommand() {
        return new KeyPairGeneratorTool();
    }

    public void testMissingKeyPaths() throws Exception {
        Path exists = createTempFile("", "existing");
        Path dne = createTempDir().resolve("dne");
        UserException e = expectThrows(UserException.class, () -> {
            execute("--publicKeyPath", exists.toString(), "--privateKeyPath", dne.toString());
        });
        assertThat(e.getMessage(), containsString("existing"));
        assertEquals(ExitCodes.USAGE, e.exitCode);
        e = expectThrows(UserException.class, () -> {
            execute("--publicKeyPath", dne.toString(), "--privateKeyPath", exists.toString());
        });
        assertThat(e.getMessage(), containsString("existing"));
        assertEquals(ExitCodes.USAGE, e.exitCode);
    }

    public void testTool() throws Exception {
        Path keysDir = createTempDir();
        Path publicKeyFilePath = keysDir.resolve("public");
        Path privateKeyFilePath = keysDir.resolve("private");

        execute("--publicKeyPath", publicKeyFilePath.toString(), "--privateKeyPath", privateKeyFilePath.toString());
        assertTrue(publicKeyFilePath.toString(), Files.exists(publicKeyFilePath));
        assertTrue(privateKeyFilePath.toString(), Files.exists(privateKeyFilePath));
    }
}
