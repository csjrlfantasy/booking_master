package com.ticket.app.common.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;

/**
 * Mainland China 18-digit ID card validation helpers.
 */
public final class IdCardUtils {

    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdCardUtils() {
    }

    public static boolean isValid(String idCardNo) {
        if (idCardNo == null) {
            return false;
        }
        String normalized = normalize(idCardNo);
        if (!normalized.matches("\\d{17}[0-9X]")) {
            return false;
        }
        if (extractBirthday(normalized) == null) {
            return false;
        }
        return expectedCheckCode(normalized) == normalized.charAt(17);
    }

    public static int calculateAge(String idCardNo, LocalDate referenceDate) {
        String normalized = normalize(idCardNo);
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("身份证号不合法");
        }
        LocalDate birthday = extractBirthday(normalized);
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        return Period.between(birthday, referenceDate).getYears();
    }

    public static String mask(String idCardNo) {
        if (idCardNo == null || idCardNo.length() < 10) {
            return idCardNo;
        }
        String normalized = normalize(idCardNo);
        return normalized.substring(0, 6) + "********" + normalized.substring(normalized.length() - 4);
    }

    public static LocalDate extractBirthday(String idCardNo) {
        if (idCardNo == null || idCardNo.length() < 14) {
            return null;
        }
        String normalized = normalize(idCardNo);
        try {
            int year = Integer.parseInt(normalized.substring(6, 10));
            int month = Integer.parseInt(normalized.substring(10, 12));
            int day = Integer.parseInt(normalized.substring(12, 14));
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException | DateTimeException ex) {
            return null;
        }
    }

    private static String normalize(String idCardNo) {
        return idCardNo == null ? "" : idCardNo.trim().toUpperCase();
    }

    private static char expectedCheckCode(String normalized) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (normalized.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CODES[sum % 11];
    }
}
