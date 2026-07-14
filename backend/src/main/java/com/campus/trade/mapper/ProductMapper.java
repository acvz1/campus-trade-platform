package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Update("UPDATE product SET view_count = view_count + 1 WHERE id = #{id} AND deleted_at IS NULL")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE product SET favorite_count = favorite_count + 1 WHERE id = #{id} AND deleted_at IS NULL")
    int incrementFavoriteCount(@Param("id") Long id);

    @Update("UPDATE product SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = #{id} AND deleted_at IS NULL")
    int decrementFavoriteCount(@Param("id") Long id);
}
