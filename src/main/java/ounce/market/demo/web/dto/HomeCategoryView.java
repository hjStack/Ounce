package ounce.market.demo.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeCategoryView {
    private final int id;
    private final String label;
    private final String title;
    private final boolean timer;
    private final String image;
}
