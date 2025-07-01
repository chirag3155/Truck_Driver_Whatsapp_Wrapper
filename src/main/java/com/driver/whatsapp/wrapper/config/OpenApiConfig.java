package com.driver.whatsapp.wrapper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import java.util.List;

/**
 * OpenAPI configuration for Truck Driver WhatsApp Wrapper
 * Provides Swagger UI documentation for all REST endpoints
 */
@Configuration
@OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "Truck Driver WhatsApp Wrapper API",
        version = "1.0.0",
        description = "API Documentation for Truck Driver WhatsApp Wrapper - Facilitates WhatsApp communication between TruKKer and truck drivers via Infobip"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "Bearer",
    in = SecuritySchemeIn.HEADER,
    description = "JWT Authorization header using the Bearer scheme. Example: 'Bearer {token}'"
)
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Truck Driver WhatsApp Wrapper API")
                        .version("1.0.0")
                        .description("API Documentation for Truck Driver WhatsApp Wrapper Service\n\n" +
                                "This service facilitates WhatsApp communication between TruKKer logistics platform and truck drivers using Infobip's WhatsApp Business API.\n\n" +
                                "**Main Features:**\n" +
                                "- Receive truck driver details from TruKKer\n" +
                                "- Send WhatsApp messages to drivers via Infobip API\n" +
                                "- Process driver responses through webhooks\n" +
                                "- Update TruKKer system with driver status and ETA changes\n\n" +
                                "**Message Flow:**\n" +
                                "1. TruKKer sends driver details to `/api/trukker/driver-details`\n" +
                                "2. System sends WhatsApp message to driver via Infobip\n" +
                                "3. Driver responds via WhatsApp\n" +
                                "4. Infobip forwards response to `/whatsapp/callback`\n" +
                                "5. System processes response and updates TruKKer\n\n" +
                                "**External Dependencies:**\n" +
                                "- Infobip WhatsApp Business API\n" +
                                "- TruKKer API")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("support@example.com")
                                .url("https://github.com/your-repo"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("https://api.yourdomain.com")
                                .description("Production Server")
                ));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("truck-driver-whatsapp")
                .displayName("Truck Driver WhatsApp API")
                .packagesToScan("com.driver.whatsapp.wrapper.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi truKKerApi() {
        return GroupedOpenApi.builder()
                .group("trukker-integration")
                .displayName("TruKKer Integration API")
                .packagesToScan("com.driver.whatsapp.wrapper.controller")
                .pathsToMatch("/wawrapper/**")
                .build();
    }

    @Bean
    public GroupedOpenApi whatsAppApi() {
        return GroupedOpenApi.builder()
                .group("whatsapp-webhooks")
                .displayName("WhatsApp Webhook API")
                .packagesToScan("com.driver.whatsapp.wrapper.controller")
                .pathsToMatch("/wawrapper/whatsapp/**")
                .build();
    }
} 