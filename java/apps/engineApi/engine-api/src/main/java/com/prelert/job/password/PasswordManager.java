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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import com.prelert.job.PasswordStorage;


/**
 * Manages passwords.  Supports 3 operations:
 * <ol>
 * <li>Encryption</li>
 * <li>Decryption</li>
 * <li>Securing a password storage object by encrypting any password it contains</li>
 * </ol>
 */
public class PasswordManager
{
    private static final Logger LOGGER = Logger.getLogger(PasswordManager.class);

    private static final int BITS_PER_BYTE = 8;

    private final String m_Transformation;
    private final Key m_EncryptionKey;

    /**
     * Construct with a supplied key.
     * @param transformation The transformation to apply, which is a string of
     * the form algorithm/mode/padding.  Given the way this class works there are
     * constraints on the sensible values for this.  Some sensible values are:
     * <ul>
     * <li>AES/CBC/PKCS5Padding</li>
     * <li>DES/CBC/PKCS5Padding</li>
     * <li>DESede/CBC/PKCS5Padding</li>
     * </ul>
     * @param keyBytes The raw bytes of the key to use.
     */
    public PasswordManager(String transformation, byte[] keyBytes)
            throws NoSuchAlgorithmException
    {
        Objects.requireNonNull(transformation);
        String encryptionAlgorithm = transformation.split("/")[0];

        // This is here purely to validate that the specified algorithm is supported
        KeyGenerator.getInstance(encryptionAlgorithm);

        m_Transformation = transformation;
        m_EncryptionKey = new SecretKeySpec(keyBytes, encryptionAlgorithm);
    }

    /**
     * Construct by reading the key from a file.
     * @param transformation The transformation to apply, which is a string of
     * the form algorithm/mode/padding.  Given the way this class works there are
     * constraints on the sensible values for this.  Some sensible values are:
     * <ul>
     * <li>AES/CBC/PKCS5Padding</li>
     * <li>DES/CBC/PKCS5Padding</li>
     * <li>DESede/CBC/PKCS5Padding</li>
     * </ul>
     * @param encryptionKeyFile File storing the encryption key.
     */
    public PasswordManager(String transformation, File encryptionKeyFile)
            throws NoSuchAlgorithmException, IOException
    {
        Objects.requireNonNull(transformation);
        String encryptionAlgorithm = transformation.split("/")[0];

        // Generate a dummy key so we get to find out how many bytes ought to be
        // in the file
        byte[] keyBytes = KeyGenerator.getInstance(encryptionAlgorithm).generateKey().getEncoded();

        try (FileInputStream stream = new FileInputStream(encryptionKeyFile))
        {
            int totalBytesRead = 0;
            int bytesRead = 0;
            do
            {
                bytesRead = stream.read(keyBytes, totalBytesRead, keyBytes.length - totalBytesRead);
                if (bytesRead > 0)
                {
                    totalBytesRead += bytesRead;
                }
            }
            while (bytesRead > 0 && totalBytesRead < keyBytes.length);

            if (totalBytesRead < keyBytes.length)
            {
                throw new EOFException("Expected to read " + keyBytes.length +
                        " bytes from " + encryptionKeyFile.getAbsolutePath() +
                        " but could only read " + totalBytesRead);
            }
        }

        LOGGER.debug("Read a " + (keyBytes.length * BITS_PER_BYTE) +
                " bit " + encryptionAlgorithm + " encryption key from " +
                encryptionKeyFile.getAbsolutePath());

        m_Transformation = transformation;
        m_EncryptionKey = new SecretKeySpec(keyBytes, encryptionAlgorithm);
    }

    /**
     * Encrypt a password.  <code>null</code> encrypts to <code>null</code>.
     * Apart from that, an identical password encrypted twice will result
     * in a different encrypted form.  The encrypted form is returned as a
     * base 64 string.
     * @param password The plain text password to encrypt.
     * @return The encrypted form.
     */
    public String encryptPassword(String password)
            throws GeneralSecurityException
    {
        if (password == null)
        {
            return null;
        }

        Cipher cipher = Cipher.getInstance(m_Transformation);
        SecureRandom sr = new SecureRandom();
        byte[] ivBytes = new byte[cipher.getBlockSize()];
        sr.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, m_EncryptionKey, ivSpec, sr);

        byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(concatByteArrays(ivBytes, encryptedBytes));
    }

    private static byte[] concatByteArrays(byte[] first, byte[] second)
    {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0 , result, 0, first.length);
        System.arraycopy(second, 0 , result, first.length, second.length);
        return result;
    }

    /**
     * Decrypt a password.  <code>null</code> decrypts to <code>null</code>.
     * The encrypted string provided must be a base 64 created by this class.
     * @param encryptedPassword The encrypted password to decrypt.
     * @return The plain text form.
     */
    public String decryptPassword(String encryptedPassword)
            throws GeneralSecurityException
    {
        if (encryptedPassword == null)
        {
            return null;
        }

        byte[] decodedInput = Base64.getDecoder().decode(encryptedPassword);

        Cipher cipher = Cipher.getInstance(m_Transformation);
        int blockSize = cipher.getBlockSize();
        IvParameterSpec ivSpec = new IvParameterSpec(decodedInput, 0, blockSize);
        cipher.init(Cipher.DECRYPT_MODE, m_EncryptionKey, ivSpec);

        return new String(cipher.doFinal(decodedInput, blockSize, decodedInput.length - blockSize),
                StandardCharsets.UTF_8);
    }

    /**
     * If a password storage object contains a plain text password, replace
     * it with an encrypted password.
     * @param storage The password storage to be secured.
     * @return Was a change made?
     */
    public boolean secureStorage(PasswordStorage storage)
            throws GeneralSecurityException
    {
        String password = storage.getPassword();
        if (password == null)
        {
            return false;
        }
        storage.setEncryptedPassword(encryptPassword(password));
        storage.setPassword(null);
        return true;
    }
}
