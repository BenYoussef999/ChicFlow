package com.chicflow.tunisia.utils;

import java.net.URI;
import java.net.URISyntaxException;

/** Cleans a website address typed by an admin before it is stored and opened. */
public final class UrlUtils {

    private UrlUtils() { }

    /**
     * @return "" when the input is empty (no website), a clean http(s) address when it is valid,
     *         or null when it is not a valid website address.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String url = input.trim();
        if (url.isEmpty()) return "";
        if (url.length() > 255) return null;

        boolean hasScheme = url.regionMatches(true, 0, "http://", 0, 7)
                || url.regionMatches(true, 0, "https://", 0, 8);
        if (!hasScheme) {
            url = "https://" + url;
        }
        try {
            String host = new URI(url).getHost();
            // needs a real host name such as shop.example.tn (this also rejects "javascript:..." tricks)
            if (host == null || !host.contains(".") || host.startsWith(".") || host.endsWith(".")) {
                return null;
            }
            return url;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
