package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.entities.user.AddressType;
import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import com.ndh.ShopTechnology.repository.UserAddressRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Một bản ghi {@link AddressType#WAREHOUSE} trong {@code user_address} để lưu tọa độ kho; ưu tiên hơn {@link DeliveryRoutingProperties} khi routing.
 */
@Component
    @Order(2)
public class DefaultWarehouseAddressLoader implements ApplicationRunner {

    private final UserAddressRepository userAddressRepository;
    private final DeliveryRoutingProperties deliveryRoutingProperties;

    public DefaultWarehouseAddressLoader(
            UserAddressRepository userAddressRepository,
            DeliveryRoutingProperties deliveryRoutingProperties) {
        this.userAddressRepository = userAddressRepository;
        this.deliveryRoutingProperties = deliveryRoutingProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAddressRepository.countByAddressType(AddressType.WAREHOUSE) > 0) {
            return;
        }
        userAddressRepository.save(UserAddressEntity.builder()
                .user(null)
                .addressType(AddressType.WAREHOUSE)
                .addressLine("Kho mặc định")
                .city("Đà Nẵng")
                .state(null)
                .country("Việt Nam")
                .zipCode(null)
                .isDefault(false)
                .latitude(deliveryRoutingProperties.getWarehouseLatitude())
                .longitude(deliveryRoutingProperties.getWarehouseLongitude())
                .distanceToWarehouseMeters(0.0)
                .shippingFeeVnd(0L)
                .build());
    }
}
