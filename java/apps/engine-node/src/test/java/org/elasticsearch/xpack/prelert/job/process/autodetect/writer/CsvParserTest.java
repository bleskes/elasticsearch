package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.elasticsearch.test.ESTestCase;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import org.elasticsearch.xpack.prelert.job.DataDescription;

public class CsvParserTest extends ESTestCase {

    /**
     * Test parsing CSV with the NUL character code point (\0 or \u0000)
     *
     * @throws IOException
     */
    public void test() throws IOException {
        String data = "1422936876.262044869, 1422936876.262044869, 90, 2, 10.132.0.1, 0, 224.0.0.5, 0, 1, 1, 268435460, null, null, null, null, null, null, null, null, null, null, null\n"
                + "1422943772.875342698, 1422943772.875342698, 90, 2, 10.132.0.1, 0, 224.0.0.5, 0, 1, 1, 268435460,,,,,\0,\u0000,,,,,\u0000\n"
                + "\0";
        InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        CsvPreference csvPref = new CsvPreference.Builder(
                DataDescription.DEFAULT_QUOTE_CHAR,
                ',',
                new String(new char[]{DataDescription.LINE_ENDING})).build();

        try (CsvListReader csvReader = new CsvListReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                csvPref)) {
            String[] header = csvReader.getHeader(true);
            assertEquals(22, header.length);

            List<String> line = csvReader.read();
            assertEquals(22, line.size());

            // last line is \0
            line = csvReader.read();
            assertEquals(1, line.size());
        }
    }
}
