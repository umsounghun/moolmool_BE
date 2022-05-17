package com.sparta.mulmul.service;

import com.sparta.mulmul.dto.NotificationDto;
import com.sparta.mulmul.dto.barter.BarterDto;
import com.sparta.mulmul.dto.barter.BarterStatusDto;
import com.sparta.mulmul.dto.barter.OpponentBarterDto;
import com.sparta.mulmul.model.Barter;
import com.sparta.mulmul.model.Item;
import com.sparta.mulmul.model.Notification;
import com.sparta.mulmul.model.User;
import com.sparta.mulmul.repository.BarterRepository;
import com.sparta.mulmul.repository.ItemRepository;
import com.sparta.mulmul.repository.NotificationRepository;
import com.sparta.mulmul.repository.UserRepository;
import com.sparta.mulmul.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BarterService {
    private final BarterRepository barterRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final NotificationRepository notificationRepository;



    // 성훈 - 거래내역서 보기
    public List<BarterDto> showMyBarter(UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("유저 정보가 없습니다.")
        );

        Long userId = userDetails.getUserId();
        // (거래 물품리스트들과 거래내역의 Id값)이 포함된 거래내역 리스트를 담을 Dto
        List<BarterDto> totalList = new ArrayList<>();
        // 상대방 아이디
        Long opponentId = null;
        // 나의 포지션
        String myPosition = null;
        // 나의 거래완료 여부
        Boolean myTradeCheck;
        // 나의 평가완료 여부
        Boolean myScoreCheck;

        // 유저의 거래내역 리스트를 전부 조회한다
        List<Barter> mybarterList = barterRepository.findAllByBuyerIdOrSellerId(userId, userId);

        // 내가 거래한 거래리스트를 대입한다.
        // barterId, buyerId, SellerId를 분리한다.
        for (Barter barters : mybarterList) {
            Long barterId = barters.getId();
            LocalDateTime date = barters.getModifiedAt();
            // 거래 물품리스트를 담을 Dto -> 내것과 상대것을 담는다
            List<OpponentBarterDto> myBarterList = new ArrayList<>();
            List<OpponentBarterDto> barterList = new ArrayList<>();

            String barter = barters.getBarter();
            //barter 거래내역 id split하기 -> 파싱하여 거래항 물품의 Id값을 찾기
            String[] barterIds = barter.split(";");
            String[] buyerItemIdList = barterIds[0].split(",");
            String[] sellerItemIdList = barterIds[1].split(",");


            // 바이어(유저)의 물품을 찾아서 정보를 넣기
            for (String buyerItemId : buyerItemIdList) {
                Long itemId = Long.parseLong(buyerItemId);
                Item buyerItem = itemRepository.findById(itemId).orElseThrow(
                        () -> new IllegalArgumentException("buyerItem not found")
                );

                OpponentBarterDto buyerItemList = getMyBarterDto(itemId, buyerItem);

                //바이어가 유저라면
                if (buyerItem.getBag().getUserId().equals(userId)) {
                    myBarterList.add(buyerItemList);
                    // 바이어가 유저이기 때문에, 상대방은 셀러가 된다.
                    opponentId = barters.getSellerId();
                    myPosition = "buyer";
                } else {
                    barterList.add(buyerItemList);
                    opponentId = barters.getBuyerId();
                }
            }

            //셀러(유저)의 물품을 찾아서 정보를 넣기
            for (String sellerItemId : sellerItemIdList) {
                Long itemId = Long.parseLong(sellerItemId);
                Item sellerItem = itemRepository.findById(itemId).orElseThrow(
                        () -> new IllegalArgumentException("sellerItem not found")
                );

                OpponentBarterDto sellerItemList = getMyBarterDto(itemId, sellerItem);
                //셀러가 유저라면
                if (sellerItem.getBag().getUserId().equals(userId)) {
                    myBarterList.add(sellerItemList);
                    myPosition = "seller";
                } else {
                    barterList.add(sellerItemList);
                }
            }

            // 상대 유저 정보
            User opponentUser = userRepository.findById(opponentId).orElseThrow(
                    () -> new IllegalArgumentException("유저 정보가 없습니다.")
            );
            // 거래상태 정보 1 : 신청중 / 2 : 거래중 / 3 : 거래완료 / 4 : 평가완료
            int status = barters.getStatus();

            //내포지션이 바이어라면 거래내역의 상태 확인하기
            if (myPosition.equals("buyer")) {
                myTradeCheck = barters.getIsBuyerTrade();
                myScoreCheck = barters.getIsBuyerScore();
                //내포지션이 셀러라면 거래내역의 상태 확인하기
            } else {
                myTradeCheck = barters.getIsSellerTrade();
                myScoreCheck = barters.getIsSellerScore();
            }

            if (status == 2 || status == 1) {
                BarterDto barterFin = new BarterDto(
                        barterId,
                        opponentId,
                        opponentUser.getNickname(),
                        opponentUser.getProfile(),
                        null,
                        status,
                        myPosition,
                        myTradeCheck,
                        myScoreCheck,
                        myBarterList,
                        barterList
                );
                totalList.add(barterFin);
                // 거래완료, 평가완료일 경우
            } else if (status == 3 || status == 4) {
                BarterDto barterFin = new BarterDto(
                        barterId,
                        opponentId,
                        opponentUser.getNickname(),
                        opponentUser.getProfile(),
                        date,
                        status,
                        myPosition,
                        myTradeCheck,
                        myScoreCheck,
                        myBarterList,
                        barterList
                );
                totalList.add(barterFin);
            }
        }
        return totalList;
    }
