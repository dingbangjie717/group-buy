package com.groupbuy.module.cart.controller;

import com.groupbuy.common.context.UserContext;
import com.groupbuy.common.result.R;
import com.groupbuy.module.cart.service.CartService;
import com.groupbuy.module.cart.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public R<Void> add(@RequestParam Long goodsId, @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.add(UserContext.requireUserId(), goodsId, quantity);
        return R.ok();
    }

    @PostMapping("/quantity")
    public R<Void> updateQuantity(@RequestParam Long goodsId, @RequestParam Integer quantity) {
        cartService.updateQuantity(UserContext.requireUserId(), goodsId, quantity);
        return R.ok();
    }

    @PostMapping("/check")
    public R<Void> check(@RequestParam Long goodsId, @RequestParam Boolean checked) {
        cartService.check(UserContext.requireUserId(), goodsId, checked);
        return R.ok();
    }

    @DeleteMapping("/{goodsId}")
    public R<Void> remove(@PathVariable Long goodsId) {
        cartService.remove(UserContext.requireUserId(), goodsId);
        return R.ok();
    }

    @GetMapping("/list")
    public R<List<CartItemVO>> list() {
        return R.ok(cartService.list(UserContext.requireUserId()));
    }

    @DeleteMapping("/clear")
    public R<Void> clear() {
        cartService.clear(UserContext.requireUserId());
        return R.ok();
    }
}
