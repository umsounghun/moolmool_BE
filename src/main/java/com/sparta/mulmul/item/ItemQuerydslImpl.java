package com.sparta.mulmul.item;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.mulmul.barter.barterDto.BarterHotItemListDto;
import com.sparta.mulmul.barter.barterDto.BarterItemListDto;
import com.sparta.mulmul.barter.barterDto.QBarterHotItemListDto;
import com.sparta.mulmul.barter.barterDto.QBarterItemListDto;
import com.sparta.mulmul.item.itemDto.ItemUserResponseDto;
import com.sparta.mulmul.item.itemDto.QItemUserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import static com.sparta.mulmul.item.QItem.*;
import static com.sparta.mulmul.item.QScrab.*;

@Repository
public class ItemQuerydslImpl implements ItemQuerydsl {

    @PersistenceContext
    EntityManager em;

    private final JPAQueryFactory queryFactory;

    public ItemQuerydslImpl(JPAQueryFactory jpaQueryFactory) {
        this.queryFactory = jpaQueryFactory;
    }

    @Override
    public Page<Item> findAllItemOrderByCreatedAtDesc(Pageable pageable) {
        List<Item> results = queryFactory
                .selectFrom(item)
                .where(item.status.eq(0).or(item.status.eq(1)))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Item> total = queryFactory
                .selectFrom(item)
                .where(item.status.eq(0).or(item.status.eq(1)))
                .fetch();

        return new PageImpl<>(results, pageable, total.size());
    }

    @Override
    public Page<Item> findAllItemByCategoryOrderByCreatedAtDesc(String category, Pageable pageable) {
        List<Item> results = queryFactory
                .selectFrom(item)
                .where(item.status.eq(0).or(item.status.eq(1)).and(item.category.eq(category)))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Item> total = queryFactory
                .selectFrom(item)
                .where(item.status.eq(0).or(item.status.eq(1)).and(item.category.eq(category)))
                .fetch();

        return new PageImpl<>(results, pageable, total.size());
    }

    @Override
    public List<Item> searchByKeyword(String keyword){
        return  queryFactory
                .selectFrom(item)
                .where(item.title.contains(keyword))
                .orderBy(item.id.desc())
                .fetch();
    }



    // 성훈 - 마이페이지 0-2상태의 아이템정보를 dto에 담는다
    @Override
    public List<ItemUserResponseDto> findByMyPageItems(Long userId) {

        return queryFactory
                .select(new QItemUserResponseDto(
                        item.id,
                        item.itemImg,
                        item.status
                ))
                .from(item)
                .where(
                        item.bag.userId.eq(userId),
                        item.status.between(0, 2))
                .fetch();
    }

    // 성훈 - 찜하기를 한 아이템을 찾는다
    @Override
    public List<ItemUserResponseDto> findByMyScrabItems(Long userId) {

        return queryFactory
                .select(new QItemUserResponseDto(
                        item.id,
                        item.itemImg,
                        item.status
                ))
                .from(item)
                .join(scrab1).on(scrab1.itemId.eq(item.id))
                .fetchJoin()
                .distinct()
                .where(scrab1.userId.eq(userId), scrab1.scrab.eq(true))
                .orderBy(scrab1.modifiedAt.desc())
                .limit(3)
                .fetch();
    }

//     개래내역보기
    @Override
    public BarterItemListDto findByBarterItems(Long itemId) {
        return queryFactory
                .select(new QBarterItemListDto(
                        item.id,
                        item.title,
                        item.itemImg,
                        item.contents
                ))
                .from(item)
                .where(item.id.eq(itemId))
                .fetchOne();
    }
    // 떠로으는 거래
    @Override
    public BarterHotItemListDto findByHotBarterItems(Long itemId) {
        return queryFactory
                .select(new QBarterHotItemListDto(
                        item.id,
                        item.title,
                        item.itemImg,
                        item.contents,
                        item.status
                ))
                .from(item)
                .where(item.id.eq(itemId))
                .fetchOne();
    }
}
