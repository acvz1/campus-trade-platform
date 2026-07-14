package com.campus.trade.product;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductModuleIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void categoryTreeIsPublicAndUnverifiedUserCannotPublish() throws Exception {
        mockMvc.perform(get("/api/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("教材"));
        mockMvc.perform(get("/api/product/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));

        String token = register("13720000001", null);
        mockMvc.perform(post("/api/product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("不能发布的教材", "12.00", 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void pendingProductIsPrivateThenApprovedProductCanBeFilteredSearchedAndViewed() throws Exception {
        String token = register("13720000002", "PROD0201");
        long productId = publish(token, "高等数学教材", "15.00", 1);

        mockMvc.perform(get("/api/product/{id}", productId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/product/{id}", productId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.images.length()").value(2));

        jdbcTemplate.update("UPDATE product SET status = 'ON_SALE', published_at = CURRENT_TIMESTAMP WHERE id = ?", productId);
        mockMvc.perform(get("/api/product")
                        .param("keyword", "数学")
                        .param("categoryId", "1")
                        .param("minPrice", "10")
                        .param("maxPrice", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(productId))
                .andExpect(jsonPath("$.data.records[0].mainImage").value("/uploads/product-main.png"));

        mockMvc.perform(get("/api/search").param("keyword", "教材"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(productId));
        mockMvc.perform(get("/api/search/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("教材"));

        mockMvc.perform(get("/api/product/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seller.nickname").value("商品用户0002"))
                .andExpect(jsonPath("$.data.viewCount").value(1));
        Integer views = jdbcTemplate.queryForObject("SELECT view_count FROM product WHERE id = ?", Integer.class, productId);
        assertThat(views).isEqualTo(1);
    }

    @Test
    void onlyOwnerCanEditAndShelfTransitionsThenSoftDeleteAreEnforced() throws Exception {
        String owner = register("13720000003", "PROD0301");
        String other = register("13720000004", "PROD0401");
        long productId = publish(owner, "可管理的台灯", "35.00", 3);
        jdbcTemplate.update("UPDATE product SET status = 'ON_SALE' WHERE id = ?", productId);

        mockMvc.perform(put("/api/product/{id}", productId)
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("越权修改台灯", "20.00", 3)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/product/{id}/shelf", productId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onShelf\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF_SHELF"));

        mockMvc.perform(put("/api/product/{id}", productId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("编辑后的宿舍台灯", "30.00", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF_SHELF"));

        mockMvc.perform(put("/api/product/{id}/status", productId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_SALE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/product/{id}", productId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/product/{id}/shelf", productId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onShelf\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/product/{id}", productId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, productId);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void publishValidationAndDailyLimitAreEnforced() throws Exception {
        String token = register("13720000005", "PROD0501");
        mockMvc.perform(post("/api/product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("原价错误商品", "50.00", 2).replace("\"originalPrice\":\"80.00\"", "\"originalPrice\":\"40.00\"")))
                .andExpect(status().isBadRequest());

        for (int index = 1; index <= 5; index++) {
            publish(token, "每日限额商品" + index, new BigDecimal("10.00").add(BigDecimal.valueOf(index)).toString(), 2);
        }
        mockMvc.perform(post("/api/product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("第六件限额商品", "20.00", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("每天最多发布5件商品"));
    }

    @Test
    void combinedFiltersAndPriceSortingAreApplied() throws Exception {
        String token = register("13720000006", "PROD0601");
        long cheapId = publish(token, "便宜的二手耳机", "20.00", 2);
        long expensiveId = publish(token, "更好的二手耳机", "60.00", 2);
        jdbcTemplate.update("UPDATE product SET status = 'ON_SALE' WHERE id IN (?, ?)", cheapId, expensiveId);

        mockMvc.perform(get("/api/search")
                        .param("keyword", "耳机")
                        .param("categoryId", "2")
                        .param("minPrice", "10")
                        .param("maxPrice", "70")
                        .param("conditionLevel", "USED")
                        .param("tradeType", "PICKUP")
                        .param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(expensiveId))
                .andExpect(jsonPath("$.data.records[1].id").value(cheapId));

        mockMvc.perform(get("/api/search").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private String register(String phone, String studentId) throws Exception {
        String suffix = phone.substring(phone.length() - 4);
        String studentPart = studentId == null ? "" : ",\"studentId\":\"" + studentId + "\",\"realName\":\"商品测试\"";
        MvcResult result = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"888888\",\"password\":\"secret88\","
                                + "\"nickname\":\"商品用户" + suffix + "\"" + studentPart + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private long publish(String token, String title, String price, long categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(title, price, categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private String productBody(String title, String price, long categoryId) {
        return "{\"categoryId\":" + categoryId + ",\"title\":\"" + title
                + "\",\"description\":\"这是一段足够详细的商品描述，成色良好可以校内交易。\","
                + "\"price\":\"" + price + "\",\"originalPrice\":\"80.00\","
                + "\"conditionLevel\":\"USED\",\"tradeType\":\"PICKUP\","
                + "\"tradeRemark\":\"图书馆门口自提\",\"images\":["
                + "{\"url\":\"/uploads/product-main.png\",\"isMain\":true,\"sort\":1},"
                + "{\"url\":\"/uploads/product-detail.png\",\"isMain\":false,\"sort\":2}]}";
    }
}
