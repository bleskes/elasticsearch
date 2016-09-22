
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RuleActionTest extends ESTestCase {

    public void testForString() {
        assertEquals(RuleAction.FILTER_RESULTS, RuleAction.forString("filter_results"));
        assertEquals(RuleAction.FILTER_RESULTS, RuleAction.forString("FILTER_RESULTS"));
        assertEquals(RuleAction.FILTER_RESULTS, RuleAction.forString("fiLTer_Results"));
    }
}
