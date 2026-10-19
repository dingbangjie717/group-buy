package com.groupbuy.module.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.groupbuy.module.goods.entity.Goods;

/**
 * 商品服务
 */
public interface GoodsService {

    IPage<Goods> pageGoods(Long shopId, Long categoryId, String keyword, Integer pageNo, Integer pageSize);

    Goods detail(Long id);

    /**
     * 新增 / 修改商品（方法名避开 IService#save，避免返回类型冲突）
     */
    Long saveGoods(Goods goods);

    /**
     * 上架 / 下架
     */
    void updateStatus(Long id, Integer status);

    void remove(Long id);

    /**
     * 查询实时库存：优先读 Redis，Redis 未预热则回源数据库并写入缓存
     */
    Integer stock(Long goodsId);

    /**
     * 预热库存到 Redis
     */
    void warmStock(Long goodsId);
}
