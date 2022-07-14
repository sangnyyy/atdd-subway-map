package nextstep.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.StationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@DisplayName("지하철 노선 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LineAcceptanceTest {

    @LocalServerPort
    int port;

    StationClient stationClient;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        stationClient = new StationClient();
        stationClient.create("지하철역");
        stationClient.create("새로운지하철역");
        stationClient.create("또다른지하철역");
    }

    /**
     * When 지하철 노선을 생성하면
     * Then 지하철 노선 목록 조회 시 생성한 노선을 찾을 수 있다
     */
    @DisplayName("지하철노선을 생성한다.")
    @Test
    @DirtiesContext
    void createLine() {
        // when
        Map<String, Object> params = Map.of(
                "name", "신분당선",
                "color", "bg-red-600",
                "upStationId", 1,
                "downStationId", 2,
                "distance", 10
        );

        ExtractableResponse<Response> response = createLines(params);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // then
        List<String> lineNames = findAllLines().jsonPath().getList("name", String.class);

        assertThat(lineNames).containsAnyOf("신분당선");

    }


    /**
     * Given 2개의 지하철 노선을 생성하고
     * When 지하철 노선 목록을 조회하면
     * Then 지하철 노선 목록 조회 시 2개의 노선을 조회할 수 있다.
     */
    @DisplayName("지하철노선 목록을 조회한다.")
    @Test
    @DirtiesContext
    void getLines() {
        // given
        Map<String, Object> params1 = Map.of(
                "name", "신분당선",
                "color", "bg-red-600",
                "upStationId", 1,
                "downStationId", 2,
                "distance", 10
        );

        Map<String, Object> params2 = Map.of(
                "name", "분당선",
                "color", "bg-yellow-600",
                "upStationId", 1,
                "downStationId", 3,
                "distance", 20
        );

        createLines(params1);
        createLines(params2);

        // when
        List<String> lineNames = findAllLines().jsonPath().getList("name", String.class);

        // then
        assertThat(lineNames).containsExactly("신분당선", "분당선");
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 생성한 지하철 노선을 조회하면
     * Then 생성한 지하철 노선의 정보를 응답받을 수 있다.
     */
    @DisplayName("지하철노선을 조회한다.")
    @Test
    @DirtiesContext
    void getLine() {
        // given
        Map<String, Object> params = Map.of(
                "name", "신분당선",
                "color", "bg-red-600",
                "upStationId", 1,
                "downStationId", 2,
                "distance", 10
        );

        createLines(params);

        // when
        ExtractableResponse<Response> response = findLineById(1L);

        // then
        assertThat(response.jsonPath().getString("name")).isEqualTo("신분당선");
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 생성한 지하철 노선을 수정하면
     * Then 해당 지하철 노선 정보는 수정된다
     */
    @DisplayName("지하철노선을 수정한다.")
    @Test
    @DirtiesContext
    void putLine() {
        // given
        Map<String, Object> params = Map.of(
                "name", "신분당선",
                "color", "bg-red-600",
                "upStationId", 1,
                "downStationId", 2,
                "distance", 10
        );

        createLines(params);

        // when
        putLineById(1L, Map.of("name", "5호선", "color", "bg-purple-600"));

        // then
        assertAll(
                () -> assertThat(findLineById(1L).jsonPath().getString("name")).isEqualTo("5호선"),
                () -> assertThat(findLineById(1L).jsonPath().getString("color")).isEqualTo("bg-purple-600")
        );
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 생성한 지하철 노선을 삭제하면
     * Then 해당 지하철 노선 정보는 삭제된다
     */
    @DisplayName("지하철노선을 삭제한다.")
    @Test
    @DirtiesContext
    void deleteLine() {
        // given
        Map<String, Object> params = Map.of(
                "name", "신분당선",
                "color", "bg-red-600",
                "upStationId", 1,
                "downStationId", 2,
                "distance", 10
        );

        createLines(params);

        // when
        deleteLineById(1L);

        // then
        assertAll(
                () -> assertThat(findLineById(1L).jsonPath().getString("name")).isNullOrEmpty(),
                () -> assertThat(findLineById(1L).jsonPath().getString("color")).isNullOrEmpty()
        );
    }

    private ExtractableResponse<Response> createLines(Map<String, Object> params) {
        return RestAssured.given().log().all()
                .body(params)
                .contentType(APPLICATION_JSON_VALUE)
                .when().post("/lines")
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> findAllLines() {
        return RestAssured.given().log().all()
                .when().get("/lines")
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> findLineById(Long id) {
        return RestAssured.given().log().all()
                .when().get("/lines/" + id)
                .then().log().all().extract();
    }

    private void putLineById(Long id, Map<String, String> params) {
        RestAssured.given().log().all()
                .headers("Content-Type", "application/json; charset=UTF-8")
                .body(params)
                .when().put("/lines/" + id)
                .then().log().all();
    }

    private void deleteLineById(Long id) {
        RestAssured.given().log().all()
                .when().delete("/lines/" + id)
                .then().log().all();
    }

}
