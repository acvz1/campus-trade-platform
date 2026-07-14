package com.campus.trade.favorite.controller;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.favorite.dto.FavoriteQueryDTO;
import com.campus.trade.favorite.service.FavoriteService;
import com.campus.trade.favorite.vo.FavoriteStatusVO;
import com.campus.trade.favorite.vo.FavoriteVO;
import com.campus.trade.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏")
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "收藏或取消收藏")
    @PostMapping("/{productId}")
    public Result<FavoriteStatusVO> toggle(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable Long productId) {
        return Result.success(favoriteService.toggle(user.userId(), productId));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long productId) {
        favoriteService.remove(user.userId(), productId);
        return Result.success();
    }

    @Operation(summary = "检查收藏状态")
    @GetMapping("/check/{productId}")
    public Result<FavoriteStatusVO> check(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long productId) {
        return Result.success(favoriteService.check(user.userId(), productId));
    }

    @Operation(summary = "我的收藏")
    @GetMapping
    public Result<PageResult<FavoriteVO>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @ModelAttribute FavoriteQueryDTO query) {
        return Result.success(favoriteService.list(user.userId(), query));
    }
}
