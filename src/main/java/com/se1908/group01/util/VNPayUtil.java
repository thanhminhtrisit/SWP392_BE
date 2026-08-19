package com.se1908.group01.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class VNPayUtil {

    public static String hmacSHA512(
            String key,
            String data) {

        try {

            Mac hmac512 =
                    Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            key.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA512");

            hmac512.init(secretKey);

            byte[] bytes =
                    hmac512.doFinal(
                            data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash =
                    new StringBuilder();

            for (byte b : bytes) {
                hash.append(
                        String.format("%02x", b));
            }

            return hash.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildQuery(
            Map<String, String> params) {

        return params.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry ->
                        entry.getKey()
                                + "="
                                + URLEncoder.encode(
                                entry.getValue(),
                                StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    public static boolean isValidSignature(
            Map<String, String> params,
            String hashSecret) {

        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> signedParams = params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("vnp_"))
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> !"vnp_SecureHashType".equals(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        String expectedHash = hmacSHA512(
                hashSecret,
                buildQuery(signedParams)
        );

        return MessageDigest.isEqual(
                expectedHash.toLowerCase(Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8),
                receivedHash.toLowerCase(Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
