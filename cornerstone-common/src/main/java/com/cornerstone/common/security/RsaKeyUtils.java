package com.cornerstone.common.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥解析工具：解析 PEM 内容（可含头尾与换行）为 {@link RSAPublicKey}/{@link RSAPrivateKey}。
 *
 * <p>gateway/auth/system/demo 四处共用，保证密钥解析行为一致（AGENTS 关键约定：四处密钥一致）。
 */
public final class RsaKeyUtils {

    private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_FOOTER = "-----END PUBLIC KEY-----";
    private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_FOOTER = "-----END PRIVATE KEY-----";

    private RsaKeyUtils() {}

    /** 解析 SPKI PEM 公钥内容（可含或不含 BEGIN/END 头尾、换行）为 RSAPublicKey。 */
    public static RSAPublicKey parsePublicKey(String pem) {
        String base64 = clean(pem, PUBLIC_HEADER, PUBLIC_FOOTER);
        try {
            return (RSAPublicKey)
                    KeyFactory.getInstance("RSA")
                            .generatePublic(
                                    new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (Exception e) {
            throw new IllegalArgumentException("RSA 公钥解析失败：PEM 格式非法", e);
        }
    }

    /** 解析 PKCS8 PEM 私钥内容（可含或不含 BEGIN/END 头尾、换行）为 RSAPrivateKey。 */
    public static RSAPrivateKey parsePrivateKey(String pem) {
        String base64 = clean(pem, PRIVATE_HEADER, PRIVATE_FOOTER);
        try {
            return (RSAPrivateKey)
                    KeyFactory.getInstance("RSA")
                            .generatePrivate(
                                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (Exception e) {
            throw new IllegalArgumentException("RSA 私钥解析失败：PEM 格式非法", e);
        }
    }

    private static String clean(String pem, String header, String footer) {
        return pem.replace(header, "").replace(footer, "").replaceAll("\\s", "");
    }
}
