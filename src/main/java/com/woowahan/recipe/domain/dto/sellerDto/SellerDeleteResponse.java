package com.woowahan.recipe.domain.dto.sellerDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SellerDeleteResponse {
    private Long id;
    private String message;
}