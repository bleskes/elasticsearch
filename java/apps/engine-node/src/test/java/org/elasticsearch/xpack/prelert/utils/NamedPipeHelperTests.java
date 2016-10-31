/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (C) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained therein, is the
 * exclusive property of Elasticsearch BV and its licensors, if any, and is
 * protected under applicable domestic and foreign law, and international
 * treaties. Reproduction, republication or distribution without the express
 * written consent of Elasticsearch BV is strictly prohibited.
 */

package org.elasticsearch.xpack.prelert.utils;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;


/**
 * Only negative test cases are covered, as positive tests would need to create named pipes,
 * and this is not possible in Java with the Elasticsearch security manager configuration.
 */
public class NamedPipeHelperTests extends ESTestCase {

    public void testOpenForInputGivenPipeDoesNotExist() {
        Environment env = new Environment(
                Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build());
        IOException ioe = ESTestCase.expectThrows(NoSuchFileException.class,
                () ->
        NamedPipeHelper.openNamedPipeInputStream(env,
                env.tmpFile().resolve(NamedPipeHelper.getDefaultPipeDirectoryPrefix() + "this pipe does not exist"),
                Duration.ofSeconds(1)));

        assertTrue(ioe.getMessage(),
                ioe.getMessage().contains("pipe does not exist") ||
                ioe.getMessage().contains("The system cannot find the file specified"));
    }

    public void testOpenForOutputGivenPipeDoesNotExist() {
        Environment env = new Environment(
                Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build());
        IOException ioe = ESTestCase.expectThrows(FileNotFoundException.class, () ->
        NamedPipeHelper.openNamedPipeOutputStream(env,
                env.tmpFile().resolve(NamedPipeHelper.getDefaultPipeDirectoryPrefix() + "this pipe does not exist"),
                Duration.ofSeconds(1)));

        assertTrue(ioe.getMessage(), ioe.getMessage().contains("No such file or directory") ||
                ioe.getMessage().contains("The system cannot find the file specified"));
    }

    public void testOpenForInputGivenPipeIsRegularFile() throws IOException {
        Environment env = new Environment(
                Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build());
        Path tempFile = Files.createTempFile(env.tmpFile(), "not a named pipe", null);

        IOException ioe = ESTestCase.expectThrows(IOException.class, () ->
        NamedPipeHelper.openNamedPipeInputStream(tempFile, Duration.ofSeconds(1)));

        assertTrue(ioe.getMessage(), ioe.getMessage().contains("is not a named pipe"));

        assertTrue(Files.deleteIfExists(tempFile));
    }

    public void testOpenForOutputGivenPipeIsRegularFile() throws IOException {
        Environment env = new Environment(
                Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build());
        Path tempFile = Files.createTempFile(env.tmpFile(), "not a named pipe", null);

        IOException ioe = ESTestCase.expectThrows(IOException.class, () ->
        NamedPipeHelper.openNamedPipeOutputStream(env, tempFile, Duration.ofSeconds(1)));

        assertTrue(ioe.getMessage(), ioe.getMessage().contains("is not a named pipe") ||
                ioe.getMessage().contains("The system cannot find the file specified"));

        assertTrue(Files.deleteIfExists(tempFile));
    }
}
