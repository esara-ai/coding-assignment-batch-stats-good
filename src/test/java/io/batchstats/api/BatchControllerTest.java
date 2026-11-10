package io.batchstats.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "batch-stats.max-numbers-per-request=3")
@AutoConfigureMockMvc
class BatchControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createAppendAndRead() throws Exception {
        String body = mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content("{\"numbers\":[1,2,3]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/batches/")))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.batchId");

        mvc.perform(post("/batches/{id}/numbers", id).contentType(MediaType.APPLICATION_JSON).content("{\"numbers\":[4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.count").value(4))
                .andExpect(jsonPath("$.stats.median").value(2.5));

        mvc.perform(get("/batches/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.max").value(4.0));
    }

    @Test
    void createWithoutBodyStartsEmpty() throws Exception {
        mvc.perform(post("/batches"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stats.count").value(0))
                .andExpect(jsonPath("$.stats.mean").value(nullValue()));
    }

    @Test
    void unknownBatchReturns404() throws Exception {
        mvc.perform(get("/batches/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Batch not found"));
    }

    @Test
    void malformedIdReturns400() throws Exception {
        mvc.perform(get("/batches/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void nullNumberReturns400() throws Exception {
        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content("{\"numbers\":[1,null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericValueReturns400() throws Exception {
        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content("{\"numbers\":[1,\"abc\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overflowingNumberReturns400() throws Exception {
        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content("{\"numbers\":[1e400]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("numbers must be finite"));
    }

    @Test
    void tooManyNumbersReturns400() throws Exception {
        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content("{\"numbers\":[1,2,3,4]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appendWithoutNumbersReturns400() throws Exception {
        String body = mvc.perform(post("/batches")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.batchId");

        mvc.perform(post("/batches/{id}/numbers", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
