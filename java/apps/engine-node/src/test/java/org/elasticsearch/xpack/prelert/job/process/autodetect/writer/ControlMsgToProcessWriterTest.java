
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;

public class ControlMsgToProcessWriterTest extends ESTestCase {
    @Mock
    private LengthEncodedWriter lengthEncodedWriter;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }

    public void testWriteCalcInterimMessage_GivenAdvanceTime() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);
        InterimResultsParams interimResultsParams = InterimResultsParams.newBuilder()
                .advanceTime(1234567890L).build();

        writer.writeCalcInterimMessage(interimResultsParams);

        InOrder inOrder = inOrder(lengthEncodedWriter);
        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("t1234567890");
        verifyNoMoreInteractions(lengthEncodedWriter);
    }

    public void testWriteCalcInterimMessage_GivenCalcInterimResultsWithNoTimeParams() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);
        InterimResultsParams interimResultsParams = InterimResultsParams.newBuilder()
                .calcInterim(true).build();

        writer.writeCalcInterimMessage(interimResultsParams);

        InOrder inOrder = inOrder(lengthEncodedWriter);
        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("i");
        verifyNoMoreInteractions(lengthEncodedWriter);
    }

    public void testWriteCalcInterimMessage_GivenNeitherCalcInterimNorAdvanceTime() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);
        InterimResultsParams interimResultsParams = InterimResultsParams.newBuilder().build();

        writer.writeCalcInterimMessage(interimResultsParams);

        verifyNoMoreInteractions(lengthEncodedWriter);
    }

    public void testWriteCalcInterimMessage_GivenCalcInterimResultsWithTimeParams() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);
        InterimResultsParams interimResultsParams = InterimResultsParams.newBuilder()
                .calcInterim(true).forTimeRange(120L, 180L).build();

        writer.writeCalcInterimMessage(interimResultsParams);

        InOrder inOrder = inOrder(lengthEncodedWriter);
        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("i120 180");
        verifyNoMoreInteractions(lengthEncodedWriter);
    }

    public void testWriteCalcInterimMessage_GivenCalcInterimAndAdvanceTime() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);
        InterimResultsParams interimResultsParams = InterimResultsParams.newBuilder()
                .calcInterim(true).forTimeRange(50L, 100L).advanceTime(180L).build();

        writer.writeCalcInterimMessage(interimResultsParams);

        InOrder inOrder = inOrder(lengthEncodedWriter);
        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("t180");
        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("i50 100");
        verifyNoMoreInteractions(lengthEncodedWriter);
    }

    public void testWriteFlushMessage() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);
        long firstId = Long.parseLong(writer.writeFlushMessage());
        Mockito.reset(lengthEncodedWriter);

        writer.writeFlushMessage();

        InOrder inOrder = inOrder(lengthEncodedWriter);

        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("f" + (firstId + 1));

        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        StringBuilder spaces = new StringBuilder();
        IntStream.rangeClosed(1, 8192).forEach(i -> spaces.append(' '));
        inOrder.verify(lengthEncodedWriter).writeField(spaces.toString());

        inOrder.verify(lengthEncodedWriter).flush();
        verifyNoMoreInteractions(lengthEncodedWriter);
    }

    public void testWriteResetBucketsMessage() throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, 2);

        writer.writeResetBucketsMessage(new DataLoadParams(new TimeRange(0L, 600L)));

        InOrder inOrder = inOrder(lengthEncodedWriter);
        inOrder.verify(lengthEncodedWriter).writeNumFields(4);
        inOrder.verify(lengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(lengthEncodedWriter).writeField("r0 600");
        verifyNoMoreInteractions(lengthEncodedWriter);
    }
}
