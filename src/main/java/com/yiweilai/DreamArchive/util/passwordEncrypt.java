package com.yiweilai.DreamArchive.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

public class passwordEncrypt {
    public static String encrypt(String password) throws Exception {
        KeyGenerator keygen =KeyGenerator.getInstance("AES");
        keygen.init(256);
        SecretKey secretKey = keygen.generateKey();
        byte[] raw = secretKey.getEncoded();
        SecretKeySpec key = new SecretKeySpec(raw, "AES");
        return String.valueOf(key);
    }
}
