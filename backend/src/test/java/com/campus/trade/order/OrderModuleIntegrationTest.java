package com.campus.trade.order;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderModuleIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createOrderEnforcesVerificationOwnershipAvailabilityAndNoDuplicate() throws Exception {
        Session seller = register("13630000001", "ORDS0101");
        Session buyer = register("13630000002", "ORDB0101");
        Session otherBuyer = register("13630000003", "ORDB0102");
        Session unverified = register("13630000004", null);
        long productId = publishOnSale(seller.token(), "可交易的教材", "PICKUP");

        order(unverified.token(), productId, null, "未认证下单", status().isForbidden());
        order(seller.token(), productId, null, "购买自己的商品", status().isConflict());
        long orderId = order(buyer.token(), productId, null, "正常下单", status().isOk());
        order(buyer.token(), productId, null, "重复下单", status().isConflict());
        order(otherBuyer.token(), productId, null, "商品已锁定", status().isConflict());

        mockMvc.perform(get("/api/order/{id}", orderId).header("Authorization", bearer(otherBuyer.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/order/buy").header("Authorization", bearer(buyer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/order/sell").header("Authorization", bearer(seller.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(orderId));
    }

    @Test
    void strictStateMachineAllowsOnlySequentialBuyerCompletionAndSellsProduct() throws Exception {
        Session seller = register("13630000005", "ORDS0201");
        Session buyer = register("13630000006", "ORDB0201");
        long productId = publishOnSale(seller.token(), "状态流转台灯", "PICKUP");
        long orderId = order(buyer.token(), productId, null, "状态机测试", status().isOk());

        mockMvc.perform(put("/api/order/{id}/status", orderId)
                        .header("Authorization", bearer(buyer.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"COMPLETE\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/order/{id}/status", orderId)
                        .header("Authorization", bearer(seller.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CONFIRM_PICKUP\",\"pickupTime\":\"2026-07-20 14:00\",\"pickupLocation\":\"北校区图书馆\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PICKUP"))
                .andExpect(jsonPath("$.data.pickupLocation").value("北校区图书馆"));
        mockMvc.perform(put("/api/order/{id}/status", orderId)
                        .header("Authorization", bearer(seller.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/order/{id}/status", orderId)
                        .header("Authorization", bearer(buyer.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"COMPLETE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        String productStatus = jdbcTemplate.queryForObject("SELECT status FROM product WHERE id = ?", String.class, productId);
        assertThat(productStatus).isEqualTo("SOLD");
        mockMvc.perform(put("/api/order/{id}/cancel", orderId)
                        .header("Authorization", bearer(buyer.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"不能再取消\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellationRecordsActorAndReasonThenAllowsAnotherBuyer() throws Exception {
        Session seller = register("13630000007", "ORDS0301");
        Session firstBuyer = register("13630000008", "ORDB0301");
        Session secondBuyer = register("13630000009", "ORDB0302");
        long productId = publishOnSale(seller.token(), "可重新下单的耳机", "PICKUP");
        long firstOrderId = order(firstBuyer.token(), productId, null, "第一次", status().isOk());

        mockMvc.perform(put("/api/order/{id}/cancel", firstOrderId)
                        .header("Authorization", bearer(seller.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"商品信息需要调整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelReason").value("商品信息需要调整"))
                .andExpect(jsonPath("$.data.cancelledBy").value(seller.id()));
        long secondOrderId = order(secondBuyer.token(), productId, null, "第二次", status().isOk());
        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
    }

    @Test
    void deliveryRequiresBuyersOwnAddressAndReturnsItInOrder() throws Exception {
        Session seller = register("13630000010", "ORDS0401");
        Session buyer = register("13630000011", "ORDB0401");
        long productId = publishOnSale(seller.token(), "送货交易显示器", "DELIVERY");
        order(buyer.token(), productId, null, "缺少地址", status().isBadRequest());

        long sellerAddress = createAddress(seller.token(), "卖家地址");
        order(buyer.token(), productId, sellerAddress, "越权地址", status().isBadRequest());
        long buyerAddress = createAddress(buyer.token(), "买家地址");
        long orderId = order(buyer.token(), productId, buyerAddress, "请送到宿舍", status().isOk());
        mockMvc.perform(get("/api/order/{id}", orderId).header("Authorization", bearer(buyer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address.contact").value("买家地址"))
                .andExpect(jsonPath("$.data.buyerRemark").value("请送到宿舍"));
    }

    private Session register(String phone, String studentId) throws Exception {
        String suffix = phone.substring(7);
        String student = studentId == null ? "" : ",\"studentId\":\"" + studentId + "\",\"realName\":\"订单测试\"";
        MvcResult result = mockMvc.perform(post("/api/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"888888\",\"password\":\"secret88\","
                                + "\"nickname\":\"订单用户" + suffix + "\"" + student + "}"))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.data.userId");
        return new Session(id.longValue(), JsonPath.read(body, "$.data.token"));
    }

    private long publishOnSale(String token, String title, String tradeType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(productBody(title, tradeType)))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        jdbcTemplate.update("UPDATE product SET status = 'ON_SALE' WHERE id = ?", id.longValue());
        return id.longValue();
    }

    private long order(String token, long productId, Long addressId, String remark,
                       org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        String address = addressId == null ? "" : ",\"addressId\":" + addressId;
        MvcResult result = mockMvc.perform(post("/api/order").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + address + ",\"remark\":\"" + remark + "\"}"))
                .andExpect(expected).andReturn();
        if (result.getResponse().getStatus() != 200) return -1;
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private long createAddress(String token, String contact) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/address").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactName\":\"" + contact + "\",\"contactPhone\":\"13639999999\","
                                + "\"campus\":\"北校区\",\"building\":\"1号楼\",\"room\":\"301\"}"))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private String productBody(String title, String tradeType) {
        return "{\"categoryId\":1,\"title\":\"" + title + "\","
                + "\"description\":\"这是用于验证订单交易流程的完整商品描述。\",\"price\":\"25.00\","
                + "\"originalPrice\":\"50.00\",\"conditionLevel\":\"USED\",\"tradeType\":\""
                + tradeType + "\",\"images\":[{\"url\":\"/uploads/order-product.png\",\"isMain\":true,\"sort\":1}]}";
    }

    private String bearer(String token) { return "Bearer " + token; }

    private record Session(long id, String token) { }
}
