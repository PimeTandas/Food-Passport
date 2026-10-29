package com.foodpassport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodpassport.photo.PhotoRepository;
import com.foodpassport.stamp.FoodEntryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Boots the whole app (H2 in place of Postgres, local disk in place of S3) and drives it over HTTP,
 * the same way a phone app would.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FoodPassportFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FoodEntryRepository entries;
    @Autowired PhotoRepository photoRepository;

    record Account(long id, String username, String token) {
    }

    Account alice;
    Account bob;
    Account carol;

    @BeforeEach
    void setUp() throws Exception {
        // Unique names per test so tests don't trip over each other's data.
        alice = register("alice");
        bob = register("bob");
        carol = register("carol");
    }

    // ---------- auth ----------

    @Test
    void requiresALoginToken() throws Exception {
        mvc.perform(get("/api/passport")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/passport").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logsInWithEmailAndPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("email", alice.username() + "@example.com", "password", "correct-horse")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("email", alice.username() + "@example.com", "password", "wrong-password")))
                .andExpect(status().isUnauthorized());
        call(alice, get("/api/users/me"))
                .andExpect(jsonPath("$.username").value(alice.username()))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("email", "other@example.com", "username", alice.username(), "password", "correct-horse")))
                .andExpect(status().isConflict());
    }

    // ---------- friends ----------

    @Test
    void friendRequestMustBeAccepted() throws Exception {
        long requestId = befriendRequest(alice, bob);
        call(bob, get("/api/friends/requests")).andExpect(jsonPath("$.incoming[0].from.username").value(alice.username()));
        call(alice, get("/api/friends")).andExpect(jsonPath("$.length()").value(0));

        call(alice, post("/api/friends/requests/" + requestId + "/accept")).andExpect(status().isForbidden());
        call(bob, post("/api/friends/requests/" + requestId + "/accept")).andExpect(status().isOk());

        call(alice, get("/api/friends")).andExpect(jsonPath("$[0].username").value(bob.username()));
        call(bob, get("/api/friends")).andExpect(jsonPath("$[0].username").value(alice.username()));
    }

    // ---------- countries & stamps ----------

    @Test
    void allCountriesArePreloaded() throws Exception {
        call(alice, get("/api/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(249));
        call(alice, get("/api/countries/jp"))
                .andExpect(jsonPath("$.country.name").value("Japan"))
                .andExpect(jsonPath("$.country.flag").value("🇯🇵"))
                .andExpect(jsonPath("$.stamps.length()").value(0));
        call(alice, get("/api/countries/XX")).andExpect(status().isNotFound());
    }

    @Test
    void stampingAPageTagsFriendsWhoGetTheirOwnStamp() throws Exception {
        makeFriends(alice, bob);

        JsonNode stamp = stamp(alice, "JP", """
                {"dishName": "Tonkotsu ramen", "placeName": "Ichiran, Tokyo", "homemade": false,
                 "price": {"amount": 1290, "currency": "jpy"}, "rating": 5, "notes": "Life changing",
                 "withFriendIds": [%d]}
                """.formatted(bob.id()));

        assertThat(stamp.get("stampedAt").asText()).isNotBlank();
        assertThat(stamp.at("/entry/price/currency").asText()).isEqualTo("JPY");
        assertThat(stamp.at("/entry/with/0/username").asText()).isEqualTo(bob.username());

        // Bob's Japan page now has the same entry, with his own (empty) rating and notes.
        JsonNode bobsPage = read(call(bob, get("/api/countries/JP")));
        JsonNode bobsStamp = bobsPage.at("/stamps/0");
        assertThat(bobsStamp.at("/entry/id").asLong()).isEqualTo(stamp.at("/entry/id").asLong());
        assertThat(bobsStamp.get("rating").isNull()).isTrue();
        assertThat(bobsStamp.at("/entry/with/0/username").asText()).isEqualTo(alice.username());

        // Bob rates it himself; Alice's rating is untouched.
        call(bob, put("/api/stamps/" + bobsStamp.get("id").asLong() + "/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 3, \"notes\": \"Too salty\"}"))
                .andExpect(jsonPath("$.rating").value(3));
        call(alice, get("/api/stamps/" + stamp.get("id").asLong())).andExpect(jsonPath("$.rating").value(5));

        // Shared details: Bob fixes the place name and Alice sees it.
        call(bob, put("/api/stamps/" + bobsStamp.get("id").asLong() + "/entry").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishName\": \"Tonkotsu ramen\", \"placeName\": \"Ichiran Shibuya\"}"))
                .andExpect(status().isOk());
        call(alice, get("/api/stamps/" + stamp.get("id").asLong()))
                .andExpect(jsonPath("$.entry.placeName").value("Ichiran Shibuya"));

        call(alice, get("/api/passport"))
                .andExpect(jsonPath("$.totalStamps").value(1))
                .andExpect(jsonPath("$.countriesStamped").value(1))
                .andExpect(jsonPath("$.countriesStampedPerContinent.Asia").value(1));
    }

    @Test
    void canOnlyTagFriends() throws Exception {
        mvc.perform(post("/api/countries/IT/stamps").header("Authorization", "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishName\": \"Carbonara\", \"withFriendIds\": [%d]}".formatted(carol.id())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stampsArePrivateToTheirOwner() throws Exception {
        JsonNode stamp = stamp(alice, "MX", "{\"dishName\": \"Tacos al pastor\"}");
        call(bob, get("/api/stamps/" + stamp.get("id").asLong())).andExpect(status().isNotFound());
        call(bob, delete("/api/stamps/" + stamp.get("id").asLong())).andExpect(status().isNotFound());
    }

    @Test
    void onlyTheCreatorChangesWhoWasThere() throws Exception {
        makeFriends(alice, bob);
        makeFriends(alice, carol);
        JsonNode stamp = stamp(alice, "TH", "{\"dishName\": \"Pad kra pao\", \"withFriendIds\": [%d]}".formatted(bob.id()));
        long bobsStampId = read(call(bob, get("/api/countries/TH"))).at("/stamps/0/id").asLong();

        call(bob, put("/api/stamps/" + bobsStampId + "/companions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"friendIds\": []}"))
                .andExpect(status().isForbidden());

        // Alice swaps Bob for Carol.
        call(alice, put("/api/stamps/" + stamp.get("id").asLong() + "/companions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"friendIds\": [%d]}".formatted(carol.id())))
                .andExpect(jsonPath("$.entry.with.length()").value(1))
                .andExpect(jsonPath("$.entry.with[0].username").value(carol.username()));
        call(bob, get("/api/countries/TH")).andExpect(jsonPath("$.stamps.length()").value(0));
        call(carol, get("/api/countries/TH")).andExpect(jsonPath("$.stamps.length()").value(1));
    }

    @Test
    void entryIsDeletedWithItsLastStamp() throws Exception {
        makeFriends(alice, bob);
        JsonNode stamp = stamp(alice, "FR", "{\"dishName\": \"Croissant\", \"withFriendIds\": [%d]}".formatted(bob.id()));
        long entryId = stamp.at("/entry/id").asLong();
        long bobsStampId = read(call(bob, get("/api/countries/FR"))).at("/stamps/0/id").asLong();

        call(alice, delete("/api/stamps/" + stamp.get("id").asLong())).andExpect(status().isNoContent());
        assertThat(entries.existsById(entryId)).isTrue(); // Bob still has it

        call(bob, delete("/api/stamps/" + bobsStampId)).andExpect(status().isNoContent());
        assertThat(entries.existsById(entryId)).isFalse();
    }

    @Test
    void validatesInput() throws Exception {
        mvc.perform(post("/api/countries/ES/stamps").header("Authorization", "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishName\": \"\", \"rating\": 9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dishName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.rating").exists());
        mvc.perform(post("/api/countries/ES/stamps").header("Authorization", "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishName\": \"Paella\", \"price\": {\"amount\": 15, \"currency\": \"XYZ\"}}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- photos ----------

    @Test
    void participantsShareEntryPhotos() throws Exception {
        makeFriends(alice, bob);
        JsonNode stamp = stamp(alice, "IN", "{\"dishName\": \"Masala dosa\", \"withFriendIds\": [%d]}".formatted(bob.id()));
        byte[] image = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

        JsonNode photo = read(mvc.perform(multipart("/api/stamps/" + stamp.get("id").asLong() + "/photos")
                        .file(new MockMultipartFile("file", "dosa.jpg", "image/jpeg", image))
                        .header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isCreated()));
        String url = photo.get("url").asText();

        call(bob, get(url)).andExpect(status().isOk()).andExpect(content().bytes(image));
        call(carol, get(url)).andExpect(status().isNotFound());
        call(bob, delete(url)).andExpect(status().isForbidden()); // not uploader, not creator

        mvc.perform(multipart("/api/stamps/" + stamp.get("id").asLong() + "/photos")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", new byte[] {1}))
                        .header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isBadRequest());

        call(alice, delete(url)).andExpect(status().isNoContent());
        assertThat(photoRepository.existsById(photo.get("id").asLong())).isFalse();
    }

    // ---------- helpers ----------

    private Account register(String name) throws Exception {
        String username = name + "_" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode response = read(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("email", username + "@example.com", "username", username, "password", "correct-horse")))
                .andExpect(status().isCreated()));
        return new Account(response.at("/user/id").asLong(), username, response.get("token").asText());
    }

    private long befriendRequest(Account from, Account to) throws Exception {
        return read(call(from, post("/api/friends/requests").contentType(MediaType.APPLICATION_JSON)
                        .content(body("username", to.username())))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private void makeFriends(Account a, Account b) throws Exception {
        long id = befriendRequest(a, b);
        call(b, post("/api/friends/requests/" + id + "/accept")).andExpect(status().isOk());
    }

    private JsonNode stamp(Account who, String country, String requestJson) throws Exception {
        return read(call(who, post("/api/countries/" + country + "/stamps")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isCreated()));
    }

    private ResultActions call(Account who, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + who.token()));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }

    private String body(String... keyValues) throws Exception {
        var map = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return json.writeValueAsString(map);
    }
}
