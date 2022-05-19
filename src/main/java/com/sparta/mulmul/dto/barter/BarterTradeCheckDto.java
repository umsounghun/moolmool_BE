package com.sparta.mulmul.dto.barter;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BarterTradeCheckDto {
    private Boolean isTrade;


    public BarterTradeCheckDto(Boolean isTrade) {
        this.isTrade = isTrade;
    }
}
