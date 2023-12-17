package com.zrlog.plugin.backup.util;

import com.zrlog.plugin.common.SecurityUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AESCrypto {

    private final IvParameterSpec iv;
    private final SecretKeySpec secretKeySpec;

    public AESCrypto(String key) {
        iv = new IvParameterSpec(SecurityUtils.md5(key).substring(0, 16).getBytes(StandardCharsets.UTF_8));
        secretKeySpec = new SecretKeySpec(SecurityUtils.md5(key).getBytes(StandardCharsets.UTF_8), "AES");
    }

    public byte[] encrypt(byte[] value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
        return cipher.doFinal(value);
    }

    public byte[] decrypt(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
        return cipher.doFinal(encrypted);
    }
}