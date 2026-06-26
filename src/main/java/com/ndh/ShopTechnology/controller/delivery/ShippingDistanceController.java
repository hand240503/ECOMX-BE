package com.ndh.ShopTechnology.controller.delivery;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.delivery.ShippingDistanceResponse;
import com.ndh.ShopTechnology.dto.response.delivery.ShippingStoreOptionResponse;
import com.ndh.ShopTechnology.services.delivery.DeliveryRoutingService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/shipping")
@Validated
public class ShippingDistanceController {

    private final DeliveryRoutingService deliveryRoutingService;

    public ShippingDistanceController(DeliveryRoutingService deliveryRoutingService) {
        this.deliveryRoutingService = deliveryRoutingService;
    }

    @GetMapping("/distance-to-warehouse")
    public ResponseEntity<APIResponse<ShippingDistanceResponse>> distanceToWarehouse(
            @RequestParam @NotBlank(message = "address is required") @Size(max = 500) String address) {
        ShippingDistanceResponse data = deliveryRoutingService.distanceFromAddressToWarehouse(address);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /**
     * Danh sách kho (store) đang hoạt động kèm khoảng cách & phí ship tới {@code address}.
     * Khách dùng để chọn store khi checkout.
     */
    @GetMapping("/stores")
    public ResponseEntity<APIResponse<List<ShippingStoreOptionResponse>>> storeOptions(
            @RequestParam @NotBlank(message = "address is required") @Size(max = 500) String address) {
        List<ShippingStoreOptionResponse> data = deliveryRoutingService.storeShippingOptions(address);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Khoảng cách & phí ship từ một kho cụ thể tới địa chỉ giao. */
    @GetMapping("/distance-to-store")
    public ResponseEntity<APIResponse<ShippingDistanceResponse>> distanceToStore(
            @RequestParam @NotNull(message = "storeId is required") Long storeId,
            @RequestParam @NotBlank(message = "address is required") @Size(max = 500) String address) {
        ShippingDistanceResponse data = deliveryRoutingService.distanceFromStoreToAddress(storeId, address);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }
}
