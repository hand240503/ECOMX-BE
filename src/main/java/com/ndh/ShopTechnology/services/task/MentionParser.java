package com.ndh.ShopTechnology.services.task;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích parse @mention từ nội dung comment.
 * Format hỗ trợ: @username hoặc @{username} (cho username có khoảng trắng).
 *
 * Ví dụ:  "Nhờ @tran.hung và @{nguyen duc} xem qua"
 *         → {"tran.hung", "nguyen duc"}
 */
public final class MentionParser {

    /** @username — chữ, số, dấu chấm, gạch dưới, gạch ngang */
    private static final Pattern PLAIN   = Pattern.compile("@([\\w.\\-]+)");

    /** @{username với khoảng trắng} */
    private static final Pattern BRACKET = Pattern.compile("@\\{([^}]+)}");

    private MentionParser() {}

    public static Set<String> extractUsernames(String content) {
        Set<String> result = new LinkedHashSet<>();
        if (content == null || content.isBlank()) return result;

        Matcher m1 = BRACKET.matcher(content);
        while (m1.find()) result.add(m1.group(1).trim().toLowerCase());

        // Xóa @{...} để tránh match lại bởi PLAIN
        String cleaned = content.replaceAll("@\\{[^}]+}", "");

        Matcher m2 = PLAIN.matcher(cleaned);
        while (m2.find()) result.add(m2.group(1).trim().toLowerCase());

        return result;
    }
}
