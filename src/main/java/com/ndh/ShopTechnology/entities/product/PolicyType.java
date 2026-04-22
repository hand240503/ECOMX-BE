package com.ndh.ShopTechnology.entities.product;

/**
 * Loại chính sách — ý nghĩa {@link PolicyEntity#getNumericValue()} / {@link PolicyEntity#getTextValue()} tùy loại.
 */
public enum PolicyType {

  /** Miễn phí vận chuyển khi giá trị đơn đạt tối thiểu; dùng {@code numericValue} là ngưỡng (vd 45000). */
  FREE_SHIPPING_MIN_ORDER,

  /** Giảm giá cố định (VNĐ); {@code numericValue} = số tiền giảm (vd 10000). */
  FIXED_AMOUNT_DISCOUNT,

  /** Giảm theo %; {@code numericValue} = phần trăm (vd 10 = 10%). */
  PERCENT_DISCOUNT,

  /** Đổi trả trong N ngày; {@code numericValue} = số ngày. */
  RETURN_PERIOD_DAYS,

  /** Bảo hành / cam kết dạng chữ; ưu tiên {@code textValue} hoặc {@code detail}. */
  WARRANTY_OR_NOTE,

  /** Ghi chú hiển thị tự do; dùng {@code detail} / {@code textValue}. */
  CUSTOM
}
