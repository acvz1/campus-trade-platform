package com.campus.trade.favorite;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FavoriteModuleIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void toggleCheckListAndExplicitRemovalStayConsistentWithProductCount() throws Exception {
        String seller = register("13540000001", "FAVS0101");
        String user = register("13540000002", "FAVU0101");
        long productId = publishOnSale(seller, "值得收藏的教材");

        mockMvc.perform(get("/api/favorite/check/{id}", productId).header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.favorited").value(false));
        mockMvc.perform(post("/api/favorite/{id}", productId).header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.favorited").value(true));
        mockMvc.perform(get("/api/favorite").header("Authorization", bearer(user)).param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].product.id").value(productId))
                .andExpect(jsonPath("$.data.records[0].product.mainImage").value("/uploads/favorite.png"));
        assertThat(favoriteCount(productId)).isEqualTo(1);

        mockMvc.perform(post("/api/favorite/{id}", productId).header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.favorited").value(false));
        assertThat(favoriteCount(productId)).isZero();

        mockMvc.perform(post("/api/favorite/{id}", productId).header("Authorization", bearer(user)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/favorite/{id}", productId).header("Authorization", bearer(user)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/favorite/{id}", productId).header("Authorization", bearer(user)))
                .andExpect(status().isOk());
        assertThat(favoriteCount(productId)).isZero();
    }

    @Test
    void sellerCannotFavoriteOwnProductAndAuthenticationIsRequired() throws Exception {
        String seller = register("13540000003", "FAVS0201");
        long productId = publishOnSale(seller, "不能自藏的商品");
        mockMvc.perform(post("/api/favorite/{id}", productId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/favorite/{id}", productId).header("Authorization", bearer(seller)))
                .andExpect(status().isConflict());
    }

    private String register(String phone, String studentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"888888\",\"password\":\"secret88\","
                                + "\"nickname\":\"收藏用户\",\"studentId\":\"" + studentId + "\",\"realName\":\"收藏测试\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private long publishOnSale(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"title\":\"" + title + "\","
                                + "\"description\":\"这是一段足够详细并用于收藏测试的商品描述。\",\"price\":\"18.00\","
                                + "\"originalPrice\":\"30.00\",\"conditionLevel\":\"USED\",\"tradeType\":\"PICKUP\","
                                + "\"images\":[{\"url\":\"/uploads/favorite.png\",\"isMain\":true,\"sort\":1}]}"))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        jdbcTemplate.update("UPDATE product SET status = 'ON_SALE' WHERE id = ?", id.longValue());
        return id.longValue();
    }

    private int favoriteCount(long productId) {
        return jdbcTemplate.queryForObject("SELECT favorite_count FROM product WHERE id = ?", Integer.class, productId);
    }

    private String bearer(String token) { return "Bearer " + token; }
}
