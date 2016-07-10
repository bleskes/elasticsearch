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

import javax.crypto.SecretKey;
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
     * Signs the given text and returns the signed text (original text + signature)
     * @param text the string to sign
     * @param key the key to sign the text with
     * @param systemKey the system key. This is optional and if the key != systemKey then the format of the
     *                  message will change
     */
    String sign(String text, SecretKey key, SecretKey systemKey) throws IOException;

    /**
     * Unsigns the given signed text, verifies the original text with the attached signature and if valid returns
     * the unsigned (original) text. If signature verification fails a {@link IllegalArgumentException} is thrown.
     * @param text the string to unsign and verify
     * @param key the key to unsign the text with
     */
    String unsignAndVerify(String text, SecretKey key);

    /**
     * Checks whether the given text is signed.
     */
    boolean signed(String text);

    /**
     * Encrypts the provided char array and returns the encrypted values in a char array
     * @param chars the characters to encrypt
     * @return character array representing the encrypted data
     */
    char[] encrypt(char[] chars);

    /**
     * Encrypts the provided byte array and returns the encrypted value
     * @param bytes the data to encrypt
     * @return encrypted data
     */
    byte[] encrypt(byte[] bytes);

    /**
     * Decrypts the provided char array and returns the plain-text chars
     * @param chars the data to decrypt
     * @return plaintext chars
     */
    char[] decrypt(char[] chars);

    /**
     * Decrypts the provided char array and returns the plain-text chars
     * @param chars the data to decrypt
     * @param key the key to decrypt the data with
     * @return plaintext chars
     */
    char[] decrypt(char[] chars, SecretKey key);

    /**
     * Decrypts the provided byte array and returns the unencrypted bytes
     * @param bytes the bytes to decrypt
     * @return plaintext bytes
     */
    byte[] decrypt(byte[] bytes);

    /**
     * Decrypts the provided byte array and returns the unencrypted bytes
     * @param bytes the bytes to decrypt
     * @param key the key to decrypt the data with
     * @return plaintext bytes
     */
    byte[] decrypt(byte[] bytes, SecretKey key);

    /**
     * Checks whether the given chars are encrypted
     * @param chars the chars to check if they are encrypted
     * @return true is data is encrypted
     */
    boolean encrypted(char[] chars);

    /**
     * Checks whether the given bytes are encrypted
     * @param bytes the chars to check if they are encrypted
     * @return true is data is encrypted
     */
    boolean encrypted(byte[] bytes);

    /**
     * Flag for callers to determine if values will actually be encrypted or returned plaintext
     * @return true if values will be encrypted
     */
    boolean encryptionEnabled();
}
