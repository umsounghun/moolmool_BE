package com.sparta.mulmul.dto.barter;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
public class OpponentBarterDto {
    private Long itemId;
    private String title;
    private String itemImg;
    private String contents;

    //성훈 - 거래내역
    public OpponentBarterDto(Long itemId, String title, String itemImg, String contents) {
        this.itemId = itemId;
        this.title = title;
        this.itemImg = itemImg;
        this.contents = contents;
    }
}



