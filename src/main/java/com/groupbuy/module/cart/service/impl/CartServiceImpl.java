package com.groupbuy.module.cart.service.impl;

import com.groupbuy.common.constant.Constants;
import com.groupbuy.common.exception.BizException;
import com.groupbuy.module.cart.service.CartService;
import com.groupbuy.module.cart.vo.CartItemVO;
import com.groupbuy.module.goods.entity.Goods;
import com.groupbuy.module.goods.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 * <p>
 * 为什么用 Redis Hash：
 * - 购物车是「高频读写 + 允许丢失（可从 DB 重建）」的数据，放在 Redis 能显著降低数据库压力
 * - Hash 结构天然支持「按商品 ID 直接改数量」，不用整条覆盖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GoodsMapper goodsMapper;

    /** 购物车 30 天未操作自动过期 */
    private static final long CART_TTL_DAYS = 30;

    @Override
    public void add(Long userId, Long goodsId, Integer quantity) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException("商品不存在或已下架");
        }
        if (goods.getStock() == null || goods.getStock() < quantity) {
            throw new BizException("库存不足");
        }
        String key = cartKey(userId);
        Object exist = redisTemplate.opsForHash().get(key, String.valueOf(goodsId));
        int newQuantity = quantity;
        if (exist instanceof CartItemVO item && item.getQuantity() != null) {
            newQuantity = item.getQuantity() + quantity;
        }
        if (newQuantity > goods.getStock()) {
            throw new BizException("库存不足，购物车最多可放 " + goods.getStock() + " 件");
        }
        redisTemplate.opsForHash().put(key, String.valueOf(goodsId), buildItem(goods, newQuantity, true));
        redisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void updateQuantity(Long userId, Long goodsId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            remove(userId, goodsId);
            return;
        }
        String key = cartKey(userId);
        Object exist = redisTemplate.opsForHash().get(key, String.valueOf(goodsId));
        if (!(exist instanceof CartItemVO item)) {
            throw new BizException("购物车中没有该商品");
        }
        item.setQuantity(quantity);
        redisTemplate.opsForHash().put(key, String.valueOf(goodsId), item);
    }

    @Override
    public void check(Long userId, Long goodsId, Boolean checked) {
        String key = cartKey(userId);
        Object exist = redisTemplate.opsForHash().get(key, String.valueOf(goodsId));
        if (!(exist instanceof CartItemVO item)) {
            return;
        }
        item.setChecked(checked != null && checked);
        redisTemplate.opsForHash().put(key, String.valueOf(goodsId), item);
    }

    @Override
    public void remove(Long userId, Long goodsId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(goodsId));
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey(userId));
        List<CartItemVO> items = new ArrayList<>();
        for (Object value : entries.values()) {
            if (value instanceof CartItemVO item) {
                // 商品可能已下架或改价，返回前用最新数据刷新
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null) {
                    item.setGoodsName(goods.getName());
                    item.setGoodsImage(goods.getImage());
                    item.setPrice(goods.getPrice());
                }
                items.add(item);
            }
        }
        return items.stream()
                .sorted((a, b) -> Long.compare(b.getGoodsId(), a.getGoodsId()))
                .collect(Collectors.toList());
    }

    @Override
    public void clear(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    @Override
    public List<CartItemVO> listCheckedByShop(Long userId, Long shopId) {
        return list(userId).stream()
                .filter(i -> Boolean.TRUE.equals(i.getChecked()))
                .filter(i -> shopId == null || shopId.equals(i.getShopId()))
                .collect(Collectors.toList());
    }

    @Override
    public void removeBatch(Long userId, List<Long> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            return;
        }
        Object[] fields = goodsIds.stream().map(String::valueOf).toArray();
        redisTemplate.opsForHash().delete(cartKey(userId), fields);
    }

    private CartItemVO buildItem(Goods goods, int quantity, boolean checked) {
        return CartItemVO.builder()
                .goodsId(goods.getId())
                .goodsName(goods.getName())
                .goodsImage(goods.getImage())
                .price(goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice())
                .quantity(quantity)
                .checked(checked)
                .shopId(goods.getShopId())
                .build();
    }

    private String cartKey(Long userId) {
        return Constants.KEY_CART + userId;
    }
}
