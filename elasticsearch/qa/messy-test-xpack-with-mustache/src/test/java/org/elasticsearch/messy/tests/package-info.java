/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

/**
 * This package contains tests that use mustache to test what looks
 * to be unrelated functionality, or functionality that should be 
 * tested with a mock instead. Instead of doing an epic battle
 * with these tests, they are temporarily moved here to the mustache
 * module's tests, but that is likely not where they belong. Please 
 * help by cleaning them up and we can remove this package!
 *
 * <ul>
 *   <li>If the test is testing templating integration with another core subsystem,
 *       fix it to use a mock instead, so it can be in the core tests again</li>
 *   <li>If the test is just being lazy, and does not really need templating to test
 *       something, clean it up!</li>
 * </ul>
 */

package org.elasticsearch.messy.tests;
