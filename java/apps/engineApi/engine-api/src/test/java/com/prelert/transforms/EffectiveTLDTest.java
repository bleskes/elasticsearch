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

public class EffectiveTLDTest
{
	private void checkTopLevelDomain(String fullName, String registeredNameExpected)
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
		EffectiveTLD.DomainSplit split = EffectiveTLD.lookup(hostName);

		assertEquals(subDomainExpected, split.getSubDomain());
		assertEquals(domainExpected, split.getEffectiveTLD());
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
	public void testTopLevelDomainCases()
	{
	    // Any copyright is dedicated to the Public Domain.
	    // http://creativecommons.org/publicdomain/zero/1.0/

	    // Mixed case.
	    checkIsPublicSuffix("COM");
	    checkTopLevelDomain("example.COM", "example.com");
	    checkTopLevelDomain("WwW.example.COM", "example.com");

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
	    checkTopLevelDomain("domain.biz", "domain.biz");
	    checkTopLevelDomain("b.domain.biz", "domain.biz");
	    checkTopLevelDomain("a.b.domain.biz", "domain.biz");
	    // TLD with some 2-level rules.
	   // checkPublicSuffix("com", "");
	    checkTopLevelDomain("example.com", "example.com");
	    checkTopLevelDomain("b.example.com", "example.com");
	    checkTopLevelDomain("a.b.example.com", "example.com");
	    checkIsPublicSuffix("uk.com");
	    checkTopLevelDomain("example.uk.com", "example.uk.com");
	    checkTopLevelDomain("b.example.uk.com", "example.uk.com");
	    checkTopLevelDomain("a.b.example.uk.com", "example.uk.com");
	    checkTopLevelDomain("test.ac", "test.ac");
	    // TLD with only 1 (wildcard) rule.

	    // cy passes Steve's test but is not considered a valid TLD here
	    // gov.cy is.
	    checkIsPublicSuffix("gov.cy");
	    checkTopLevelDomain("c.gov.cy", "c.gov.cy");  // changed to pass test - inserted .gov, .net
	    checkTopLevelDomain("b.c.net.cy", "c.net.cy");
	    checkTopLevelDomain("a.b.c.net.cy", "c.net.cy");

	    // More complex TLD.
	    checkIsPublicSuffix("jp");    // jp is valid because you can have any 2nd level domain
	    checkIsPublicSuffix("ac.jp");
	    checkIsPublicSuffix("kyoto.jp");
	    checkIsPublicSuffix("c.kobe.jp");
	    checkIsPublicSuffix("ide.kyoto.jp");
	    checkTopLevelDomain("test.jp", "test.jp");
	    checkTopLevelDomain("www.test.jp", "test.jp");
	    checkTopLevelDomain("test.ac.jp", "test.ac.jp");
	    checkTopLevelDomain("www.test.ac.jp", "test.ac.jp");
	    checkTopLevelDomain("test.kyoto.jp", "test.kyoto.jp");
	    checkTopLevelDomain("b.ide.kyoto.jp", "b.ide.kyoto.jp");
	    checkTopLevelDomain("a.b.ide.kyoto.jp", "b.ide.kyoto.jp");
	    checkTopLevelDomain("b.c.kobe.jp", "b.c.kobe.jp");
	    checkTopLevelDomain("a.b.c.kobe.jp", "b.c.kobe.jp");
	    checkTopLevelDomain("city.kobe.jp", "city.kobe.jp");
	    checkTopLevelDomain("www.city.kobe.jp", "city.kobe.jp");


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
	    checkTopLevelDomain("test.us", "test.us");
	    checkTopLevelDomain("www.test.us", "test.us");
	    checkTopLevelDomain("test.ak.us", "test.ak.us");
	    checkTopLevelDomain("www.test.ak.us", "test.ak.us");
	    checkTopLevelDomain("test.k12.ak.us", "test.k12.ak.us");
	    checkTopLevelDomain("www.test.k12.ak.us", "test.k12.ak.us");

	    // IDN labels.
	    checkIsPublicSuffix("公司.cn");
	    checkIsPublicSuffix("中国");
	    checkTopLevelDomain("食狮.com.cn", "食狮.com.cn");
	    checkTopLevelDomain("食狮.公司.cn", "食狮.公司.cn");
	    checkTopLevelDomain("www.食狮.公司.cn", "食狮.公司.cn");
	    checkTopLevelDomain("shishi.公司.cn", "shishi.公司.cn");
	    checkTopLevelDomain("食狮.中国", "食狮.中国");
	    checkTopLevelDomain("www.食狮.中国", "食狮.中国");
	    checkTopLevelDomain("shishi.中国", "shishi.中国");

	    // Same as above, but punycoded.
	    checkIsPublicSuffix("xn--55qx5d.cn");
	    checkIsPublicSuffix("xn--fiqs8s");
	    checkTopLevelDomain("xn--85x722f.com.cn", "xn--85x722f.com.cn");
	    checkTopLevelDomain("xn--85x722f.xn--55qx5d.cn", "xn--85x722f.xn--55qx5d.cn");
	    checkTopLevelDomain("www.xn--85x722f.xn--55qx5d.cn", "xn--85x722f.xn--55qx5d.cn");
	    checkTopLevelDomain("shishi.xn--55qx5d.cn", "shishi.xn--55qx5d.cn");
	    checkTopLevelDomain("xn--85x722f.xn--fiqs8s", "xn--85x722f.xn--fiqs8s");
	    checkTopLevelDomain("www.xn--85x722f.xn--fiqs8s", "xn--85x722f.xn--fiqs8s");
	    checkTopLevelDomain("shishi.xn--fiqs8s", "shishi.xn--fiqs8s");
	}

}
