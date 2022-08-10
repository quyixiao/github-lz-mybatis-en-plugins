//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.lz.mybatis.plugins.interceptor.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PAesUtil {


    private static byte[] doEncrypt(String content, String password) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(password.getBytes());
            kgen.init(128, secureRandom);
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            byte[] byteContent = content.getBytes("UTF-8");
            cipher.init(1, key);
            byte[] result = cipher.doFinal(byteContent);
            return result;
        } catch (NoSuchAlgorithmException var10) {
            System.out.println("encrypt NoSuchAlgorithmException");
        } catch (NoSuchPaddingException var11) {
            System.out.println("encrypt NoSuchPaddingException");
        } catch (InvalidKeyException var12) {
            System.out.println("encrypt InvalidKeyException");
        } catch (UnsupportedEncodingException var13) {
            System.out.println("encrypt UnsupportedEncodingException");
        } catch (IllegalBlockSizeException var14) {
            System.out.println("encrypt IllegalBlockSizeException");
        } catch (BadPaddingException var15) {
            System.out.println("encrypt BadPaddingException");
        }

        return null;
    }

    private static String decrypt(String contentStr, String password) {
        try {
            byte[] content = Base64.decodeBase64(contentStr.getBytes());
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(password.getBytes());
            kgen.init(128, secureRandom);
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(2, key);
            byte[] result = cipher.doFinal(content);
            return new String(result);
        } catch (NoSuchAlgorithmException var10) {
            System.out.println("decrypt NoSuchAlgorithmException");
        } catch (NoSuchPaddingException var11) {
            System.out.println("decrypt NoSuchPaddingException");
        } catch (InvalidKeyException var12) {
            System.out.println("decrypt InvalidKeyException");
        } catch (IllegalBlockSizeException var13) {
            System.out.println("decrypt IllegalBlockSizeException");
        } catch (BadPaddingException var14) {
            System.out.println("decrypt BadPaddingException");
        }

        return contentStr;
    }

    private static String encrypt(String value, String encrypt_password) {
        return new String(Base64.encodeBase64(doEncrypt(value, encrypt_password)));
    }

    public static String decrypt(String value) {
        return decrypt(value, getPassword());
    }


    private static String getPassword() {
        String password = "123456";
        return password;
    }

    public static String encrypt(String value) {
        return new String(Base64.encodeBase64(doEncrypt(value, getPassword())));
    }

    public static void main(String[] args) {
        System.out.println(decrypt("xxxxx"));
    }
}
