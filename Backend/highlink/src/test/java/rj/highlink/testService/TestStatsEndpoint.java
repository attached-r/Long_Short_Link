package rj.highlink.testService;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 统计接口集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestStatsEndpoint {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test // 测试获取统计数据
    void testGetStats_Success() {
        given()
                .pathParam("shortCode", "test123")
                .when()
                .get("/shortlink/stats/{shortCode}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(200))
                .body("data.shortCode", equalTo("test123"))
                .body("data.pv", notNullValue())
                .body("data.uv", notNullValue());
    }

    @Test // 测试获取统计数据（带时间范围）
    void testGetStatsWithTimeRange() {
        given()
                .pathParam("shortCode", "test123")
                .queryParam("startTime", "2024-01-01 00:00:00")
                .queryParam("endTime", "2024-12-31 23:59:59")
                .when()
                .get("/shortlink/stats/{shortCode}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(200))
                .body("data.startTime", notNullValue())
                .body("data.endTime", notNullValue());
    }

    @Test // 测试获取不存在的短链接的统计数据
    void testGetStats_InvalidShortCode() {
        given()
                .pathParam("shortCode", "")
                .when()
                .get("/shortlink/stats/{shortCode}")
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
