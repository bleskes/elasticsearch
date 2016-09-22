
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DetectorVerifierInvalidFieldNameTest {
    private static final String SUFFIX = "suffix";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Detector detector;
    private String character;
    private boolean isValid;

    @Parameters
    public static Collection<Object[]> getCharactersAndValidity() {
        return Arrays.asList(new Object[][]{
                // char, isValid?
                {"a", true},
                {"[", true},
                {"]", true},
                {"(", true},
                {")", true},
                {"=", true},
                {"-", true},
                {" ", true},
                {"\"", false},
                {"\\", false},
                {"\t", false},
                {"\n", false},
        });
    }

    public DetectorVerifierInvalidFieldNameTest(String character, boolean isValid) {
        this.character = character;
        this.isValid = isValid;
    }

    @Before
    public void setUp() {
        detector = createDetectorWithValidFieldNames();
    }

    @Test
    public void testVerify_FieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setFieldName(detector.getFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }

    @Test
    public void testVerify_ByFieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setByFieldName(detector.getByFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }

    @Test
    public void testVerify_OverFieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setOverFieldName(detector.getOverFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }

    @Test
    public void testVerify_PartitionFieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setPartitionFieldName(detector.getPartitionFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }

    @Test
    public void testVerify_FieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setFieldName(detector.getFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }

    @Test
    public void testVerify_ByFieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setByFieldName(detector.getByFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }

    @Test
    public void testVerify_OverFieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setOverFieldName(detector.getOverFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }

    @Test
    public void testVerify_PartitionFieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setPartitionFieldName(detector.getPartitionFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }

    private String getCharacterPlusSuffix() {
        return character + SUFFIX;
    }

    private void expectJobConfigurationExceptionWhenCharIsInvalid(ErrorCodes errorCode) {
        if (!isValid) {
            expectJobConfigurationException(errorCode);
        }
    }

    private void expectJobConfigurationException(ErrorCodes errorCode) {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(errorCode));
    }

    private static Detector createDetectorWithValidFieldNames() {
        Detector d = new Detector();
        d.setFieldName("field");
        d.setByFieldName("by");
        d.setOverFieldName("over");
        d.setPartitionFieldName("partition");
        return d;
    }
}
