package org.elasticsearch.license.plugin;

import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class LicenseVersionTests extends ElasticsearchTestCase {

    @Test
    public void testStrings() throws Exception {
        boolean beta = randomBoolean();
        int buildNumber = beta ? randomIntBetween(0, 49) : randomIntBetween(0, 48);
        int major = randomIntBetween(0, 20);
        int minor = randomIntBetween(0, 20);
        int revision = randomIntBetween(0, 20);

        String build = buildNumber == 0 ? "" :
                beta ? "-beta" + buildNumber : "-rc" + buildNumber;


        String versionName = new StringBuilder()
                .append(major)
                .append(".").append(minor)
                .append(".").append(revision)
                .append(build).toString();
        LicenseVersion version = LicenseVersion.fromString(versionName);

        logger.info("version: {}", versionName);

        assertThat(version.major, is((byte) major));
        assertThat(version.minor, is((byte) minor));
        assertThat(version.revision, is((byte) revision));
        if (buildNumber == 0) {
            assertThat(version.build, is((byte) 99));
        } else if (beta) {
            assertThat(version.build, is((byte) buildNumber));
        } else {
            assertThat(version.build, is((byte) (buildNumber + 50)));
        }

        assertThat(version.number(), equalTo(versionName));
    }

}
