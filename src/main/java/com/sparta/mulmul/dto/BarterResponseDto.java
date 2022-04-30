package com.sparta.mulmul.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BarterResponseDto {
    private Long barterId;
    private LocalDateTime date;
    private List<MyBarterDto> myItem;
    private List<MyBarterDto> barterItem;


    public  BarterResponseDto(Long barterId, LocalDateTime date, List<MyBarterDto> myItem, List<MyBarterDto> barterItem) {
        this.barterId = barterId;
        this.date = date;
        this.myItem = myItem;
        this.barterItem = barterItem;
    }
}
