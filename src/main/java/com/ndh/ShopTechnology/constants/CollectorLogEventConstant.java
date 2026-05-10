package com.ndh.ShopTechnology.constants;

import java.util.Set;

/**
 * Các giá trị {@code event} được phép ghi vào {@code collector_log} và dùng cho gợi ý / popularity.
 */
public final class CollectorLogEventConstant {

    public static final String DETAILS = "details";
    public static final String MORE_DETAILS = "moreDetails";
    public static final String BUY = "buy";

    public static final Set<String> ALLOWED_EVENTS = Set.of(DETAILS, MORE_DETAILS, BUY);

    private CollectorLogEventConstant() {
    }
}
