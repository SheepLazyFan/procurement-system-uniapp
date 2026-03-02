package com.procurement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger API 文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("采购系统微信小程序 API")
                        .description("Procurement System UniApp — RESTful API 文档")
                        .version("v1.0")
                        .contact(new Contact().name("Procurement Team")));
    }
}
