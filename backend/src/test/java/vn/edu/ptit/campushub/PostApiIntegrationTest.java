package vn.edu.ptit.campushub;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicUserCanReadSeededPosts() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void anonymousUserCannotCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bài mới","content":"Nội dung hợp lệ","categoryId":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tìm nhóm học DevOps","content":"Cần thêm hai thành viên cùng thực hành.","categoryId":6}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tìm nhóm học DevOps"))
                .andExpect(jsonPath("$.author.username").value("student"));
    }

    @Test
    void invalidPostReturnsReadableMessage() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","content":"Nội dung","categoryId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
