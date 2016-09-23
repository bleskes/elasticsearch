
package org.elasticsearch.xpack.prelert.job.errorcodes;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * This test ensures that all the error values in {@linkplain ErrorCodes}
 * are unique so no 2 conditions can return the same error code.
 * This tests is designed to catch copy/paste errors.
 */
public class ErrorCodesTest extends ESTestCase
{
	public void testErrorCodesAreUnique()
	throws IllegalArgumentException, IllegalAccessException
	{
		ErrorCodes[] values = ErrorCodes.class.getEnumConstants();

		Set<Long> errorValueSet = new HashSet<>();

		for (ErrorCodes value : values)
		{
			errorValueSet.add(value.getValue());
		}

		assertEquals(values.length, errorValueSet.size());
	}
}
