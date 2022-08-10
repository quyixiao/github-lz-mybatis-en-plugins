package com.lz.mybatis.plugins.interceptor.utils;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PStringUtils {


    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String FOLDER_SEPARATOR = "/";

    private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

    private static final String TOP_PATH = "..";

    private static final String CURRENT_PATH = ".";

    private static final char EXTENSION_SEPARATOR = '.';


    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isUpperCase(char c) {
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        return false;
    }


    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static String getDataBaseColumn(String javaName) {
        StringBuilder sb = new StringBuilder();
        char[] javaNames = javaName.toCharArray();
        int i = 0;
        for (char c : javaNames) {
            if (i != 0 && isUpperCase(c)) {
                sb.append("_");
            }
            sb.append((c + "").toLowerCase());
            i++;
        }
        return sb.toString();
    }


    public static String field2JavaCode(String field) {
        String javaCode = field;

        javaCode = javaCode.toLowerCase();
        javaCode = javaCode.trim();

        if (javaCode.contains("_")) {
            String[] codes = javaCode.split("_");
            if (codes.length > 1) {
                for (int i = 1; i < codes.length; i++) {
                    codes[i] = (codes[i].substring(0, 1)).toUpperCase()
                            + codes[i].substring(1);
                }
                javaCode = "";
                for (int i = 0; i < codes.length; i++) {
                    javaCode += codes[i];
                }
            }
            return javaCode;

        }
        return field;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static String[] commaDelimitedListToStringArray(String str) {
        return delimitedListToStringArray(str, ",");
    }

    public static String[] delimitedListToStringArray(String str, String delimiter) {
        return delimitedListToStringArray(str, delimiter, null);
    }

    public static String[] delimitedListToStringArray(
            String str, String delimiter, String charsToDelete) {

        if (str == null) {
            return EMPTY_STRING_ARRAY;
        }
        if (delimiter == null) {
            return new String[]{str};
        }

        List<String> result = new ArrayList<>();
        if (delimiter.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
            }
        } else {
            int pos = 0;
            int delPos;
            while ((delPos = str.indexOf(delimiter, pos)) != -1) {
                result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
                pos = delPos + delimiter.length();
            }
            if (str.length() > 0 && pos <= str.length()) {
                // Add rest of String, but not in case of empty input.
                result.add(deleteAny(str.substring(pos), charsToDelete));
            }
        }
        return toStringArray(result);
    }

    public static String[] toStringArray(Collection<String> collection) {
        return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_STRING_ARRAY) : EMPTY_STRING_ARRAY);
    }

    public static String deleteAny(String inString, String charsToDelete) {
        if (!hasLength(inString) || !hasLength(charsToDelete)) {
            return inString;
        }

        StringBuilder sb = new StringBuilder(inString.length());
        for (int i = 0; i < inString.length(); i++) {
            char c = inString.charAt(i);
            if (charsToDelete.indexOf(c) == -1) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }


    public static void main(String[] args) {
        String a = "AbcA";
        System.out.println(getDataBaseColumn(a));
    }


    public static int getStringCountKey(String str, String key) {
        if (str == null || key == null || "".equals(str.trim()) || "".equals(key.trim())) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(key, index)) != -1) {
            index = index + key.length();
            count++;
        }
        return count;
    }

}
