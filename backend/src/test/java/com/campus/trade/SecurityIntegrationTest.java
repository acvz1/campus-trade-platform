package com.campus.trade;

import com.campus.trade.common.constant.Constant;
import com.campus.trade.common.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtUtils jwtUtils;

    @Test
    void publicPingDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    void uploadRequiresBearerToken() throws Exception {
        MockMultipartFile file = pngFile();
        mockMvc.perform(multipart("/api/file/upload").file(file).param("type", "product"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void currentUsersProductListIsNotAccidentallyPublic() throws Exception {
        mockMvc.perform(get("/api/product/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void validBearerTokenCanUploadImage() throws Exception {
        String token = jwtUtils.generateToken(9L, Constant.ROLE_USER);
        byte[] imageBytes = pngBytes();
        MvcResult uploadResult = mockMvc.perform(multipart("/api/file/upload")
                        .file(pngFile())
                        .param("type", "avatar")
                        .header(Constant.AUTHORIZATION_HEADER, Constant.BEARER_PREFIX + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.containsString("/uploads/")))
                .andExpect(jsonPath("$.data.type").value("avatar"))
                .andReturn();

        String responseBody = uploadResult.getResponse().getContentAsString();
        String uploadedUrl = responseBody.replaceFirst(".*\\\"url\\\":\\\"([^\\\"]+)\\\".*", "$1");
        mockMvc.perform(get(uploadedUrl))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "image.png", "image/png", pngBytes());
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    }
}
