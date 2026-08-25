package ounce.market.demo.review.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "08. 리뷰", description = "리뷰 작성/조회/수정/삭제")
@Slf4j
@RestController
@RequestMapping("/api/review")
public class ReviewController {
}
