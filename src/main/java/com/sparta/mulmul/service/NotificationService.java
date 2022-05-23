package com.sparta.mulmul.service;

import com.sparta.mulmul.dto.NotificationDto;
import com.sparta.mulmul.dto.NotificationType;
import com.sparta.mulmul.exception.CustomException;
import com.sparta.mulmul.exception.ErrorCode;
import com.sparta.mulmul.model.ChatRoom;
import com.sparta.mulmul.model.Notification;
import com.sparta.mulmul.repository.NotificationRepository;
import com.sparta.mulmul.repository.chat.ChatRoomRepository;
import com.sparta.mulmul.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

import static com.sparta.mulmul.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatRoomRepository roomRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // 알림 전체 목록
    public List<NotificationDto> getNotification(UserDetailsImpl userDetails){

        List<Notification> notifications = notificationRepository.findAllByUserIdOrderByIdDesc(userDetails.getUserId());
        List<NotificationDto> dtos = new ArrayList<>();

        for (Notification notification : notifications){
            if ( notification.getType() == NotificationType.CHAT ) {
                ChatRoom chatRoom = roomRepository.findByIdFetch(notification.getChangeId())
                        .orElseThrow( () -> new CustomException(NOT_FOUND_CHAT));
                if ( chatRoom.getAcceptor().getId() == userDetails.getUserId() ) {
                    dtos.add(NotificationDto.createOf(notification, chatRoom.getRequester()));
                } else {
                    dtos.add(NotificationDto.createOf(notification, chatRoom.getAcceptor()));
                }
            } else { dtos.add(NotificationDto.createFrom(notification)); }

        }
        return dtos;
    }

    // 읽음 상태 업데이트
    @Transactional
    public void setRead(Long notificationId){
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow( () -> new CustomException(NOT_FOUND_NOTIFICATION));

        notification.setRead();
    }
}
