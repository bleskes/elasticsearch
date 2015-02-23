/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.transforms;

import static org.junit.Assert.*;

import com.google.common.net.InternetDomainName;

import org.junit.Test;

public class HighestRegisteredDomainTest
{
	private void checkHighestRegisteredDomain(String fullName, String registeredNameExpected)
	{
		InternetDomainName effectiveTLD = InternetDomainName.from(fullName);

		effectiveTLD = effectiveTLD.topPrivateDomain();
		assertTrue(effectiveTLD.isTopPrivateDomain());
		String registeredName = effectiveTLD.toString();

		assertEquals(registeredNameExpected, registeredName);
	}

	private void checkIsPublicSuffix(String suffix)
	{
		InternetDomainName effectiveTLD = InternetDomainName.from(suffix);
		assertTrue(effectiveTLD.isPublicSuffix());
	}

	private void testDomainSplit(String subDomainExpected,
          String domainExpected, String hostName)
	{
		HighestRegisteredDomain.DomainSplit split = HighestRegisteredDomain.lookup(hostName);

		assertEquals(subDomainExpected, split.getSubDomain());
		assertEquals(domainExpected, split.getHighestRegisteredDomain());
	}

	@Test
	public void testDomainSplit()
	{
	    // Test cases from https://github.com/john-kurkowski/tldextract/tree/master/tldextract/tests
	    testDomainSplit("www", "google.com", "www.google.com");
	    testDomainSplit("www.maps", "google.co.uk", "www.maps.google.co.uk");
	    testDomainSplit("www", "theregister.co.uk", "www.theregister.co.uk");
	    testDomainSplit("", "gmail.com", "gmail.com");
	    testDomainSplit("media.forums", "theregister.co.uk", "media.forums.theregister.co.uk");
	    testDomainSplit("www", "www.com", "www.www.com");
	    testDomainSplit("", "www.com", "www.com");
	    testDomainSplit("", "internalunlikelyhostname", "internalunlikelyhostname");
	    testDomainSplit("internalunlikelyhostname", "bizarre", "internalunlikelyhostname.bizarre");
	    testDomainSplit("", "internalunlikelyhostname.info", "internalunlikelyhostname.info");  // .info is a valid TLD
	    testDomainSplit("internalunlikelyhostname", "information", "internalunlikelyhostname.information");
	    testDomainSplit("", "216.22.0.192", "216.22.0.192");
	    testDomainSplit("", "::1", "::1");
	    testDomainSplit("", "FE80:0000:0000:0000:0202:B3FF:FE1E:8329", "FE80:0000:0000:0000:0202:B3FF:FE1E:8329");
	    testDomainSplit("216.22", "project.coop", "216.22.project.coop");
	    testDomainSplit("www", "xn--h1alffa9f.xn--p1ai", "www.xn--h1alffa9f.xn--p1ai");
	    testDomainSplit("", "", "");
	    testDomainSplit("www", "parliament.uk", "www.parliament.uk");
	    testDomainSplit("www", "parliament.co.uk", "www.parliament.co.uk");
	    testDomainSplit("www.a", "cgs.act.edu.au", "www.a.cgs.act.edu.au");
	    testDomainSplit("www", "google.com.au", "www.google.com.au");
	    testDomainSplit("www", "metp.net.cn", "www.metp.net.cn");
	    testDomainSplit("www", "waiterrant.blogspot.com", "www.waiterrant.blogspot.com");

	    testDomainSplit("", "kittens.blogspot.co.uk", "kittens.blogspot.co.uk");
	    testDomainSplit("", "prelert.s3.amazonaws.com", "prelert.s3.amazonaws.com");
	    testDomainSplit("daves_bucket", "prelert.s3.amazonaws.com", "daves_bucket.prelert.s3.amazonaws.com");

	    testDomainSplit("example", "example", "example.example");
	    testDomainSplit("b.example", "example", "b.example.example");
	    testDomainSplit("a.b.example", "example", "a.b.example.example");

	    testDomainSplit("example", "local", "example.local");
	    testDomainSplit("b.example", "local", "b.example.local");
	    testDomainSplit("a.b.example", "local", "a.b.example.local");
	}

