package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Phản hồi tiến trình đơn hàng dành cho user FE.
 *
 * <p>Mỗi {@link TimelineStep} đại diện cho một mốc trạng thái.
 * FE render thành dạng thanh tiến trình ngang (stepper), tương tự ảnh mẫu.
 *
 * <p><b>Luồng bình thường:</b>
 * <ol>
 *   <li>Đơn hàng đã đặt       (status = 1)</li>
 *   <li>Đã xác nhận           (status = 2)</li>
 *   <li>Đã giao cho ĐVVC      (status = 3)</li>
 *   <li>Hoàn thành            (status = 4)</li>
 *   <li>Đánh giá              (virtual – không có status code)</li>
 * </ol>
 *
 * <p>Nếu đơn bị hủy, bước cuối là "Đã hủy" (status = 5); bước "Đánh giá"
 * không được hiển thị.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTimelineResponse {

    /** Mã đơn hàng */
    private String orderCode;

    /** Trạng thái hiện tại (1-5) */
    private Integer currentStatus;

    /** Label trạng thái hiện tại */
    private String currentStatusLabel;

    /** Đơn hàng đã kết thúc (hoàn thành hoặc hủy) chưa */
    private Boolean finished;

    /** Danh sách các bước trong tiến trình, theo thứ tự hiển thị */
    private List<TimelineStep> steps;

    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimelineStep {

        /**
         * Số thứ tự bước (1-based), dùng để FE hiển thị đúng vị trí.
         */
        private int stepIndex;

        /**
         * Mã trạng thái tương ứng (1-5).
         * {@code null} cho bước virtual "Đánh giá".
         */
        private Integer statusCode;

        /** Nhãn bước hiển thị cho người dùng */
        private String statusLabel;

        /**
         * Bước này đã được đi qua chưa?
         * {@code true} nếu đơn hàng đã đạt/vượt qua trạng thái này.
         */
        private boolean completed;

        /**
         * Đây có phải trạng thái hiện tại không?
         * Chỉ đúng với một bước duy nhất tại một thời điểm.
         */
        private boolean current;

        /**
         * Thời điểm đơn hàng đạt trạng thái này.
         * {@code null} nếu chưa đạt.
         */
        private Date timestamp;

        /**
         * ID người cập nhật sang trạng thái này.
         * {@code null} nếu hệ thống tự động hoặc bước đầu tiên (người dùng tạo đơn).
         */
        private Long updatedByUserId;

        /** Username của người cập nhật */
        private String updatedByUsername;

        /** Họ tên đầy đủ của người cập nhật */
        private String updatedByFullName;

        /** Ghi chú đi kèm (nếu có) */
        private String note;
    }
}
