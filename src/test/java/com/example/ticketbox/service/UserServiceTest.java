package com.example.ticketbox.service;

import com.example.ticketbox.TestDataUtils;
import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.user.dto.request.CreateUserRequest;
import com.example.ticketbox.user.dto.request.FindManyRequest;
import com.example.ticketbox.user.dto.request.UpdateUserRequest;
import com.example.ticketbox.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class UserServiceTest {

    @Autowired
    private UserService testService;

    @Test
    void getUsers() {
        List<Users> users = testService.getUsers(FindManyRequest.builder().build(), null);
        assertThat(users).isEmpty();

        System.out.println("Users: " + users);
    }

    @Test
    void createUserTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();

        Users saved = testService.createUser(userA);
        Users user = testService.getUserById(saved.getId());
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("beanchloe");
    }

    @Test
    void updateUserTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();
        Users saved = testService.createUser(userA);

        UpdateUserRequest updateReq =
                new UpdateUserRequest("newname", null, null);

        Users updated =
                testService.updateUser(saved.getId(), updateReq);

        assertThat(updated.getFirstName()).isEqualTo("newname");
    }

    @Test
    void findUserTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();

        Users saved = testService.createUser(userA);
        Users found = testService.getUserById(saved.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUsername()).isEqualTo("beanchloe");
    }

    @Test
    void updateRoleTest() {

        CreateUserRequest userA = TestDataUtils.createUserRequestA();

        Users saved = testService.createUser(userA);

        Users result = testService.updateUserRole(saved.getId(), UserRoles.ADMIN);

        assertThat(result.getRole()).isEqualTo(UserRoles.ADMIN);

    }

    @Test
    void getUsersTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();

        Users savedA = testService.createUser(userA);

        CreateUserRequest userB = TestDataUtils.createUserRequestB();

        Users savedB = testService.createUser(userB);

        List<Users> users = testService.getUsers(FindManyRequest.builder().username("beanchloe").build(), null);

        System.out.println("USERS" + users);
        assertThat(users.stream().toArray().length).isEqualTo(1);
        assertThat(users.stream()
                .map(user -> {
                    return user.getUsername();
                })
                .toList())
                .containsExactly("beanchloe");
    }

    @Test
    void findByUsernameTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();

        Users savedA = testService.createUser(userA);
        testService.updateUser(savedA.getId(), UpdateUserRequest.builder().firstName("Denny").build());

        CreateUserRequest userB = TestDataUtils.createUserRequestB();
        Users savedB = testService.createUser(userB);

        CreateUserRequest userC = TestDataUtils.createUserRequestC();


        List<Users> users = testService.getUsers(FindManyRequest.builder().name("Denny").build(), null);

        assertThat(users).hasSize(1);
    }

    @Test
    void findByNameTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();

        Users savedA = testService.createUser(userA);
        Users userAUpdate = testService.updateUser(savedA.getId(), UpdateUserRequest.builder().firstName("Denny").build());

        CreateUserRequest userB = TestDataUtils.createUserRequestB();
        Users savedB = testService.createUser(userB);

        CreateUserRequest userC = TestDataUtils.createUserRequestC();

        List<Users> users = testService.getUsers(FindManyRequest.builder().name("Denny").build(), null);

        assertThat(users).hasSize(1);
    }

    @Test
    void updateUserRoleAndFindByRoleTest() {
        CreateUserRequest userA = TestDataUtils.createUserRequestA();
        Users savedA = testService.createUser(userA);

        CreateUserRequest userB = TestDataUtils.createUserRequestB();
        Users savedB = testService.createUser(userB);

        Users result = testService.updateUserRole(savedA.getId(), UserRoles.MODERATOR);

        List<Users> users = testService.getUsers(FindManyRequest.builder().role(UserRoles.MODERATOR).build(), null);
        assertThat(users).hasSize(1);
        assertThat(
                users.stream()
                        .map(user -> user.getRole())
                        .toList()
        ).containsOnly(UserRoles.MODERATOR);

    }
}
