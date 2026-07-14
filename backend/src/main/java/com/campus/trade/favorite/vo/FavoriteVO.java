package com.campus.trade.favorite.vo;

import java.time.OffsetDateTime;

public record FavoriteVO(Long id, FavoriteProductVO product, OffsetDateTime createdAt) {
}
