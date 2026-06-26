package com.ndh.ShopTechnology.controller.store;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.store.StoreResponse;
import com.ndh.ShopTechnology.services.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API công khai cho web khách: liệt kê các kho / cửa hàng đang hoạt động để khách
 * chọn store khi checkout (kết hợp với GET /shipping/stores để có phí ship).
 */
@RestController
@RequestMapping("${api.prefix}/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /** Danh sách kho đang hoạt động. */
    @GetMapping
    public ResponseEntity<APIResponse<List<StoreResponse>>> listActive() {
        List<StoreResponse> data = storeService.listActive();
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /**
     * Danh sách kho đang hoạt động mà CÓ ĐỦ tồn cho tất cả sản phẩm yêu cầu.
     * Truyền {@code variantIds} (id biến thể) và/hoặc {@code productIds} (BE quy về biến thể mặc định).
     * Không truyền gì → trả tất cả kho hoạt động.
     */
    @GetMapping("/stocking")
    public ResponseEntity<APIResponse<List<StoreResponse>>> listStocking(
            @RequestParam(value = "variantIds", required = false) List<Long> variantIds,
            @RequestParam(value = "productIds", required = false) List<Long> productIds) {
        List<StoreResponse> data = storeService.listStockingAll(variantIds, productIds);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Chi tiết một kho đang hoạt động. */
    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<StoreResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", storeService.get(id), null, null));
    }
}
