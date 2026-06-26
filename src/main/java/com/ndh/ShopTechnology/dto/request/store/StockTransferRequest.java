package com.ndh.ShopTechnology.dto.request.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/** Phiếu chuyển kho: xuất từ {@code fromStoreId} sang {@code toStoreId}. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockTransferRequest {

    @NotNull(message = "fromStoreId là bắt buộc")
    private Long fromStoreId;

    @NotNull(message = "toStoreId là bắt buộc")
    private Long toStoreId;

    @NotEmpty(message = "Danh sách sản phẩm chuyển không được rỗng")
    @Valid
    private List<Item> items;

    private String note;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Item {
        @NotNull(message = "variantId là bắt buộc")
        private Long variantId;

        @NotNull(message = "quantity là bắt buộc")
        @Min(value = 1, message = "Số lượng chuyển phải >= 1")
        private Integer quantity;
    }
}
