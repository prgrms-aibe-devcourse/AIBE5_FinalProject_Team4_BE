package com.closetnangam.be.global.external.naver.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 네이버 쇼핑 상품명에서 HTML·뒤쪽 품번/SKU·사이즈 표기를 제거한다.
 */
public final class NaverShoppingTitleSanitizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern TRAILING_ALNUM_SKU = Pattern.compile("\\s+[A-Z]{1,4}\\d{6,12}$");
    private static final Pattern TRAILING_HYPHEN_SKU = Pattern.compile("\\s+[A-Z0-9]{2,}(?:-[A-Z0-9]+)+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_LONG_NUMERIC = Pattern.compile("\\s+\\d{7,}$");
    private static final Pattern TRAILING_SEASON_CODE = Pattern.compile("\\s+(?:SS|FW|AW|HS)\\d{2}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_LATIN_MODEL_TOKENS = Pattern.compile(
            "\\s+(?:(?:SS|FW|AW|HS)\\d{2}\\s+)?[A-Z]{2,}(?:\\s+[A-Z]{2,})*$"
    );
    private static final Pattern TRAILING_SIZE_SUFFIX = Pattern.compile("\\s+(?:[MSXL]{1,2}\\s+)?\\d{2,3}$");
    private static final Pattern TRAILING_SINGLE_LETTER_CODE = Pattern.compile("\\s+[A-Z]$");
    private static final Pattern EMBEDDED_LONG_NUMERIC = Pattern.compile("\\d{7,}");
    private static final List<Pattern> TRAILING_PATTERNS = List.of(
            TRAILING_LONG_NUMERIC,
            TRAILING_ALNUM_SKU,
            TRAILING_HYPHEN_SKU,
            TRAILING_LATIN_MODEL_TOKENS,
            TRAILING_SEASON_CODE,
            TRAILING_SIZE_SUFFIX,
            TRAILING_SINGLE_LETTER_CODE
    );

    private NaverShoppingTitleSanitizer() {
    }

    public record Result(String displayName, String productCode) {
    }

    public static Result sanitize(String rawTitle, String externalProductId) {
        String withoutHtml = stripHtml(rawTitle);
        if (!StringUtils.hasText(withoutHtml)) {
            return new Result(UNKNOWN, fallbackProductCode(externalProductId));
        }

        String working = normalizeSpaces(withoutHtml);
        List<String> removedCodes = new ArrayList<>();

        boolean changed;
        do {
            changed = false;
            for (Pattern pattern : TRAILING_PATTERNS) {
                Matcher matcher = pattern.matcher(working);
                if (!matcher.find()) {
                    continue;
                }
                String token = matcher.group().trim();
                if (StringUtils.hasText(token)) {
                    removedCodes.add(token);
                }
                working = working.substring(0, matcher.start()).trim();
                changed = true;
                break;
            }
        } while (changed);

        String displayName = finalizeDisplayName(working);
        String productCode = resolveProductCode(removedCodes, withoutHtml, externalProductId);
        return new Result(displayName, productCode);
    }

    private static String stripHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return HTML_TAG.matcher(value).replaceAll("").trim();
    }

    private static String normalizeSpaces(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String finalizeDisplayName(String value) {
        if (!StringUtils.hasText(value)) {
            return UNKNOWN;
        }
        return value
                .replaceAll("\\s{2,}", " ")
                .replaceAll("[\\s\\-,:;]+$", "")
                .trim();
    }

    private static String resolveProductCode(List<String> removedCodes, String originalTitle, String externalProductId) {
        for (int index = removedCodes.size() - 1; index >= 0; index--) {
            String candidate = normalizeCodeToken(removedCodes.get(index));
            if (isMeaningfulProductCode(candidate)) {
                return candidate;
            }
        }

        Matcher matcher = EMBEDDED_LONG_NUMERIC.matcher(originalTitle);
        if (matcher.find()) {
            return matcher.group();
        }

        return fallbackProductCode(externalProductId);
    }

    private static String normalizeCodeToken(String token) {
        return token.replaceAll("^\\s+", "").trim();
    }

    private static boolean isMeaningfulProductCode(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        if (TRAILING_HYPHEN_SKU.matcher(" " + token).matches()) {
            return true;
        }
        if (TRAILING_ALNUM_SKU.matcher(" " + token).matches()) {
            return true;
        }
        return token.matches("\\d{7,}");
    }

    private static String fallbackProductCode(String externalProductId) {
        if (StringUtils.hasText(externalProductId)) {
            return "NAVER_" + externalProductId.trim();
        }
        return UNKNOWN;
    }

    private static final String UNKNOWN = "UNKNOWN";
}
