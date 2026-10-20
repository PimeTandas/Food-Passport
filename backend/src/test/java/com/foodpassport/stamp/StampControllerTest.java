package com.foodpassport.stamp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Starts the whole app (with the in-memory DB) and fires real HTTP requests at it. */
@SpringBootTest
@AutoConfigureMockMvc
class StampControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    StampRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndFetchesAStamp() throws Exception {
        String json = """
                {"dishName": "Ramen", "country": "japan", "rating": 5, "dateTried": "2026-01-15"}
                """;

        String location = mvc.perform(post("/api/stamps").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.country").value("Japan"))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(location.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mvc.perform(get("/api/stamps/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishName").value("Ramen"));
    }

    @Test
    void rejectsInvalidStamp() throws Exception {
        String json = """
                {"dishName": "", "country": "Italy", "rating": 9}
                """;
        mvc.perform(post("/api/stamps").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForMissingStamp() throws Exception {
        mvc.perform(get("/api/stamps/999999")).andExpect(status().isNotFound());
    }

    @Test
    void updatesAndDeletesAStamp() throws Exception {
        Stamp saved = repository.save(new Stamp("Pho", "Vietnam", null, java.time.LocalDate.now(), 3, null));

        mvc.perform(put("/api/stamps/" + saved.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishName\": \"Pho Bo\", \"country\": \"Vietnam\", \"rating\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishName").value("Pho Bo"))
                .andExpect(jsonPath("$.rating").value(5));

        mvc.perform(delete("/api/stamps/" + saved.getId())).andExpect(status().isNoContent());
        mvc.perform(get("/api/stamps/" + saved.getId())).andExpect(status().isNotFound());
    }

    @Test
    void summarisesThePassport() throws Exception {
        for (String[] s : new String[][] {{"Ramen", "Japan"}, {"Sushi", "japan"}, {"Kimchi", "south korea"}}) {
            mvc.perform(post("/api/stamps").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"dishName\": \"" + s[0] + "\", \"country\": \"" + s[1] + "\"}"))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/passport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStamps").value(3))
                .andExpect(jsonPath("$.countriesVisited").value(2))
                .andExpect(jsonPath("$.stampsPerCountry.Japan").value(2))
                .andExpect(jsonPath("$.stampsPerCountry['South Korea']").value(1));

        mvc.perform(get("/api/stamps").param("country", "JAPAN"))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
