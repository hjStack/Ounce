package ounce.market.demo.subscription.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SubscriptionCreateRequest(

        @Min(value = 4, message = "주당 끼수는 4끼 이상이어야 합니다.")
        @Max(value = 7, message = "주당 끼수는 7끼 이하여야 합니다.")
        int mealsPerWeek,

        @NotNull(message = "구독 시작일은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate) {
}