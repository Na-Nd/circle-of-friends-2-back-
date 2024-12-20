package ru.nand.sharedthings.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class KeyGenerator {
    public static String generateKey(String secret, String token){
        try{
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e){
            throw new RuntimeException("Ошибка генерации ключа", e);
        }
    }
}