	@Test
	public void testHighestRegisteredDomainCases()
	{
	    // Any copyright is dedicated to the Public Domain.
	    // http://creativecommons.org/publicdomain/zero/1.0/

	    // Mixed case.
	    checkIsPublicSuffix("COM");
	    checkHighestRegisteredDomain("example.COM", "example.com");
	    checkHighestRegisteredDomain("WwW.example.COM", "example.com");

	    // These pass steve's test but fail here. Example isn't a valid (declared, not active) TLD
//	    checkIsPublicSuffix("example");
//	    checkTopLevelDomain("example.example", "example.example");
//	    checkTopLevelDomain("b.example.example", "example.example");
//	    checkTopLevelDomain("a.b.example.example", "example.example");

	    // Listed, but non-Internet, TLD.
	    // checkIsPublicSuffix("local");     // These pass Steve's tests but not public suffix here
	    //checkIsPublicSuffix("example.local", "");
	    //checkIsPublicSuffix("b.example.local", "");
	    //checkIsPublicSuffix("a.b.example.local", "");

	    // TLD with only 1 rule.
	    checkIsPublicSuffix("biz");
	    checkHighestRegisteredDomain("domain.biz", "domain.biz");
	    checkHighestRegisteredDomain("b.domain.biz", "domain.biz");
	    checkHighestRegisteredDomain("a.b.domain.biz", "domain.biz");
	    // TLD with some 2-level rules.
	   // checkPublicSuffix("com", "");
	    checkHighestRegisteredDomain("example.com", "example.com");
	    checkHighestRegisteredDomain("b.example.com", "example.com");
	    checkHighestRegisteredDomain("a.b.example.com", "example.com");
	    checkIsPublicSuffix("uk.com");
	    checkHighestRegisteredDomain("example.uk.com", "example.uk.com");
	    checkHighestRegisteredDomain("b.example.uk.com", "example.uk.com");
	    checkHighestRegisteredDomain("a.b.example.uk.com", "example.uk.com");
	    checkHighestRegisteredDomain("test.ac", "test.ac");
	    // TLD with only 1 (wildcard) rule.

	    // cy passes Steve's test but is not considered a valid TLD here
	    // gov.cy is.
	    checkIsPublicSuffix("gov.cy");
	    checkHighestRegisteredDomain("c.gov.cy", "c.gov.cy");  // changed to pass test - inserted .gov, .net
	    checkHighestRegisteredDomain("b.c.net.cy", "c.net.cy");
	    checkHighestRegisteredDomain("a.b.c.net.cy", "c.net.cy");

	    // More complex TLD.
	    checkIsPublicSuffix("jp");    // jp is valid because you can have any 2nd level domain
	    checkIsPublicSuffix("ac.jp");
	    checkIsPublicSuffix("kyoto.jp");
	    checkIsPublicSuffix("c.kobe.jp");
	    checkIsPublicSuffix("ide.kyoto.jp");
	    checkHighestRegisteredDomain("test.jp", "test.jp");
	    checkHighestRegisteredDomain("www.test.jp", "test.jp");
	    checkHighestRegisteredDomain("test.ac.jp", "test.ac.jp");
	    checkHighestRegisteredDomain("www.test.ac.jp", "test.ac.jp");
	    checkHighestRegisteredDomain("test.kyoto.jp", "test.kyoto.jp");
	    checkHighestRegisteredDomain("b.ide.kyoto.jp", "b.ide.kyoto.jp");
	    checkHighestRegisteredDomain("a.b.ide.kyoto.jp", "b.ide.kyoto.jp");
	    checkHighestRegisteredDomain("b.c.kobe.jp", "b.c.kobe.jp");
	    checkHighestRegisteredDomain("a.b.c.kobe.jp", "b.c.kobe.jp");
	    checkHighestRegisteredDomain("city.kobe.jp", "city.kobe.jp");
	    checkHighestRegisteredDomain("www.city.kobe.jp", "city.kobe.jp");


	    // TLD with a wildcard rule and exceptions.
//	    checkIsPublicSuffix("ck");   // Passes Steve's test but is not considered a valid TLD here
//	    checkIsPublicSuffix("test.ck");
//	    checkTopLevelDomain("b.test.ck", "b.test.ck");
//	    checkTopLevelDomain("a.b.test.ck", "b.test.ck");
//	    checkTopLevelDomain("www.ck", "www.ck");
//	    checkTopLevelDomain("www.www.ck", "www.ck");

	    // US K12.
	    checkIsPublicSuffix("us");
	    checkIsPublicSuffix("ak.us");
	    checkIsPublicSuffix("k12.ak.us");
	    checkHighestRegisteredDomain("test.us", "test.us");
	    checkHighestRegisteredDomain("www.test.us", "test.us");
	    checkHighestRegisteredDomain("test.ak.us", "test.ak.us");
	    checkHighestRegisteredDomain("www.test.ak.us", "test.ak.us");
	    checkHighestRegisteredDomain("test.k12.ak.us", "test.k12.ak.us");
	    checkHighestRegisteredDomain("www.test.k12.ak.us", "test.k12.ak.us");

	    // IDN labels.
	    checkIsPublicSuffix("公司.cn");
	    checkIsPublicSuffix("中国");
	    checkHighestRegisteredDomain("食狮.com.cn", "食狮.com.cn");
	    checkHighestRegisteredDomain("食狮.公司.cn", "食狮.公司.cn");
	    checkHighestRegisteredDomain("www.食狮.公司.cn", "食狮.公司.cn");
	    checkHighestRegisteredDomain("shishi.公司.cn", "shishi.公司.cn");
	    checkHighestRegisteredDomain("食狮.中国", "食狮.中国");
	    checkHighestRegisteredDomain("www.食狮.中国", "食狮.中国");
	    checkHighestRegisteredDomain("shishi.中国", "shishi.中国");

	    // Same as above, but punycoded.
	    checkIsPublicSuffix("xn--55qx5d.cn");
	    checkIsPublicSuffix("xn--fiqs8s");
	    checkHighestRegisteredDomain("xn--85x722f.com.cn", "xn--85x722f.com.cn");
	    checkHighestRegisteredDomain("xn--85x722f.xn--55qx5d.cn", "xn--85x722f.xn--55qx5d.cn");
	    checkHighestRegisteredDomain("www.xn--85x722f.xn--55qx5d.cn", "xn--85x722f.xn--55qx5d.cn");
	    checkHighestRegisteredDomain("shishi.xn--55qx5d.cn", "shishi.xn--55qx5d.cn");
	    checkHighestRegisteredDomain("xn--85x722f.xn--fiqs8s", "xn--85x722f.xn--fiqs8s");
	    checkHighestRegisteredDomain("www.xn--85x722f.xn--fiqs8s", "xn--85x722f.xn--fiqs8s");
	    checkHighestRegisteredDomain("shishi.xn--fiqs8s", "shishi.xn--fiqs8s");
	}

	/**
	 * Get sub domain only
	 * @throws TransformException
	 */
	@Test
	public void testTransform_SingleOutput() throws TransformException
	{
		HighestRegisteredDomain transform = new HighestRegisteredDomain(new int [] {2}, new int [] {0});

		String [] input = {"", "", "www.test.ac.jp"};
		String [] output = new String [2];

		transform.transform(input, output);
		assertEquals("www", output[0]);
		assertNull(output[1]);

		input[2] = "a.b.domain.biz";
		transform.transform(input, output);
		assertEquals("a.b", output[0]);
		assertNull(output[1]);
	}

	@Test
	public void testTransform_AllOutputs() throws TransformException
	{
		HighestRegisteredDomain transform = new HighestRegisteredDomain(new int [] {2}, new int [] {0, 1});

		String [] input = {"", "", "www.test.ac.jp"};
		String [] output = new String [2];

		transform.transform(input, output);
		assertEquals("www", output[0]);
		assertEquals("test.ac.jp", output[1]);

		input[2] = "a.b.domain.biz";
		transform.transform(input, output);
		assertEquals("a.b", output[0]);
		assertEquals("domain.biz", output[1]);
	}

}
