package com.groupbuy.module.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.groupbuy.common.constant.Constants;
import com.groupbuy.common.exception.BizException;
import com.groupbuy.module.goods.entity.Goods;
import com.groupbuy.module.goods.mapper.CategoryMapper;
import com.groupbuy.module.goods.mapper.GoodsMapper;
import com.groupbuy.module.goods.mapper.ShopMapper;
import com.groupbuy.module.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CategoryMapper categoryMapper;
    private final ShopMapper shopMapper;

    @Override
    public IPage<Goods> pageGoods(Long shopId, Long categoryId, String keyword, Integer pageNo, Integer pageSize) {
        IPage<Goods> page = page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Goods>()
                        .eq(shopId != null, Goods::getShopId, shopId)
                        .eq(categoryId != null, Goods::getCategoryId, categoryId)
                        .eq(Goods::getStatus, 1)
                        .like(StringUtils.hasText(keyword), Goods::getName, keyword)
                        .orderByDesc(Goods::getSales));
        fillNames(page.getRecords());
        return page;
    }

    @Override
    public Goods detail(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        fillNames(List.of(goods));
        return goods;
    }

    @Override
    public Long saveGoods(Goods goods) {
        if (goods.getStatus() == null) {
            goods.setStatus(1);
        }
        if (goods.getStock() == null) {
            goods.setStock(0);
        }
        if (goods.getSales() == null) {
            goods.setSales(0);
        }
        saveOrUpdate(goods);
        // 编辑商品后同步刷新 Redis 库存，避免缓存与数据库不一致
        redisTemplate.delete(stockKey(goods.getId()));
        warmStock(goods.getId());
        return goods.getId();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        goods.setStatus(status);
        updateById(goods);
    }

    @Override
    public void remove(Long id) {
        removeById(id);
        redisTemplate.delete(stockKey(id));
    }

    @Override
    public Integer stock(Long goodsId) {
        Object cached = redisTemplate.opsForValue().get(stockKey(goodsId));
        if (cached != null) {
            return Integer.parseInt(String.valueOf(cached));
        }
        // 缓存未命中：回源数据库并预热
        Goods goods = getById(goodsId);
        if (goods == null) {
            return 0;
        }
        warmStock(goodsId);
        return goods.getStock() == null ? 0 : goods.getStock();
    }

    @Override
    public void warmStock(Long goodsId) {
        Goods goods = getById(goodsId);
        if (goods == null) {
            return;
        }
        redisTemplate.opsForValue().set(stockKey(goodsId),
                goods.getStock() == null ? 0 : goods.getStock(), 1, TimeUnit.DAYS);
    }

    private void fillNames(List<Goods> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Goods g : list) {
            if (g.getShopId() != null) {
                var shop = shopMapper.selectById(g.getShopId());
                if (shop != null) {
                    g.setShopName(shop.getName());
                }
            }
            if (g.getCategoryId() != null) {
                var category = categoryMapper.selectById(g.getCategoryId());
                if (category != null) {
                    g.setCategoryName(category.getName());
                }
            }
        }
    }

    private String stockKey(Long goodsId) {
        return Constants.KEY_STOCK + goodsId;
    }
}
