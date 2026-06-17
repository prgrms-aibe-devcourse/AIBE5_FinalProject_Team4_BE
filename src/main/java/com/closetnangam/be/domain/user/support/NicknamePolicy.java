package com.closetnangam.be.domain.user.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class NicknamePolicy {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 30;
    public static final String RULE_MESSAGE =
            "닉네임은 영문 소문자, 숫자, 마침표(.), 밑줄(_)만 3~30자로 사용할 수 있으며 처음과 끝은 영문 또는 숫자여야 합니다.";

    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._]{1,28}[a-z0-9]$");

    private NicknamePolicy() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public static void validate(String nickname) {
        if (!isValid(nickname)) {
            throw new IllegalArgumentException(RULE_MESSAGE);
        }
    }

    public static boolean isValid(String nickname) {
        if (nickname == null) {
            return false;
        }
        if (nickname.length() < MIN_LENGTH || nickname.length() > MAX_LENGTH) {
            return false;
        }
        if (!ALLOWED_PATTERN.matcher(nickname).matches()) {
            return false;
        }
        return !nickname.contains("..");
    }

    public static String generateUniqueCandidate(String rawName, NicknameExistsChecker existsChecker) {
        String base = normalize(rawName)
                .replaceAll("[^a-z0-9._]", "")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("^[._]+", "")
                .replaceAll("[._]+$", "");

        if (base.length() < MIN_LENGTH) {
            base = "user";
        }
        if (base.length() > MAX_LENGTH) {
            base = base.substring(0, MAX_LENGTH).replaceAll("[._]+$", "");
        }
        if (!isValid(base)) {
            base = "user";
        }

        String candidate = base;
        while (existsChecker.exists(candidate)) {
            String suffix = "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            int baseMaxLength = MAX_LENGTH - suffix.length();
            String trimmedBase = base.length() > baseMaxLength ? base.substring(0, baseMaxLength) : base;
            trimmedBase = trimmedBase.replaceAll("[._]+$", "");
            if (trimmedBase.length() < MIN_LENGTH) {
                trimmedBase = "user";
            }
            candidate = trimmedBase + suffix;
        }
        return candidate;
    }

    @FunctionalInterface
    public interface NicknameExistsChecker {
        boolean exists(String nickname);
    }
}
