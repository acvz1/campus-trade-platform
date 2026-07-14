package com.campus.trade.product.controller;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.product.dto.ProductPublishDTO;
import com.campus.trade.product.dto.ProductSearchDTO;
import com.campus.trade.product.dto.ProductShelfDTO;
import com.campus.trade.product.dto.ProductStatusDTO;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.vo.ProductStatusVO;
import com.campus.trade.product.vo.ProductVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品")
@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "公开商品列表与筛选")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@Valid @ModelAttribute ProductSearchDTO query) {
        return Result.success(productService.list(query));
    }

    @Operation(summary = "我的发布")
    @GetMapping("/my")
    public Result<PageResult<ProductVO>> listMine(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @ModelAttribute ProductSearchDTO query) {
        return Result.success(productService.listMine(user.userId(), query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(productService.getDetail(id, user == null ? null : user.userId()));
    }

    @Operation(summary = "发布商品")
    @PostMapping
    public Result<ProductVO> publish(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody ProductPublishDTO dto) {
        return Result.success(productService.publish(user.userId(), dto));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<ProductVO> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                    @Valid @RequestBody ProductPublishDTO dto) {
        return Result.success(productService.update(user.userId(), id, dto));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        productService.delete(user.userId(), id);
        return Result.success();
    }

    @Operation(summary = "商品上架或下架")
    @PutMapping("/{id}/shelf")
    public Result<ProductStatusVO> changeShelf(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ProductShelfDTO dto) {
        return Result.success(productService.changeShelf(user.userId(), id, dto.onShelf()));
    }

    @Operation(summary = "商品上架或下架（任务书兼容路径）")
    @PutMapping("/{id}/status")
    public Result<ProductStatusVO> changeStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable Long id,
                                                @Valid @RequestBody ProductStatusDTO dto) {
        return Result.success(productService.changeShelf(user.userId(), id, "ON_SALE".equals(dto.status())));
    }
}
