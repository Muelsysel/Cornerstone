package com.cornerstone.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** RSA PEM 解析工具测试：带头尾/换行的 SPKI 公钥与 PKCS8 私钥均可解析，非法内容抛异常。 */
class RsaKeyUtilsTest {

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void parsesPublicKeyWithPemHeaders() throws Exception {
        KeyPair pair = generateKeyPair();
        String pem =
                "-----BEGIN PUBLIC KEY-----\n"
                        + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
                        + "\n-----END PUBLIC KEY-----";

        assertEquals(pair.getPublic(), RsaKeyUtils.parsePublicKey(pem));
    }

    @Test
    void parsesPrivateKeyWithPemHeaders() throws Exception {
        KeyPair pair = generateKeyPair();
        String pem =
                "-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----";

        assertEquals(pair.getPrivate(), RsaKeyUtils.parsePrivateKey(pem));
    }

    @Test
    void rejectsInvalidPublicKey() {
        assertThrows(IllegalArgumentException.class, () -> RsaKeyUtils.parsePublicKey("not-a-pem"));
    }
}
