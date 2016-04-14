/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.job.password;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import com.prelert.job.PasswordStorage;

public class PasswordManagerTest
{
    private static final byte[] TEST_KEY_BYTES = { 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16 };

    @Test(expected = NoSuchAlgorithmException.class)
    public void testInvalidAlgorithm() throws NoSuchAlgorithmException
    {
        new PasswordManager("DAVE", TEST_KEY_BYTES);
    }

    @Test
    public void testNullEncrypt() throws GeneralSecurityException
    {
        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);

        String encrypted = mgr.encryptPassword(null);
        assertNull(encrypted);
    }

    @Test
    public void testNullDecrypt() throws GeneralSecurityException
    {
        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);

        String decrypted = mgr.decryptPassword(null);
        assertNull(decrypted);
    }

    @Test
    public void testRoundTrip() throws GeneralSecurityException
    {
        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);

        String orig = "my_password!";

        String encrypted = mgr.encryptPassword(orig);
        assertFalse(orig.equals(encrypted));

        String roundTripped = mgr.decryptPassword(encrypted);
        assertEquals(orig, roundTripped);
    }

    @Test
    public void testSecureStorage_GivenNoPassword() throws GeneralSecurityException
    {
        SimplePasswordStorage storage = new SimplePasswordStorage(null, null);

        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);
        assertFalse(mgr.secureStorage(storage));

        assertNull(storage.getPassword());
        assertNull(storage.getEncryptedPassword());
    }

    @Test
    public void testSecureStorage_GivenFirstTime() throws GeneralSecurityException
    {
        SimplePasswordStorage storage = new SimplePasswordStorage("my_new_password!", null);

        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);
        assertTrue(mgr.secureStorage(storage));

        assertNull(storage.getPassword());
        assertNotNull(storage.getEncryptedPassword());
        assertFalse("new_password".equals(storage.getEncryptedPassword()));
    }

    @Test
    public void testSecureStorage_GivenAlreadyDone() throws GeneralSecurityException
    {
        SimplePasswordStorage storage = new SimplePasswordStorage(null, "already_encrypted");

        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);
        assertFalse(mgr.secureStorage(storage));

        assertNull(storage.getPassword());
        assertEquals("already_encrypted", storage.getEncryptedPassword());
    }

    @Test
    public void testSecureStorage_GivenUpdate() throws GeneralSecurityException
    {
        SimplePasswordStorage storage = new SimplePasswordStorage("my_new_password!", "already_encrypted");

        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);
        assertTrue(mgr.secureStorage(storage));

        assertNull(storage.getPassword());
        assertNotNull(storage.getEncryptedPassword());
        assertFalse("my_new_password!".equals(storage.getEncryptedPassword()));
        assertFalse("already_encrypted".equals(storage.getEncryptedPassword()));
    }

    @Test
    public void testConstructFromFile_GivenExists() throws GeneralSecurityException, IOException
    {
        File testFile = new File("test.key");
        try (FileOutputStream strm = new FileOutputStream(testFile))
        {
            strm.write(TEST_KEY_BYTES);
        }

        PasswordManager mgrFromFile = new PasswordManager("AES/CBC/PKCS5Padding", testFile);

        String orig = "my_password!";

        String encrypted = mgrFromFile.encryptPassword(orig);
        assertFalse(orig.equals(encrypted));

        // Decrypt with a different password manager initialised directly with
        // the key bytes rather than reading them from a file - this proves
        // that reading from a file is equivalent
        PasswordManager mgr = new PasswordManager("AES/CBC/PKCS5Padding", TEST_KEY_BYTES);

        String roundTripped = mgr.decryptPassword(encrypted);
        assertEquals(orig, roundTripped);

        assertTrue(testFile.delete());
    }

    @Test(expected = EOFException.class)
    public void testConstructFromFile_GivenTooShort() throws NoSuchAlgorithmException, IOException
    {
        File testFile = new File("test.key");
        try
        {
            try (FileOutputStream strm = new FileOutputStream(testFile))
            {
                strm.write(7);
            }

            new PasswordManager("AES/CBC/PKCS5Padding", testFile);
        }
        finally
        {
            testFile.delete();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testConstructFromFile_GivenDoesNotExist() throws NoSuchAlgorithmException, IOException
    {
        new PasswordManager("AES/CBC/PKCS5Padding", new File("does_not_exist"));
    }

    private static class SimplePasswordStorage implements PasswordStorage
    {
        private String m_Password;
        private String m_EncryptedPassword;

        public SimplePasswordStorage(String password, String encryptedPassword)
        {
            m_Password = password;
            m_EncryptedPassword = encryptedPassword;
        }

        @Override
        public String getPassword()
        {
            return m_Password;
        }

        @Override
        public void setPassword(String password)
        {
            m_Password = password;
        }

        @Override
        public String getEncryptedPassword()
        {
            return m_EncryptedPassword;
        }

        @Override
        public void setEncryptedPassword(String encryptedPassword)
        {
            m_EncryptedPassword = encryptedPassword;
        }
    }
}
