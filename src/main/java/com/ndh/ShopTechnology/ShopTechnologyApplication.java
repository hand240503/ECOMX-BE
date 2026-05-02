package com.ndh.ShopTechnology;

import com.ndh.ShopTechnology.config.DeliveryRoutingProperties;
import com.ndh.ShopTechnology.config.RecommendationBlendProperties;
import com.ndh.ShopTechnology.config.VnpayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        RecommendationBlendProperties.class,
        VnpayProperties.class,
        DeliveryRoutingProperties.class
})
public class ShopTechnologyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopTechnologyApplication.class, args);
	}

}
