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

package org.elasticsearch.shield.crypto;

/**
 * Service that provides cryptographic methods based on a shared system key
 */
public interface CryptoService {

    /**
     * Signs the given text and returns the signed text (original text + signature)
     */
    String sign(String text);

    /**
     * Unsigns the given signed text, verifies the original text with the attached signature and if valid returns
     * the unsigned (original) text. If signature verification fails a {@link SignatureException} is thrown.
     */
    String unsignAndVerify(String text);

    /**
     * Checks whether the given text is signed.
     */
    boolean signed(String text);

    /**
     * Encrypts the provided char array and returns the encrypted values in a Base64 encoded char array
     * @param chars the characters to encrypt
     * @return Base64 character array representing the encrypted data
     * @throws UnsupportedOperationException if the system key is not present
     */
    char[] encrypt(char[] chars);

    /**
     * Encrypts the provided byte array and returns the encrypted value
     * @param bytes the data to encrypt
     * @return encrypted data
     * @throws UnsupportedOperationException if the system key is not present
     */
    byte[] encrypt(byte[] bytes);

    /**
     * Decrypts the provided char array and returns the plain-text chars
     * @param chars the Base64 encoded data to decrypt
     * @return plaintext chars
     * @throws UnsupportedOperationException if the system key is not present
     */
    char[] decrypt(char[] chars);

    /**
     * Decrypts the provided byte array and returns the unencrypted bytes
     * @param bytes the bytes to decrypt
     * @return plaintext bytes
     * @throws UnsupportedOperationException if the system key is not present
     */
    byte[] decrypt(byte[] bytes);
}
