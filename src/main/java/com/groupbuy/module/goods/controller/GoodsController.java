package com.groupbuy.module.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.groupbuy.common.result.PageResult;
import com.groupbuy.common.result.R;
import com.groupbuy.module.goods.entity.Category;
import com.groupbuy.module.goods.entity.Goods;
import com.groupbuy.module.goods.entity.Shop;
import com.groupbuy.module.goods.mapper.CategoryMapper;
import com.groupbuy.module.goods.mapper.ShopMapper;
import com.groupbuy.module.goods.service.GoodsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品 / 店铺 / 分类接口
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;
    private final ShopMapper shopMapper;
    private final CategoryMapper categoryMapper;

    @GetMapping("/goods/list")
    public R<PageResult<Goods>> list(@RequestParam(required = false) Long shopId,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "1") Integer pageNo,
                                     @RequestParam(defaultValue = "12") Integer pageSize) {
        IPage<Goods> page = goodsService.pageGoods(shopId, categoryId, keyword, pageNo, pageSize);
        return R.ok(PageResult.of(page));
    }

    @GetMapping("/goods/{id}/detail")
    public R<Goods> detail(@PathVariable Long id) {
        return R.ok(goodsService.detail(id));
    }

    @GetMapping("/goods/{id}/stock")
    public R<Integer> stock(@PathVariable Long id) {
        return R.ok(goodsService.stock(id));
    }

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @PostMapping("/merchant/goods/save")
    public R<Long> save(@RequestBody Goods goods) {
        return R.ok("保存成功", goodsService.saveGoods(goods));
    }

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @PostMapping("/merchant/goods/status")
    public R<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        goodsService.updateStatus(id, status);
        return R.ok();
    }

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @DeleteMapping("/merchant/goods/{id}")
    public R<Void> remove(@PathVariable Long id) {
        goodsService.remove(id);
        return R.ok();
    }

    @GetMapping("/shop/list")
    public R<List<Shop>> shops() {
        return R.ok(shopMapper.selectList(new LambdaQueryWrapper<Shop>().eq(Shop::getStatus, 1)));
    }

    @GetMapping("/category/list")
    public R<List<Category>> categories(@RequestParam(required = false) Long shopId) {
        if (shopId == null) {
            return R.ok(categoryMapper.selectList(new LambdaQueryWrapper<Category>()));
        }
        return R.ok(categoryMapper.selectByShopId(shopId));
    }
}
