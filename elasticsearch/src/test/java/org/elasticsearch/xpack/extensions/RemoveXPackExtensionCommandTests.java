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

package org.elasticsearch.xpack.extensions;

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.cli.MockTerminal;
import org.elasticsearch.cli.UserException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@LuceneTestCase.SuppressFileSystems("*")
public class RemoveXPackExtensionCommandTests extends ESTestCase {

    private Path home;
    private Environment env;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        home = createTempDir();
        env = new Environment(Settings.builder().put("path.home", home.toString()).build());
    }

    Path createExtensionDir(Environment env) throws IOException {
        Path path = env.pluginsFile().resolve("x-pack").resolve("extensions");
        return Files.createDirectories(path);
    }

    static MockTerminal removeExtension(String name, Path home) throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("path.home", home.toString());
        MockTerminal terminal = new MockTerminal();
        new RemoveXPackExtensionCommand().execute(terminal, name, settings);
        return terminal;
    }

    static void assertRemoveCleaned(Path extDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extDir)) {
            for (Path file : stream) {
                if (file.getFileName().toString().startsWith(".removing")) {
                    fail("Removal dir still exists, " + file);
                }
            }
        }
    }

    public void testMissing() throws Exception {
        Path extDir = createExtensionDir(env);
        UserException e = expectThrows(UserException.class, () -> removeExtension("dne", home));
        assertTrue(e.getMessage(), e.getMessage().contains("Extension dne not found"));
        assertRemoveCleaned(extDir);
    }

    public void testBasic() throws Exception {
        Path extDir = createExtensionDir(env);
        Files.createDirectory(extDir.resolve("fake"));
        Files.createFile(extDir.resolve("fake").resolve("extension.jar"));
        Files.createDirectory(extDir.resolve("fake").resolve("subdir"));
        Files.createDirectory(extDir.resolve("other"));
        removeExtension("fake", home);
        assertFalse(Files.exists(extDir.resolve("fake")));
        assertTrue(Files.exists(extDir.resolve("other")));
        assertRemoveCleaned(extDir);
    }

}
