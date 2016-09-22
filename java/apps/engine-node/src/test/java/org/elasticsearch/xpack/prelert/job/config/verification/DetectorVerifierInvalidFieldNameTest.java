
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;

public class DetectorVerifierInvalidFieldNameTest extends ESTestCase {
    private static final String SUFFIX = "suffix";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Detector detector;
    private String character;
    private boolean isValid;

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

//    @Before
//    public void createDetector() {
//        detector = createDetectorWithValidFieldNames();
//    }


    public void testVerifyFieldNames_givenInvalidChars() throws JobConfigurationException {

        Collection<Object[]> testCaseArguments = getCharactersAndValidity();

        for (Object [] args : testCaseArguments) {
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();

            setCharacter((String)args[0]);
            setIsValid((boolean)args[1]);

            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_FieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_ByFieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_OverFieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_PartitionFieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
        }
    }

    public void testVerifyFunction_forPreSummariedInput() throws JobConfigurationException {

        Collection<Object[]> testCaseArguments = getCharactersAndValidity();

        for (Object [] args : testCaseArguments) {
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();

            setCharacter((String)args[0]);
            setIsValid((boolean)args[1]);

            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_FieldNameGivenPresummarised();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_ByFieldNameGivenPresummarised();
            verify_OverFieldNameGivenPresummarised();
            verify_ByFieldNameGivenPresummarised();
            verify_PartitionFieldNameGivenPresummarised();
        }
    }

    public void verify_FieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setFieldName(detector.getFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }


    public void verify_ByFieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setByFieldName(detector.getByFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }


    public void verify_OverFieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setOverFieldName(detector.getOverFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }


    public void verify_PartitionFieldName() throws JobConfigurationException {
        expectJobConfigurationExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        detector.setPartitionFieldName(detector.getPartitionFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, false);
    }


    public void verify_FieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setFieldName(detector.getFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }


    public void verify_ByFieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setByFieldName(detector.getByFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }


    public void verify_OverFieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setOverFieldName(detector.getOverFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }


    public void verify_PartitionFieldNameGivenPresummarised() throws JobConfigurationException {
        expectJobConfigurationException(ErrorCodes.INVALID_FUNCTION);

        detector.setPartitionFieldName(detector.getPartitionFieldName() + getCharacterPlusSuffix());
        DetectorVerifier.verify(detector, true);
    }

    private String getCharacterPlusSuffix() {
        return character + SUFFIX;
    }

    private void setCharacter(String character ) {
        this.character = character;
    }

    private boolean isValid() { return this.isValid; }

    private void setIsValid(boolean valid) { this.isValid = valid; }

    private void expectJobConfigurationExceptionWhenCharIsInvalid(ErrorCodes errorCode) {
        if (isValid()) {
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
