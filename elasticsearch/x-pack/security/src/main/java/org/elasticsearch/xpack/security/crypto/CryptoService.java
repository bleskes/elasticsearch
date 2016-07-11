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

package org.elasticsearch.xpack.security.crypto;

import java.io.IOException;

/**
 * Service that provides cryptographic methods based on a shared system key
 */
public interface CryptoService {

    /**
     * Signs the given text and returns the signed text (original text + signature)
     * @param text the string to sign
     */
    String sign(String text) throws IOException;

    /**
     * Unsigns the given signed text, verifies the original text with the attached signature and if valid returns
     * the unsigned (original) text. If signature verification fails a {@link IllegalArgumentException} is thrown.
     * @param text the string to unsign and verify
     */
    String unsignAndVerify(String text);

    /**
     * Checks whether the given text is signed.
     */
    boolean isSigned(String text);

    /**
     * Encrypts the provided char array and returns the encrypted values in a char array
     * @param chars the characters to encrypt
     * @return character array representing the encrypted data
     */
    char[] encrypt(char[] chars);

    /**
     * Decrypts the provided char array and returns the plain-text chars
     * @param chars the data to decrypt
     * @return plaintext chars
     */
    char[] decrypt(char[] chars);

    /**
     * Checks whether the given chars are encrypted
     * @param chars the chars to check if they are encrypted
     * @return true is data is encrypted
     */
    boolean isEncrypted(char[] chars);

    /**
     * Flag for callers to determine if values will actually be encrypted or returned plaintext
     * @return true if values will be encrypted
     */
    boolean isEncryptionEnabled();
}
