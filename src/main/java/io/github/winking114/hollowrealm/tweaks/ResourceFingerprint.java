package io.github.winking114.hollowrealm.tweaks;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ResourceFingerprint {
    private ResourceFingerprint() {}
    static boolean matches(byte[] source, String expected) {
        try {
            return expected.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("The Java runtime does not provide SHA-256", impossible);
        }
    }
}
