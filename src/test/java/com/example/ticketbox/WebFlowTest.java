package com.example.ticketbox;

import com.jayway.jsonpath.JsonPath;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.showseat.service.ShowSeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "payments.mock.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebFlowTest {
    @LocalServerPort int port;
    @Autowired TestDataFactory fixtures;
    @Autowired ShowSeatService showSeats;
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api" + path)).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void guestCanBrowseButCannotBook() throws Exception {
        assertThat(call("GET", "/movies", null, null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/shows", null, null).statusCode()).isEqualTo(200);
        assertThat(call("POST", "/bookings/newbook", null, "{}").statusCode()).isEqualTo(401);
        assertThat(call("GET", "/auth/me", "broken", null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/payments/mock/config", null, null).statusCode()).isEqualTo(401);
    }
    @Test void signupReservePayAndCancelThroughHttp() throws Exception {
        var signup = call("POST", "/auth/signup", null, "{\"username\":\"http-user\",\"password\":\"password123\"}");
        assertThat(signup.statusCode()).isEqualTo(200);
        String token = JsonPath.read(signup.body(), "$.token");
        var me = call("GET", "/auth/me", token, null);
        int userId = JsonPath.read(me.body(), "$.id");
        var room = fixtures.createRoom(); fixtures.createSeatsForRoom(room);
        var movie = fixtures.createMovie();
        var show = fixtures.createShow(CreateShowReq.builder().roomId(room.getId()).movieId(movie.getId()).build());
        showSeats.generateForShow(show.getId());
        var seatResponse = call("GET", "/show-seats/show/" + show.getId(), null, null);
        assertThat(seatResponse.statusCode()).isEqualTo(200);
        int seatId = JsonPath.read(seatResponse.body(), "$[0].id");
        var booked = call("POST", "/bookings/newbook", token, "{\"userId\":"+userId+",\"showId\":"+show.getId()+",\"showSeatIds\":["+seatId+"],\"totalPrice\":1}");
        assertThat(booked.statusCode()).isEqualTo(201);
        Number total = JsonPath.read(booked.body(), "$.total_price");
        assertThat(total.intValue()).isEqualTo(100000);
        Number bookingId = JsonPath.read(booked.body(), "$.id");
        assertThat((String)JsonPath.read(booked.body(), "$.movie_title")).isEqualTo(movie.getTitle());
        var payment = call("POST", "/payments", token, "{\"booking_id\":"+bookingId+",\"method\":\"CREDIT_CARD\"}");
        assertThat(payment.statusCode()).isEqualTo(201);
        Number paymentId = JsonPath.read(payment.body(), "$.id");
        assertThat(call("PATCH", "/payments/"+paymentId+"/complete", token, "{\"transaction_id\":\"fake\"}").statusCode()).isEqualTo(403);
        assertThat(call("PATCH", "/bookings/"+bookingId+"/cancel", token, null).statusCode()).isEqualTo(200);
        assertThat((String)JsonPath.read(call("GET", "/show-seats/show/"+show.getId(), null, null).body(), "$[0].status")).isEqualTo("AVAILABLE");
        var otherSignup = call("POST", "/auth/signup", null, "{\"username\":\"other-http-user\",\"password\":\"password123\"}");
        String other = JsonPath.read(otherSignup.body(), "$.token");
        assertThat(call("GET", "/bookings/"+bookingId, other, null).statusCode()).isEqualTo(403);
        assertThat(call("POST", "/bookings/newbook", other, "{\"userId\":"+userId+",\"showId\":"+show.getId()+",\"showSeatIds\":["+seatId+"]}").statusCode()).isEqualTo(403);
        var nextBooking = call("POST", "/bookings/newbook", token, "{\"userId\":"+userId+",\"showId\":"+show.getId()+",\"showSeatIds\":["+seatId+"]}");
        assertThat(nextBooking.statusCode()).isEqualTo(201);
        Number nextId = JsonPath.read(nextBooking.body(), "$.id");
        var nextPayment = call("POST", "/payments", token, "{\"booking_id\":"+nextId+",\"method\":\"BANK_TRANSFER\"}");
        Number nextPaymentId = JsonPath.read(nextPayment.body(), "$.id");
        assertThat(call("GET", "/payments/"+nextPaymentId+"/mock-checkout", token, null).statusCode()).isEqualTo(200);
        assertThat(call("POST", "/payments/"+nextPaymentId+"/mock-confirm", other, "{\"outcome\":\"SUCCESS\"}").statusCode()).isEqualTo(403);
        assertThat(call("POST", "/payments/"+nextPaymentId+"/mock-confirm", token, "{\"outcome\":\"INVALID\"}").statusCode()).isEqualTo(400);
        var confirmed = call("POST", "/payments/"+nextPaymentId+"/mock-confirm", token, "{\"outcome\":\"SUCCESS\"}");
        assertThat(confirmed.statusCode()).isEqualTo(200);
        assertThat((String)JsonPath.read(confirmed.body(), "$.status")).isEqualTo("PAID");
        assertThat(call("POST", "/payments/"+nextPaymentId+"/mock-confirm", token, "{\"outcome\":\"SUCCESS\"}").statusCode()).isEqualTo(200);
        assertThat((String)JsonPath.read(call("GET", "/bookings/"+nextId, token, null).body(), "$.status")).isEqualTo("CONFIRMED");
    }
}
