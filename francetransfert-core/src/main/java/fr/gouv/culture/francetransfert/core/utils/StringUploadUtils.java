/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.utils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;

/**
 * StringUtils class Utily class to manipulate Strings
 */
@Service
public class StringUploadUtils {

    private static final int INDEX_NOT_FOUND = -1;

    private final static String REGEX_VALID_EMAIL = "^[a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

    @Value("${upload.domaine.list:#{null}}")
    private List<String> domaineList;

    @Autowired
    private RedisManager redisManager;

    private StringUploadUtils() {
        // private constructor
    }

    /**
     * <p>
     * Checks if a CharSequence is whitespace, empty ("") or null.
     * </p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32) from both ends of this String,
     * handling {@code null} by returning {@code null}.
     * </p>
     *
     * <p>
     * The String is trimmed using {@link String#trim()}. Trim removes start and end
     * characters &lt;= 32. To strip whitespace use .
     * </p>
     *
     * <p>
     * To trim your choice of characters, use the methods.
     * </p>
     *
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("abc")         = "abc"
     * StringUtils.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * <p>
     * Checks if a CharSequence is empty ("") or null.
     * </p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * <p>
     * NOTE: This method changed in Lang version 2.0. It no longer trims the
     * CharSequence. That functionality is available in isBlank().
     * </p>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>
     * Removes all occurrences of a character from within the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("") source
     * string will return the empty string.
     * </p>
     *
     * <pre>
     * StringUtils.remove(null, *)       = null
     * StringUtils.remove("", *)         = ""
     * StringUtils.remove("queued", 'u') = "qeed"
     * StringUtils.remove("queued", 'z') = "queued"
     * </pre>
     *
     * @param str    the source String to search, may be null
     * @param remove the char to search for and remove, may be null
     * @return the substring with the char removed if found, {@code null} if null
     *         String input
     * @since 2.1
     */
    public static String remove(final String str, final char remove) {
        if (isEmpty(str) || str.indexOf(remove) == INDEX_NOT_FOUND) {
            return str;
        }
        final char[] chars = str.toCharArray();
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != remove) {
                chars[pos++] = chars[i];
            }
        }
        return new String(chars, 0, pos);
    }

    public String extractDomainNameFromEmailAddress(String str) {
        String result = "";
        if (str != null && !str.isEmpty()) {
            result = str.substring(str.lastIndexOf("@") + 1).toLowerCase();
        }
        return result;
    }

    public boolean isValidEmail(String email) {
        return isValidRegex(REGEX_VALID_EMAIL, email);
    }

    public boolean isValidEmailIgni(String email) {
        if (isValidRegex(REGEX_VALID_EMAIL, email)) {
            String domainEmail = extractDomainNameFromEmailAddress(email);
            if (domaineList != null && domaineList.size() > 0) {
                if (domaineList.contains(domainEmail.toLowerCase())) {
                    return redisManager.sexists(RedisKeysEnum.FT_DOMAINS_MAILS_MAILS.getKey(""),
                            domainEmail.toLowerCase());
                } else {
                    return false;
                }
            } else {
                return redisManager.sexists(RedisKeysEnum.FT_DOMAINS_MAILS_MAILS.getKey(""), domainEmail.toLowerCase());
            }
        }
        return false;
    }

    public static boolean isValidRegex(String p, String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return Pattern.matches(p, str);
    }

    /**
     * @param emailAddress
     * @return
     */
    public String getEmailDomain(String emailAddress) {
        return Optional.of(emailAddress.substring(emailAddress.indexOf("@") + 1).toLowerCase()).orElse(null);
    }

}