// 성훈 리팩토링 (거래리스트)
    private OpponentBarterDto getMyBarterDto(Long itemId, Item Item) {
        OpponentBarterDto itemList = new OpponentBarterDto(
                itemId,
                Item.getTitle(),
                Item.getItemImg().split(",")[0],
                Item.getContents()
        );
        return itemList;
    }


    // 교환신청 취소 물건상태 2(교환중) -> 0(물품등록한 상태), 거래내역 삭제
    @Transactional
    public void deleteBarter(Long barterId, UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("유저 정보가 없습니다.")
        );
        Long userId = user.getId();
        Barter mybarter = barterRepository.findById(barterId).orElseThrow(
                () -> new IllegalArgumentException("거래내역이 없습니다."));
        // 거래중인 상태가 아니면 예외처리
        if (mybarter.getStatus() == 1 || mybarter.getStatus() == 2) {
            // 거래외 사람이 취소를 할 경우
            if (!mybarter.getBuyerId().equals(userId) && !mybarter.getSellerId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "올바른 요청이 아닙니다");
            }

            String[] barterIdList = mybarter.getBarter().split(";");
            String[] buyerItemId = barterIdList[0].split(",");
            String sellerItemId = barterIdList[1];

            int setStatus = 0;
            for (String eachBuyer : buyerItemId) {
                Long buyerId = Long.valueOf(eachBuyer);
                Item buyerItem = itemRepository.findById(buyerId).orElseThrow(
                        () -> new IllegalArgumentException("buyerItem not found"));
                buyerItem.statusUpdate(buyerItem.getId(), setStatus);
            }
            //셀러(유저)의 물품을 찾아서 정보를 넣기
            Long sellerId = Long.parseLong(sellerItemId);
            Item sellerItem = itemRepository.findById(sellerId).orElseThrow(
                    () -> new IllegalArgumentException("sellerItem not found")
            );
            sellerItem.statusUpdate(sellerItem.getId(), setStatus);
            // 거래내역 삭제
            barterRepository.deleteById(barterId);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "올바른 요청이 아닙니다");
        }
    }


    // 성훈 - 거래 완료
    @Transactional
    public BarterStatusDto OkayBarter(Long barterId, UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("유저 정보가 없습니다.")
        );
        Long userId = user.getId();
        // 거래내역을 조회한다.
        Barter myBarter = barterRepository.findById(barterId).orElseThrow(() -> new IllegalArgumentException("거래내역이 없습니다."));
        // 거래중인 상태가 아니면 예외처리
        if (myBarter.getStatus() != 2) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "올바른 요청이 아닙니다");
        }

        boolean isTrade;
        String myPosition;
        // 유저가 바이어라면 바이어거래완료를 true로 변경한다.
        if (myBarter.getBuyerId().equals(userId)) {
            if (myBarter.getIsBuyerTrade()) {
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "완료된 거래입니다");
            }
            myBarter.updateTradBuyer(true);
            isTrade = true;
            myPosition = "buyer";
            // 유저가 셀러라면 셀러거래완료를 true로 변경한다.
        } else {
            if (myBarter.getIsSellerTrade()) {
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "완료된 거래입니다");
            }
            myBarter.updateTradSeller(true);
            isTrade = true;
            myPosition = "seller";
        }

        Boolean buyerTrade = myBarter.getIsBuyerTrade();
        Boolean sellerTrade = myBarter.getIsSellerTrade();

        // 바이어와 셀러 모두 거래완료이면 (거래내역과 아이템)의 상태를 거래완료(status : 3)으로 변경
        if (buyerTrade && sellerTrade) {

            String[] barterIdList = myBarter.getBarter().split(";");
            String[] buyerItemId = barterIdList[0].split(",");
            String sellerItemId = barterIdList[1];

            for (String eachBuyer : buyerItemId) {
                Long buyerId = Long.valueOf(eachBuyer);
                Item buyerItem = itemRepository.findById(buyerId).orElseThrow(
                        () -> new IllegalArgumentException("buyerItem not found"));
                buyerItem.statusUpdate(buyerItem.getId(), 3);
            }
            //셀러(유저)의 물품을 찾아서 정보를 넣기
            Long sellerId = Long.parseLong(sellerItemId);
            Item sellerItem = itemRepository.findById(sellerId).orElseThrow(
                    () -> new IllegalArgumentException("sellerItem not found")
            );
            sellerItem.statusUpdate(sellerItem.getId(), 3);
            myBarter.updateTradeBarter(3, LocalDateTime.now());

            return new BarterStatusDto(isTrade,false , myBarter.getStatus());
        }


        if (myPosition.equals("buyer")){
            // 알림 내역 저장 후 상대방에게 전송
            Notification notification = notificationRepository.save(Notification.createOf(myBarter, user.getNickname()));

            messagingTemplate.convertAndSend(
                    "/sub/notification/" + myBarter.getSellerId(), NotificationDto.createFrom(notification)
            );
        } else {
            // 알림 내역 저장 후 상대방에게 전송
            Notification notification = notificationRepository.save(Notification.createOf2(myBarter, user.getNickname()));

            messagingTemplate.convertAndSend(
                    "/sub/notification/" + myBarter.getBuyerId(), NotificationDto.createFrom(notification)
            );
        }

        return new BarterStatusDto(isTrade, false, myBarter.getStatus());
    }
}


