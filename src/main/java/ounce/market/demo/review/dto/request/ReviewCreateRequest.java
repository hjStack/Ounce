package ounce.market.demo.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewCreateRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank String content
) {
    public int getRating() {
        return rating;
    }

    public String getContent(){
        return content;
    }
}