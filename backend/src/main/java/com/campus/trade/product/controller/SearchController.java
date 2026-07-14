package com.campus.trade.product.controller;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.product.dto.ProductSearchDTO;
import com.campus.trade.product.service.SearchService;
import com.campus.trade.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品搜索")
@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "多条件搜索商品")
    @GetMapping
    public Result<PageResult<ProductVO>> search(@Valid @ModelAttribute ProductSearchDTO query) {
        return Result.success(searchService.search(query));
    }

    @Operation(summary = "热门搜索词")
    @GetMapping("/hot")
    public Result<List<String>> hotKeywords() {
        return Result.success(searchService.hotKeywords());
    }
}
