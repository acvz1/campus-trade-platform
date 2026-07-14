package com.campus.trade.favorite.service;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.favorite.dto.FavoriteQueryDTO;
import com.campus.trade.favorite.vo.FavoriteStatusVO;
import com.campus.trade.favorite.vo.FavoriteVO;

public interface FavoriteService {
    FavoriteStatusVO toggle(Long userId, Long productId);

    void remove(Long userId, Long productId);

    FavoriteStatusVO check(Long userId, Long productId);

    PageResult<FavoriteVO> list(Long userId, FavoriteQueryDTO query);
}
