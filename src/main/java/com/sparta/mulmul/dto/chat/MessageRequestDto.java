package com.sparta.mulmul.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MessageRequestDto {
    // 메시지 타입 : 입장, 채팅
    private String roomId;
    private String message;
    private MessageTypeEnum type;
    private Boolean isRead;
}
