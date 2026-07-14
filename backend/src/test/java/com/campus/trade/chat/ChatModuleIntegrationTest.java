package com.campus.trade.chat;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatModuleIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createIsIdempotentAndRejectsSelfConversation() throws Exception {
        Session seller = register("13750000001", "聊天卖家");
        Session buyer = register("13750000002", "聊天买家");
        long productId = publishOnSale(seller.token(), "用于私信的教材");

        mockMvc.perform(post("/api/conversation").header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":" + productId + "}"))
                .andExpect(status().isConflict());
        long first = createConversation(buyer.token(), productId);
        long second = createConversation(buyer.token(), productId);
        assertThat(second).isEqualTo(first);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM conversation WHERE product_id = ?",
                Long.class, productId)).isEqualTo(1L);

        mockMvc.perform(get("/api/conversation/{id}", first).header("Authorization", bearer(seller.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productImage").value("/uploads/chat.png"))
                .andExpect(jsonPath("$.data.otherUser.id").value(buyer.id()));
    }

    @Test
    void messagesAreParticipantOnlyChronologicalAndReadingClearsUnread() throws Exception {
        Session seller = register("13750000003", "消息卖家");
        Session buyer = register("13750000004", "消息买家");
        Session stranger = register("13750000005", "路人用户");
        long conversationId = createConversation(buyer.token(), publishOnSale(seller.token(), "消息顺序测试商品"));

        send(buyer.token(), conversationId, "第一条消息", "/api/conversation/" + conversationId + "/message");
        send(seller.token(), conversationId, "第二条回复", "/api/chat/send");
        mockMvc.perform(get("/api/conversation").header("Authorization", bearer(buyer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].lastMessage").value("第二条回复"))
                .andExpect(jsonPath("$.data.records[0].unreadCount").value(1));
        mockMvc.perform(get("/api/conversation/{id}/messages", conversationId)
                        .header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/chat/messages/{id}", conversationId)
                        .header("Authorization", bearer(buyer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].content").value("第一条消息"))
                .andExpect(jsonPath("$.data.records[1].content").value("第二条回复"));
        mockMvc.perform(get("/api/conversation").header("Authorization", bearer(buyer.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].unreadCount").value(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_read FROM message WHERE conversation_id = ? AND sender_id = ?",
                Boolean.class, conversationId, seller.id())).isTrue();
    }

    @Test
    void closedConversationAndInvalidPayloadCannotSend() throws Exception {
        Session seller = register("13750000006", "关闭卖家");
        Session buyer = register("13750000007", "关闭买家");
        long conversationId = createConversation(buyer.token(), publishOnSale(seller.token(), "关闭会话测试商品"));

        mockMvc.perform(post("/api/chat/send").header("Authorization", bearer(buyer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"缺少会话\"}"))
                .andExpect(status().isBadRequest());
        jdbcTemplate.update("UPDATE conversation SET status = 'CLOSED' WHERE id = ?", conversationId);
        mockMvc.perform(post("/api/conversation/{id}/message", conversationId)
                        .header("Authorization", bearer(buyer.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"不能发送\"}"))
                .andExpect(status().isConflict());
    }

    private Session register(String phone, String nickname) throws Exception {
        String studentId = "CHAT" + phone.substring(phone.length() - 7);
        MvcResult result = mockMvc.perform(post("/api/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"888888\","
                                + "\"password\":\"secret88\",\"nickname\":\"" + nickname + "\","
                                + "\"studentId\":\"" + studentId + "\",\"realName\":\"私信测试\"}"))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.userId");
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
        return new Session(id.longValue(), token);
    }

    private long publishOnSale(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"title\":\"" + title + "\","
                                + "\"description\":\"这是用于验证校园私信功能的完整商品描述。\",\"price\":\"20.00\","
                                + "\"conditionLevel\":\"USED\",\"tradeType\":\"PICKUP\","
                                + "\"images\":[{\"url\":\"/uploads/chat.png\",\"isMain\":true,\"sort\":1}]}"))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        jdbcTemplate.update("UPDATE product SET status = 'ON_SALE' WHERE id = ?", id.longValue());
        return id.longValue();
    }

    private long createConversation(String token, long productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/conversation").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":" + productId + "}"))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private void send(String token, long conversationId, String content, String path) throws Exception {
        String body = path.equals("/api/chat/send")
                ? "{\"conversationId\":" + conversationId + ",\"content\":\"" + content + "\"}"
                : "{\"content\":\"" + content + "\"}";
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String bearer(String token) { return "Bearer " + token; }

    private record Session(long id, String token) { }
}
