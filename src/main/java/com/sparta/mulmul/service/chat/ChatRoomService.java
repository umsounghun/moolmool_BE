package com.sparta.mulmul.service.chat;

import com.sparta.mulmul.dto.BannedUserDto;
import com.sparta.mulmul.dto.RoomDto;
import com.sparta.mulmul.exception.CustomException;
import com.sparta.mulmul.model.*;

import com.sparta.mulmul.dto.user.UserRequestDto;
import com.sparta.mulmul.dto.chat.*;
import com.sparta.mulmul.repository.NotificationRepository;
import com.sparta.mulmul.repository.UserRepository;
import com.sparta.mulmul.repository.chat.ChatBannedRepository;
import com.sparta.mulmul.repository.chat.ChatMessageRepository;
import com.sparta.mulmul.repository.chat.ChatRoomRepository;
import com.sparta.mulmul.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

import static com.sparta.mulmul.exception.ErrorCode.*;
import static com.sparta.mulmul.service.chat.ChatRoomService.UserTypeEnum.Type.ACCEPTOR;
import static com.sparta.mulmul.service.chat.ChatRoomService.UserTypeEnum.Type.REQUESTER;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final NotificationRepository notificationRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;
    private final ChatBannedRepository bannedRepository;

    // 채팅방 만들기
    @Transactional
    public Long createRoom(UserDetailsImpl userDetails, UserRequestDto requestDto){
        // 유효성 검사
        Long acceptorId = requestDto.getUserId();
        if ( userDetails.getUserId() == acceptorId ) {
            throw new IllegalArgumentException("ChatRoomService: createRoom) 채팅 대상은 자기자신이 될 수 없습니다.");
        }
        // 채팅 상대 찾아오기
        User acceptor = userRepository.findById(acceptorId)
                .orElseThrow( () -> new CustomException(NOT_FOUND_USER)
                );
        User requester = userRepository.findById(userDetails.getUserId())
                .orElseThrow( () -> new CustomException(NOT_FOUND_USER)
                );
        // 채팅방 차단 회원인지 검색
        if (bannedRepository.existsByUsers(acceptor, requester)) {
            throw new CustomException(BANNED_CHAT_USER);
        }
        // 채팅방을 찾아보고, 없을 시 DB에 채팅방 저장, 메시지를 전달할 때 상대 이미지와 프로필 사진을 같이 전달해 줘야 함.
        ChatRoom chatRoom = roomRepository.findByUser(requester, acceptor)
                        .orElseGet( () -> {
                            ChatRoom c = roomRepository.save(ChatRoom.createOf(requester, acceptor));
                            // 채팅방 개설 메시지 생성
                            notificationRepository.save(Notification.createOf(c, acceptor)); // 알림 작성 및 전달
                            messagingTemplate.convertAndSend("/sub/notification/" + acceptorId,
                                    MessageResponseDto.createFrom(
                                            messageRepository.save(ChatMessage.createInitOf(c.getId()))
                                    )
                            );
                            return c;
                        });
        chatRoom.enter(); // 채팅방에 들어간 상태로 변경 -> 람다를 사용해 일괄처리할 방법이 있는지 연구해 보도록 합니다.
        return chatRoom.getId();
    }

    // 방을 나간 상태로 변경하기
    @Transactional
    public void exitRoom(Long id, UserDetailsImpl userDetails){
        // 회원 찾기
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(NOT_FOUND_USER)
                );
        // 채팅방 찾아오기
        ChatRoom chatRoom = roomRepository.findById(id)
                .orElseThrow(() -> new CustomException(NOT_FOUND_CHAT)
                );
        if ( chatRoom.getRequester() == user) { chatRoom.reqOut(true); }
        else if ( chatRoom.getAcceptor() == user) { chatRoom.accOut(true); }
        else { throw new AccessDeniedException("ChatRoomService: '나가기'는 채팅방에 존재하는 회원만 접근 가능한 서비스입니다."); }
        // 채팅방 종료 메시지 전달 및 저장
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoom.getId(),
                MessageResponseDto.createFrom(
                        messageRepository.save(ChatMessage.createOutOf(id, user))
                )
        );
    }

    // 사용자별 채팅방 전체 목록 가져오기
    public List<RoomResponseDto> getRooms(UserDetailsImpl userDetails){
        // 회원 찾기
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow( () -> new CustomException(NOT_FOUND_USER)
                );
        // 방 목록 찾기
        List<RoomDto> dtos = roomRepository.findAllWith(user);
        // 메시지 리스트 만들기
        return getMessages(dtos, userDetails.getUserId());
    }

    // 채팅방 즐겨찾기 추가
    @Transactional
    public void fixedRoom(Long roomId, UserDetailsImpl userDetails){

        // fetchJoin 필요
        ChatRoom chatRoom = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_CHAT)
                );
        String flag;
        if ( chatRoom.getAcceptor().getId() == userDetails.getUserId() ){ flag = ACCEPTOR; }
        else { flag = REQUESTER; }

        chatRoom.fixedRoom(flag);
    }

    public List<RoomResponseDto> getMessages(List<RoomDto> roomDtos, Long userId){

        List<RoomResponseDto> prefix = new ArrayList<>();
        List<RoomResponseDto> suffix = new ArrayList<>();
        List<Long> roomIds = new ArrayList<>();

        for ( RoomDto roomDto : roomDtos ) { roomIds.add(roomDto.getRoomId()); }
        List<UnreadCntDto> cntDtos = new ArrayList<>();

        if ( roomIds.size() > 0 ) {
            cntDtos = messageRepository.getUnreadCnts(roomIds, userId);
        }

        for (RoomDto dto : roomDtos) {
            long unreadCnt = 0;
            // 해당 방의 유저가 나가지 않았을 경우에는 배열에 포함해 줍니다.
            if ( dto.getAccId() == userId ) {
                if (!dto.getAccOut()) { // 만약 Acc(내)가 나가지 않았다면
                    for ( UnreadCntDto cntDto : cntDtos ) {
                        if (cntDto.getRoomId() == dto.getRoomId() ){ unreadCnt = cntDto.getUnreadCnt(); break; } // 채팅 차단에 대한 정보도 함꼐 줘야 합니다.
                    }
                    Boolean isBanned = bannedRepository.existsBy(dto.getAccId(), dto.getReqId());
                    if (dto.getAccFixed()){ prefix.add(RoomResponseDto.createOf(ACCEPTOR, dto, unreadCnt, isBanned)); }
                    else { suffix.add(RoomResponseDto.createOf(ACCEPTOR, dto, unreadCnt, isBanned)); }
                }
            } else if ( dto.getReqId() == userId ){
                if (!dto.getReqOut()) { // 만약 Req(내)가 나가지 않았다면
                    for ( UnreadCntDto cntDto : cntDtos ) {
                        if (cntDto.getRoomId() == dto.getRoomId() ){ unreadCnt = cntDto.getUnreadCnt(); break; }
                    }
                    Boolean isBanned = bannedRepository.existsBy(dto.getAccId(), dto.getReqId());
                    if (dto.getReqFixed()){ prefix.add(RoomResponseDto.createOf(REQUESTER, dto, unreadCnt, isBanned)); }
                    else { suffix.add(RoomResponseDto.createOf(REQUESTER, dto, unreadCnt, isBanned)); }
                }
            }
        }
        prefix.addAll(suffix);
        return prefix;
    }

    // 회원 차단 기능
    public void setBanned(UserDetailsImpl userDetails, Long bannedId){

        User user = userRepository
                .findById(userDetails.getUserId())
                .orElseThrow(() -> new NullPointerException("ChatRoomService: 차단을 요청한 회원이 존재하지 않습니다.")
                );
        User bannedUser = userRepository
                .findById(bannedId)
                .orElseThrow(() -> new NullPointerException("ChatRoomService: 차단이 요청된 회원이 존재하지 않습니다.")
                );

        if ( bannedRepository.existsByUser(user.getId(), bannedUser.getId()) ) {
            throw new AccessDeniedException("ChatRoomService: 이미 차단한 회원입니다.");
        } else {
            bannedRepository.save(ChatBanned.createOf(user, bannedUser));
        }
    }

    // 차단 회원 불러오기
    public List<BannedUserDto> getBanned(UserDetailsImpl userDetails){
        User user = userRepository
                .findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
        List<User> bannedUsers = bannedRepository.findAllMyBannedByUser(user);

        List<BannedUserDto> userDtos = new ArrayList<>();

        for (User banndUser : bannedUsers){ userDtos.add(BannedUserDto.createFrom(banndUser)); }

        return userDtos;
    }

    // 회원 차단 풀기
    @Transactional
    public void releaseBanned(UserDetailsImpl userDetails, Long bannedId){

        User user = userRepository
                .findById(userDetails.getUserId())
                .orElseThrow(() -> new NullPointerException("ChatRoomService: 차단을 요청한 회원이 존재하지 않습니다.")
                );
        User bannedUser = userRepository
                .findById(bannedId)
                .orElseThrow(() -> new NullPointerException("ChatRoomService: 차단이 요청된 회원이 존재하지 않습니다.")
                );

        ChatBanned banned = bannedRepository.findByUsers(user, bannedUser)
                .orElseThrow(() -> new NullPointerException("ChatRoomService: 차단목록이 존재하지 않습니다."));

        banned.releaseBanned();
    }

    public enum UserTypeEnum {
        ACCEPTOR(Type.ACCEPTOR),
        REQUESTER(Type.REQUESTER);

        private final String userType;

        UserTypeEnum(String userType) { this.userType = userType; }

        public static class Type {
            public static final String ACCEPTOR = "ACCEPTOR";
            public static final String REQUESTER = "REQUESTER";
        }
    }
}
