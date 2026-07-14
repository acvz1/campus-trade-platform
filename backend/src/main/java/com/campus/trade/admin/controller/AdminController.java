package com.campus.trade.admin.controller;

import com.campus.trade.admin.dto.AdminProductQueryDTO;
import com.campus.trade.admin.dto.AuditDTO;
import com.campus.trade.admin.dto.ReasonDTO;
import com.campus.trade.admin.service.AdminService;
import com.campus.trade.admin.vo.AdminProductVO;
import com.campus.trade.admin.vo.DashboardVO;
import com.campus.trade.common.annotation.AdminOnly;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.product.vo.ProductStatusVO;
import com.campus.trade.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台")
@AdminOnly
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "商品审核列表")
    @GetMapping({"/product/review", "/product/pending"})
    public Result<PageResult<AdminProductVO>> products(@Valid @ModelAttribute AdminProductQueryDTO query) {
        return Result.success(adminService.listProducts(query));
    }

    @Operation(summary = "审核商品")
    @PostMapping("/product/{id}/audit")
    public Result<ProductStatusVO> audit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                         @Valid @RequestBody AuditDTO dto) {
        return Result.success(adminService.audit(user.userId(), id, dto));
    }

    @Operation(summary = "审核商品（任务书兼容路径）")
    @PostMapping("/product/audit")
    public Result<ProductStatusVO> auditCompatible(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody AuditDTO dto) {
        if (dto.productId() == null || dto.productId() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品ID不能为空");
        }
        return Result.success(adminService.audit(user.userId(), dto.productId(), dto));
    }

    @Operation(summary = "违规下架")
    @PostMapping("/product/{id}/violation")
    public Result<Void> violation(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                  @Valid @RequestBody ReasonDTO dto) {
        adminService.violation(user.userId(), id, dto.reason());
        return Result.success();
    }

    @Operation(summary = "解除违规")
    @PostMapping("/product/{id}/relieve")
    public Result<Void> relieve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                @Valid @RequestBody ReasonDTO dto) {
        adminService.relieveViolation(user.userId(), id, dto.reason());
        return Result.success();
    }

    @Operation(summary = "数据看板概览")
    @GetMapping({"/dashboard/overview", "/dashboard"})
    public Result<DashboardVO> dashboard() {
        return Result.success(adminService.dashboard());
    }
}
