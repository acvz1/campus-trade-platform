package com.campus.trade.user;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserModuleIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void registerHashesPasswordCreatesAuthRecordsAndIssuesUsableJwt() throws Exception {
        String token = register("13810000001", "STU00101", "测试用户");

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE phone = ?", String.class, "13810000001");
        assertThat(passwordHash).startsWith("$2").isNotEqualTo("secret88");
        Integer authCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_auth ua JOIN app_user u ON u.id = ua.user_id WHERE u.phone = ?",
                Integer.class, "13810000001");
        assertThat(authCount).isEqualTo(2);

        mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("138****0001"))
                .andExpect(jsonPath("$.data.studentVerified").value(true))
                .andExpect(jsonPath("$.data.studentId").value("STU00101"));

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("13810000001", "STU00102", "另一个用户")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void passwordLoginAndProfileUpdateWorkWhileWrongPasswordIsRejected() throws Exception {
        register("13810000002", "STU00201", "旧昵称");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13810000002\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String token = login("13810000002");
        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\",\"contactPhone\":\"13910000002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.contactPhone").value("13910000002"));

        String nickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM app_user WHERE phone = ?", String.class, "13810000002");
        assertThat(nickname).isEqualTo("新昵称");
    }

    @Test
    void addressesAreOwnerScopedSupportDefaultSwitchAndSoftDelete() throws Exception {
        String ownerToken = register("13810000003", "STU00301", "地址用户");
        String otherToken = register("13810000004", "STU00401", "其他用户");

        long firstId = createAddress(ownerToken, "张三", "北校区", "1号楼", false);
        long secondId = createAddress(ownerToken, "李四", "南校区", "2号楼", true);

        mockMvc.perform(get("/api/user/address").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(secondId))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));

        mockMvc.perform(put("/api/user/address/{id}", firstId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressBody("越权修改", "东校区", "3号楼", false)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/user/address/{id}", secondId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/address").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstId))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));

        Integer deletedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_address WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, secondId);
        assertThat(deletedCount).isEqualTo(1);
    }

    @Test
    void smsCodeIsFixedForMvpAndSendingIsRateLimited() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("13810000005", "STU00501", "验证码用户").replace("888888", "111111")))
                .andExpect(status().isBadRequest());

        String smsBody = "{\"phone\":\"13810000005\",\"scene\":\"REGISTER\"}";
        mockMvc.perform(post("/api/user/sms/send").contentType(MediaType.APPLICATION_JSON).content(smsBody))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/sms/send").contentType(MediaType.APPLICATION_JSON).content(smsBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void optionalStudentAuthSmsLoginPasswordAndPhoneChangesCompleteAccountLifecycle() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13810000006\",\"code\":\"888888\","
                                + "\"password\":\"secret88\",\"nickname\":\"待认证用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentVerified").value(false))
                .andReturn();
        String token = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/user/auth/student")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"STU00601\",\"realName\":\"认证用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authStatus").value("VERIFIED"));

        mockMvc.perform(post("/api/user/login/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13810000006\",\"smsCode\":\"888888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentVerified").value(true));

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"secret88\",\"newPassword\":\"newSecret99\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13810000006\",\"password\":\"newSecret99\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/user/phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPhone\":\"13910000006\",\"smsCode\":\"888888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("139****0006"));
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13910000006\",\"password\":\"newSecret99\"}"))
                .andExpect(status().isOk());
    }

    private String register(String phone, String studentId, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(phone, studentId, nickname)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentVerified").value(true))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String login(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"secret88\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private long createAddress(String token, String contact, String campus, String building, boolean isDefault)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/address")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressBody(contact, campus, building, isDefault)))
                .andExpect(status().isOk())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private String registerBody(String phone, String studentId, String nickname) {
        return "{\"phone\":\"" + phone + "\",\"smsCode\":\"888888\",\"password\":\"secret88\","
                + "\"nickname\":\"" + nickname + "\",\"studentId\":\"" + studentId
                + "\",\"realName\":\"测试姓名\"}";
    }

    private String addressBody(String contact, String campus, String building, boolean isDefault) {
        return "{\"contactName\":\"" + contact + "\",\"contactPhone\":\"13810009999\","
                + "\"campus\":\"" + campus + "\",\"building\":\"" + building
                + "\",\"room\":\"301\",\"isDefault\":" + isDefault + "}";
    }
}
