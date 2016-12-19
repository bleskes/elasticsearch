/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for reading state from the native process.
 */
public class StateProcessorTests extends ESTestCase {

    private static final String STATE_SAMPLE = "first header\n"
            + "first data\n"
            + "\0"
            + "second header\n"
            + "second data\n"
            + "\0"
            + "third header\n"
            + "third data\n"
            + "\0";

    public void testStateRead() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(STATE_SAMPLE.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<BytesReference> bytesRefCaptor = ArgumentCaptor.forClass(BytesReference.class);
        JobResultsPersister persister = Mockito.mock(JobResultsPersister.class);

        StateProcessor stateParser = new StateProcessor(Settings.EMPTY, persister);
        stateParser.process("_id", stream);

        verify(persister, times(3)).persistBulkState(eq("_id"), bytesRefCaptor.capture());

        String[] threeStates = STATE_SAMPLE.split("\0");
        List<BytesReference> capturedBytes = bytesRefCaptor.getAllValues();
        assertEquals(threeStates[0], capturedBytes.get(0).utf8ToString());
        assertEquals(threeStates[1], capturedBytes.get(1).utf8ToString());
        assertEquals(threeStates[2], capturedBytes.get(2).utf8ToString());
    }
}