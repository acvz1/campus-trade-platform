package com.campus.trade.common.utils;

import java.util.regex.Pattern;

public final class RegexUtils {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,20}$");

    private RegexUtils() {
    }

    public static boolean isPhone(String value) {
        return value != null && PHONE_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isStudentId(String value) {
        return value != null && STUDENT_ID_PATTERN.matcher(value.trim()).matches();
    }
}
