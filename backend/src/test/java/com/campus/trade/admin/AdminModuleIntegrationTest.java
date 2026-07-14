package com.campus.trade.admin;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminModuleIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void onlyAdminCanReviewAndAuditWritesTraceableLog() throws Exception {
        String admin = createAdminAndLogin("13860000001");
        String seller = register("13860000002", "ADMSELL01");
        long productId = publishPending(seller, "等待审核的校园教材");

        mockMvc.perform(get("/api/admin/product/review").header("Authorization", bearer(seller)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/product/pending").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(productId))
                .andExpect(jsonPath("$.data.records[0].seller.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].mainImage").value("/uploads/admin.png"));
        mockMvc.perform(post("/api/admin/product/{id}/audit", productId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"REJECT\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/product/{id}/audit", productId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ON_SALE"));

        assertThat(jdbcTemplate.queryForObject("SELECT action FROM audit_log WHERE product_id = ?",
                String.class, productId)).isEqualTo("APPROVE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM product WHERE id = ?",
                String.class, productId)).isEqualTo("ON_SALE");
    }

    @Test
    void compatibilityAuditAndDashboardExposeRequiredStatistics() throws Exception {
        String admin = createAdminAndLogin("13860000003");
        String seller = register("13860000004", "ADMSELL02");
        long productId = publishPending(seller, "需要驳回的商品信息");

        mockMvc.perform(post("/api/admin/product/audit").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"action\":\"REJECT\",\"reason\":\"图片信息不完整\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalProducts").isNumber())
                .andExpect(jsonPath("$.data.totalOrders").isNumber())
                .andExpect(jsonPath("$.data.todayNewUsers").isNumber())
                .andExpect(jsonPath("$.data.todayNewProducts").isNumber())
                .andExpect(jsonPath("$.data.todayNewOrders").isNumber())
                .andExpect(jsonPath("$.data.pendingReviewProducts").isNumber());
    }

    @Test
    void violationAndReliefRequireCorrectStateAndCreateTwoAuditEntries() throws Exception {
        String admin = createAdminAndLogin("13860000005");
        String seller = register("13860000006", "ADMSELL03");
        long productId = publishPending(seller, "违规处置测试商品");
        auditApprove(admin, productId);

        mockMvc.perform(post("/api/admin/product/{id}/violation", productId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"发布违禁商品\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/product/{id}/relieve", productId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"复核后确认合规\"}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log WHERE product_id = ?",
                Long.class, productId)).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM product WHERE id = ?",
                String.class, productId)).isEqualTo("ON_SALE");
    }

    private String createAdminAndLogin(String phone) throws Exception {
        jdbcTemplate.update("INSERT INTO app_user(phone, nickname, password_hash, role, status, created_at, updated_at) "
                        + "VALUES (?, '平台管理员', ?, 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                phone, passwordEncoder.encode("admin888"));
        MvcResult result = mockMvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"admin888\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN")).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String register(String phone, String studentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"888888\",\"password\":\"secret88\","
                                + "\"nickname\":\"审核卖家\",\"studentId\":\"" + studentId + "\",\"realName\":\"审核测试\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private long publishPending(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"title\":\"" + title + "\","
                                + "\"description\":\"这是用于验证管理端商品审核功能的完整描述。\",\"price\":\"32.00\","
                                + "\"conditionLevel\":\"LIKE_NEW\",\"tradeType\":\"PICKUP\","
                                + "\"images\":[{\"url\":\"/uploads/admin.png\",\"isMain\":true,\"sort\":1}]}"))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private void auditApprove(String admin, long productId) throws Exception {
        mockMvc.perform(post("/api/admin/product/{id}/audit", productId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk());
    }

    private String bearer(String token) { return "Bearer " + token; }
}
