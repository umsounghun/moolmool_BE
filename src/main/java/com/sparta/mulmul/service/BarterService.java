package com.sparta.mulmul.service;

import com.sparta.mulmul.dto.BarterItemResponseDto;
import com.sparta.mulmul.dto.BarterResponseDto;
import com.sparta.mulmul.model.Barter;
import com.sparta.mulmul.model.Item;
import com.sparta.mulmul.repository.BarterRepository;
import com.sparta.mulmul.repository.ItemRepository;
import com.sparta.mulmul.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BarterService {
    private final BarterRepository barterRepository;
    private final ItemRepository itemRepository;

    // 성훈 - 거래내역서 보기
    public List<BarterResponseDto> showMyBarter(UserDetailsImpl userDetails) {
        Long userId = userDetails.getUserId();

        // USERID? 셀러이거나 바이어 일때는 어떻게 구분하지? -> BuyerIdOrSellerId
        // 유저의 거래내역 리스트를 전부 조회한다
        List<Barter> mybarterList = barterRepository.findAllByBuyerIdOrSellerId(userId);
        // (거래 물품리스트들과 거래내역의 Id값)이 포함된 거래내역 리스트를 담을 Dto
        List<BarterResponseDto> barterResponseDtoList = new ArrayList<>();
        // 거래 물품리스트를 담을 Dto
        List<BarterItemResponseDto> barterResponseDtosList = new ArrayList<>();

        // 내가 거래한 거래리스트를 대입한다.
        // barterId, buyerId, SellerId를 분리한다.
        for (Barter barters : mybarterList) {
            Long barterId = barters.getId();
            Long buyerId = barters.getBuyerId();
            Long sellerId = barters.getSellerId();

            //barter 거래내역 id split하기
            String barter = barters.getBarter();
            String[] barterIds = barter.split(";");
            String[] buyerItemIdList = barterIds[0].split(",");
            String[] sellerItemIdList = barterIds[1].split(",");

            //buyer와 seller의 각각의 거래물품을 조회한다.
            List<Item> buyerItemList = itemRepository.findAllByUserIdAndItemId(buyerId, buyerItemIdList);
            List<Item> sellertemList = itemRepository.findAllByUserIdAndItemId(sellerId, sellerItemIdList);

            // 거래 물품리스트를 담을 Dto에 buyer의 거래물품 정보를 넣어준다
            for (Item buyerItems : buyerItemList) {
                Long itemId = buyerItems.getId();
                String title = buyerItems.getTitle();
                String itemImg = buyerItems.getItemImg();
                String tradeAt = "거래일시";
                String status = buyerItems.getStatus();

                BarterItemResponseDto BuyerItemList = new BarterItemResponseDto(itemId, title, itemImg, tradeAt, status);
                barterResponseDtosList.add(BuyerItemList);
            }

            // 거래 물품리스트를 담을 Dto에 seller의 거래물품 정보를 넣어준다.
            for (Item sellerItems : sellertemList) {
                Long itemId = sellerItems.getId();
                String title = sellerItems.getTitle();
                String itemImg = sellerItems.getItemImg();
                String tradeAt = "거래일시";
                String status = sellerItems.getStatus();

                BarterItemResponseDto sellerItemList = new BarterItemResponseDto(itemId, title, itemImg, tradeAt, status);
                barterResponseDtosList.add(sellerItemList);
            }

            // 거래Id와 모든 거래 물품을 넣어준다
            BarterResponseDto barderResponse = new BarterResponseDto(barterId, barterResponseDtosList);
            barterResponseDtoList.add(barderResponse);
        }

        return barterResponseDtoList;
    }
}
