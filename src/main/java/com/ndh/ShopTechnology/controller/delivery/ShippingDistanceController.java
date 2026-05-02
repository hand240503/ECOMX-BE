package com.ndh.ShopTechnology.controller.delivery;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.delivery.ShippingDistanceResponse;
import com.ndh.ShopTechnology.services.delivery.DeliveryRoutingService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ước lượng quãng đường giao hàng (OSM/OSRM) từ địa chỉ khách chọn tới kho mặc định.
 */
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
}
