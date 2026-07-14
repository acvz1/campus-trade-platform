package com.campus.trade.admin.service;

import com.campus.trade.admin.dto.AdminProductQueryDTO;
import com.campus.trade.admin.dto.AuditDTO;
import com.campus.trade.admin.vo.AdminProductVO;
import com.campus.trade.admin.vo.DashboardVO;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.product.vo.ProductStatusVO;

public interface AdminService {
    PageResult<AdminProductVO> listProducts(AdminProductQueryDTO query);
    ProductStatusVO audit(Long operatorId, Long productId, AuditDTO dto);
    void violation(Long operatorId, Long productId, String reason);
    void relieveViolation(Long operatorId, Long productId, String reason);
    DashboardVO dashboard();
}
