package com.example.ticketbox.admin;

import com.example.ticketbox.booking.dto.BookingMapper;
import com.example.ticketbox.booking.dto.response.BookingResponse;
import com.example.ticketbox.booking.repository.BookingRepository;
import com.example.ticketbox.enums.SeatStatus;
import com.example.ticketbox.enums.UserRoles;
import static com.example.ticketbox.enums.UserPermission.*;
import com.example.ticketbox.movie.dto.MovieMapper;
import com.example.ticketbox.movie.dto.response.MovieResponse;
import com.example.ticketbox.movie.repository.MovieRepository;
import com.example.ticketbox.payment.dto.PaymentMapper;
import com.example.ticketbox.payment.dto.response.PaymentResponse;
import com.example.ticketbox.payment.repository.PaymentRepository;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.repository.RoomRepository;
import com.example.ticketbox.room.service.RoomService;
import com.example.ticketbox.seat.dto.request.AutoGenerateSeatReq;
import com.example.ticketbox.seat.repository.SeatRepository;
import com.example.ticketbox.seat.service.SeatService;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.dto.Response.ShowResponse;
import com.example.ticketbox.show.dto.ShowMapper;
import com.example.ticketbox.show.repository.ShowRepository;
import com.example.ticketbox.show.service.ShowService;
import com.example.ticketbox.showseat.repository.ShowSeatRepository;
import com.example.ticketbox.showseat.service.ShowSeatService;
import com.example.ticketbox.user.dto.response.UserDetailResponse;
import com.example.ticketbox.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminWorkspaceService {
    private final MovieRepository movies;
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final ShowRepository shows;
    private final ShowSeatRepository showSeats;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final MovieMapper movieMapper;
    private final ShowMapper showMapper;
    private final BookingMapper bookingMapper;
    private final PaymentMapper paymentMapper;
    private final RoomService roomService;
    private final SeatService seatService;
    private final ShowService showService;
    private final ShowSeatService showSeatService;
    private final EntityManager entityManager;

    public record RoomInfo(Integer id, String name, Integer seatRows, long seatCount) {}
    public record Availability(Integer showId, long total, long available, long held, long booked) {}
    public record Workspace(List<MovieResponse> movies, List<ShowResponse> shows, List<RoomInfo> rooms,
                            List<BookingResponse> bookings, List<PaymentResponse> payments,
                            List<UserDetailResponse> users, List<UserDetailResponse> employees, List<Long> reviewablePaymentIds,
                            List<Availability> availability, LocalDateTime refreshedAt) {}

    @Transactional(readOnly = true)
    public Workspace workspace(String actorName) {
        var actor = users.findByUsername(actorName).orElseThrow(() -> new AccessDeniedException("Account not found"));
        if (!actor.hasPermission(VIEW_OPERATIONS)) throw new AccessDeniedException("No module access");
        var now = LocalDateTime.now();
        Map<Integer, Long> roomCounts = new HashMap<>();
        seats.findAll().forEach(seat -> roomCounts.merge(seat.getRoom().getId(), 1L, Long::sum));
        Map<Integer, long[]> counts = new HashMap<>();
        showSeats.findAll().forEach(seat -> {
            var count = counts.computeIfAbsent(seat.getShow().getId(), id -> new long[4]);
            count[0]++;
            if (seat.getStatus() == SeatStatus.AVAILABLE || (seat.getStatus() == SeatStatus.PENDING
                    && seat.getHeldUntil() != null && !seat.getHeldUntil().isAfter(now))) count[1]++;
            else if (seat.getStatus() == SeatStatus.PENDING) count[2]++;
            else if (seat.getStatus() == SeatStatus.BOOKED) count[3]++;
        });
        var allPayments = actor.hasPermission(VIEW_PAYMENTS) ? payments.findAll() : List.<com.example.ticketbox.entity.Payments>of();
        var nowTime = LocalDateTime.now();
        var reviewable = allPayments.stream().filter(payment -> payment.getStatus() == com.example.ticketbox.enums.PaymentStatus.PENDING
                && payment.getBooking().getStatus() == com.example.ticketbox.enums.BookingStatus.PENDING
                && payment.getBooking().getExpiresAt() != null && payment.getBooking().getExpiresAt().isAfter(nowTime))
                .map(com.example.ticketbox.entity.Payments::getId).toList();
        var accounts = actor.hasPermission(VIEW_CUSTOMERS) || actor.getRole() == UserRoles.ADMIN ? users.findAll() : List.<com.example.ticketbox.entity.Users>of();
        return new Workspace(movieMapper.toResponses(actor.hasPermission(VIEW_MOVIES) ? movies.findAll()
                    : actor.hasPermission(MANAGE_SHOWS) ? movies.findByArchivedFalse() : List.of()),
                actor.hasPermission(VIEW_SHOWS) ? showMapper.toResponses(shows.findAll()) : List.of(),
                (actor.hasPermission(VIEW_ROOMS) || actor.hasPermission(MANAGE_SHOWS) ? rooms.findAll() : List.<com.example.ticketbox.entity.Rooms>of()).stream().map(room -> new RoomInfo(room.getId(), room.getName(), room.getSeatRows(), roomCounts.getOrDefault(room.getId(), 0L))).toList(),
                actor.hasPermission(VIEW_BOOKINGS) ? bookingMapper.toResponses(bookings.findAll()) : List.of(),
                allPayments.stream().map(paymentMapper::toResponse).toList(),
                accounts.stream().filter(user -> user.getRole() == UserRoles.USER && !user.hasPermission(VIEW_OPERATIONS)).map(UserDetailResponse::from).toList(),
                actor.getRole() == UserRoles.ADMIN ? accounts.stream().filter(user -> user.getRole() != UserRoles.USER || user.hasPermission(VIEW_OPERATIONS)).map(UserDetailResponse::from).toList() : List.of(),
                reviewable,
                (actor.hasPermission(VIEW_SHOWS) ? counts : Map.<Integer, long[]>of()).entrySet().stream().map(entry -> new Availability(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3])).toList(), now);
    }

    @Transactional
    public RoomInfo createRoom(AdminRequests.RoomInput input) {
        var room = roomService.create(CreateRoomRequest.builder().name(input.name().trim()).seatRows(input.rows()).build());
        var generated = seatService.autoGenerateSeats(room.getId(), AutoGenerateSeatReq.builder()
                .rows(input.rows()).seatsPerRow(input.seatsPerRow()).startSeatNumber(1).seatType(input.seatType()).build());
        return new RoomInfo(room.getId(), room.getName(), room.getSeatRows(), generated.size());
    }

    @Transactional
    public ShowResponse schedule(AdminRequests.ShowInput input) {
        var movie = movies.findByIdForUpdate(input.movieId()).orElseThrow(() -> new EntityNotFoundException("Movie not found"));
        entityManager.refresh(movie);
        var show = showService.createShow(CreateShowReq.builder().movieId(input.movieId()).roomId(input.roomId())
                .startTime(input.startTime()).endTime(input.startTime().plusMinutes(movie.getDurationMin())).build());
        showSeatService.generateForShow(show.getId());
        return showMapper.toResponse(show);
    }

    @Transactional
    public MovieResponse archive(Integer id, boolean archived) {
        var movie = movies.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("Movie not found"));
        entityManager.refresh(movie);
        if (archived && shows.existsByMovieIdAndEndTimeAfter(id, LocalDateTime.now())) {
            throw new IllegalStateException("This movie still has scheduled screenings. Archive it after its final screening.");
        }
        movie.setArchived(archived);
        return movieMapper.toResponse(movie);
    }

    @Transactional
    public UserDetailResponse changePermissions(Integer id, AdminRequests.PermissionsInput input, String actorName) {
        users.lockAdministrators();
        var actor = users.findByUsername(actorName).orElseThrow(() -> new AccessDeniedException("Administrator not found"));
        entityManager.refresh(actor);
        if (actor.getRole() != UserRoles.ADMIN) throw new AccessDeniedException("Only administrators can change permissions");
        if (actor.getId().equals(id)) throw new IllegalStateException("You cannot change your own permissions");
        var target = users.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        entityManager.refresh(target);
        if (target.getRole() == UserRoles.ADMIN) throw new IllegalStateException("Administrators always retain full access. Change the role first.");
        if (input.permissions().contains(VIEW_OPERATIONS))
            throw new IllegalArgumentException("Choose module permissions instead of the legacy operations permission");
        target.setCustomPermissions(!input.useRoleDefaults());
        target.setPermissions(new HashSet<>(input.useRoleDefaults() ? Set.of() : input.permissions()));
        return UserDetailResponse.from(target);
    }

    @Transactional
    public UserDetailResponse changeRole(Integer id, UserRoles role, String actorName) {
        // Serialize role changes and recheck the actor after acquiring the locks.
        users.lockAdministrators();
        var actor = users.findByUsername(actorName).orElseThrow(() -> new AccessDeniedException("Administrator not found"));
        entityManager.refresh(actor);
        if (actor.getRole() != UserRoles.ADMIN) throw new AccessDeniedException("Only administrators can change roles");
        if (actor.getId().equals(id)) throw new IllegalStateException("You cannot change your own role");
        var user = users.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        entityManager.refresh(user);
        user.setRole(Objects.requireNonNull(role));
        user.setCustomPermissions(false);
        user.getPermissions().clear();
        return UserDetailResponse.from(user);
    }
}
