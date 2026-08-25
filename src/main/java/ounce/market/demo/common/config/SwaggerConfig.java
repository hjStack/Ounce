package ounce.market.demo.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(new Info()
                        .title("Ounce API")
                        .description("1인 가구 밀키트 구독 서비스, Ounce API 문서")
                        .version("v1.0.0"))
                .tags(List.of(
                        new Tag().name("01. 회원").description("회원가입, 로그인, 로그아웃"),
                        new Tag().name("02. 주문").description("주문 생성 및 조회"),
                        new Tag().name("03. 장바구니").description("장바구니 관련 API"),
                        new Tag().name("04. 상품").description("상품 전체 조회 및 단건 조회 API"),
                        new Tag().name("05. 상품 검색").description("상품 검색 관련 API"),
                        new Tag().name("06. 미드나이트").description("미드나이트 관련 API"),
                        new Tag().name("07. 미드나이트 알림").description("알림 관련 API"),
                        new Tag().name("08. 리뷰").description("리뷰 관련 API"),
                        new Tag().name("09. 쿠폰").description("쿠폰 관련 API"),
                        new Tag().name("10. 배송").description("배송 관련 API"),
                        new Tag().name("11. 포인트").description("포인트 관련 API"),
                        new Tag().name("12. Q&A").description("Q&A 관련 API")
                ))
                .addSecurityItem(securityRequirement)   // ✅ 메서드 호출
                .components(components);                // ✅ 메서드 호출
    }
}
