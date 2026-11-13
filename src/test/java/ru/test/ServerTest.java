package ru.test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.test.data.serialization.ProtobufGraphSerializer;
import ru.test.data.serialization.dto.EdgeDto;
import ru.test.data.serialization.dto.GraphDto;
import ru.test.data.serialization.proto.GraphSnapshotProto;
import ru.test.domain.AdminProfile;
import ru.test.domain.DeletedProfile;
import ru.test.domain.Profile;
import ru.test.domain.UserProfile;
import ru.test.domain.datastructure.Connections;
import ru.test.server.dto.CreateProfileRequest;
import ru.test.server.service.GraphService;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ServerTest {

    @Inject
    GraphService graphService;

    @BeforeEach
    void reset() {
        graphService.getGraph().clear();
    }

    @Test
    void testCreateAndUpdateProfileWithDtos() {
        // Создание без передачи ID
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "type": "USER",
              "name": "Сергей",
              "city": "Новосибирск",
              "birthYear": 2005,
              "rating": 4.8,
              "bio": "Студент"
            }
            """)
                .when()
                .post("/api/profiles")
                .then()
                .statusCode(201)
                .body("id", greaterThan(0))
                .body("editable", equalTo(true));

        int generatedId = graphService.getAllProfiles(null).get(0).id();

        // Обновление через UpdateProfileRequest
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "city": "Москва",
              "rating": 5.0,
              "bio": "Тимлид"
            }
            """)
                .when()
                .put("/api/profiles/" + generatedId)
                .then()
                .statusCode(200)
                .body("city", equalTo("Москва"))
                .body("rating", equalTo(5.0F))
                .body("bio", equalTo("Тимлид"));
    }

    @Test
    void testCreateDeletedProfileViaRestIsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "type": "DELETED",
              "name": "Хакер",
              "city": "Омск",
              "birthYear": 2000
            }
            """)
                .when()
                .post("/api/profiles")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("BAD_REQUEST"));
    }

    @Test
    void testStepByStepJsonMigrationV1ToV3() {
        String v1Json = """
        {
          "schema_version": 1,
          "profiles": [
            { "type": "USER", "id": 1, "name": "Иван", "city": "Новосибирск", "birthYear": 2004 },
            { "type": "ADMIN", "id": 2, "name": "Сергей", "city": "Москва", "birthYear": 2003, "groups": ["IT-Club"] }
          ],
          "edges": [
            { "from_id": 1, "friend_id": 2, "strength": 8 }
          ]
        }
        """;

        given()
                .contentType(ContentType.JSON)
                .body(v1Json)
                .when()
                .post("/api/import")
                .then()
                .statusCode(200)
                .body("importedFromVersion", equalTo(1))
                .body("migratedToVersion", equalTo(3))
                .body("wasMigrated", equalTo(true))
                .body("profilesCount", equalTo(2));

        Profile p1 = graphService.findDomainProfile(1);
        assertEquals(0.0, p1.getRating());
        assertEquals("", p1.getBio());
        assertInstanceOf(UserProfile.class, p1);
    }

    @Test
    void testDowngradeJsonToV1DropsNewFields() {
        graphService.createProfile(new CreateProfileRequest("ADMIN", 10, "Анна", "Казань", 2001, 4.9, "Тимлид", List.of("Science")));

        given()
                .queryParam("format", "json")
                .queryParam("version", 1)
                .when()
                .post("/api/export")
                .then()
                .statusCode(200)
                .header("X-Downgrade-Warning", containsString("drops fields"))
                .body("schema_version", equalTo(1))
                .body("profiles[0].rating", nullValue())
                .body("profiles[0].bio", nullValue());
    }

    @Test
    void testProtobufMigrationV1ToV3AndRoundtrip() throws Exception {
        ProtobufGraphSerializer protoSerializer = new ProtobufGraphSerializer();
        Profile userV1 = new UserProfile(10, "Дмитрий", "Новосибирск", 2003, new Connections[0]);
        AdminProfile adminV1 = new AdminProfile(20, "Сергей", "Москва", 2002, new Connections[0], List.of("IT-Club"));
        GraphDto dto = new GraphDto(3, List.of(userV1, adminV1), List.of(new EdgeDto(10, 20, 9)));

        byte[] v1Bytes = protoSerializer.serializeToBytes(dto, 1);

        given()
                .contentType("application/x-protobuf")
                .body(v1Bytes)
                .when()
                .post("/api/import")
                .then()
                .statusCode(200)
                .body("format", equalTo("protobuf"))
                .body("importedFromVersion", equalTo(1))
                .body("migratedToVersion", equalTo(3))
                .body("wasMigrated", equalTo(true))
                .body("profilesCount", equalTo(2))
                .body("edgesCount", equalTo(1));

        Profile restored = graphService.findDomainProfile(20);
        assertEquals(0.0, restored.getRating());
        assertEquals("", restored.getBio());
        assertInstanceOf(AdminProfile.class, restored);
    }

    @Test
    void testProtobufFutureVersionReturns409() {
        byte[] futureBytes = GraphSnapshotProto.newBuilder()
                .setSchemaVersion(42)
                .build()
                .toByteArray();

        given()
                .contentType("application/x-protobuf")
                .body(futureBytes)
                .when()
                .post("/api/import")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("FUTURE_SCHEMA_VERSION"));
    }

    @Test
    void testCorruptedFieldDuringMigrationReturns400() {
        String badJson = """
        {
          "schema_version": 1,
          "profiles": [
            { "type": "USER", "id": 1, "name": "Тест", "city": "Омск", "birthYear": 1800 }
          ]
        }
        """;

        given()
                .contentType(ContentType.JSON)
                .body(badJson)
                .when()
                .post("/api/import")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_FIELD_VALUE"));
    }

    @Test
    void testEditDeletedProfileReturns422() {
        // Read-only сущность попадает в граф напрямую
        graphService.getGraph().addVertex(new DeletedProfile(77, "Архив", "Москва", 1995, new Connections[0]));

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"НовоеИмя\"}")
                .when()
                .put("/api/profiles/77")
                .then()
                .statusCode(422)
                .body("errorCode", equalTo("READ_ONLY_ENTITY"));
    }

    @Test
    void testGenerateAndBfsShortestPathEndpoints() {
        given()
                .queryParam("count", 15)
                .when()
                .post("/api/generate")
                .then()
                .statusCode(201)
                .body("generatedProfiles", equalTo(15))
                .body("totalVerticesInGraph", equalTo(15));

        graphService.getGraph().clear();
        graphService.createProfile(new CreateProfileRequest("USER", 1, "A", "City", 2000, 1.0, "", null));
        graphService.createProfile(new CreateProfileRequest("USER", 2, "B", "City", 2000, 1.0, "", null));
        graphService.createProfile(new CreateProfileRequest("USER", 3, "C", "City", 2000, 1.0, "", null));

        graphService.addFriendship(1, 2, 5);
        graphService.addFriendship(2, 3, 10);

        given()
                .queryParam("from", 1)
                .queryParam("to", 3)
                .when()
                .get("/api/profiles/path")
                .then()
                .statusCode(200)
                .body("found", equalTo(true))
                .body("path", contains(1, 2, 3));
    }
}