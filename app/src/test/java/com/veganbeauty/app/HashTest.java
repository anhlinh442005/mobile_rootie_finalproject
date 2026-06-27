package com.veganbeauty.app;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashTest {
    @Test
    public void testHash() throws NoSuchAlgorithmException {
        String password = "password123";
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(password.getBytes());
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        System.out.println("Hash is: " + hash.toString());
        assertEquals("ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f", hash.toString());
    }
}
