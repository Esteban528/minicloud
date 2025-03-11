package com.estebandev.minicloud.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.entity.UserMetadata;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testSaveUser() {
        // Arrange
        User user = User.builder()
                .nickname("estebandev.test")
                .email("test@minicloud.com")
                .password("hola")
                .build();

        // Act
        User result = userRepository.save(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getNickname()).isEqualTo(user.getNickname());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    public void testFindById() {
        // Arrange
        User user = User.builder()
                .nickname("estebandev.find")
                .email("find@minicloud.com")
                .password("testpassword")
                .build();
        User savedUser = userRepository.save(user);

        // Act
        Optional<User> result = userRepository.findById(savedUser.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedUser.getId());
        assertThat(result.get().getNickname()).isEqualTo(savedUser.getNickname());
        assertThat(result.get().getEmail()).isEqualTo(savedUser.getEmail());
    }

    @Test
    private void testFindByEmail() {
        // Arrange
        User user = User.builder()
                .nickname("estebandev.email")
                .email("email@minicloud.com")
                .password("123456")
                .build();
        userRepository.save(user);

        // Act
        Optional<User> result = userRepository.findByEmail("email@minicloud.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(user.getEmail());
        assertThat(result.get().getNickname()).isEqualTo(user.getNickname());
    }

    @Test
    private void testUpdateUser() {
        // Arrange
        User user = User.builder()
                .nickname("estebandev.update")
                .email("update@minicloud.com")
                .password("oldPassword")
                .build();
        User savedUser = userRepository.save(user);

        // Act
        savedUser.setPassword("newPassword");
        User updatedUser = userRepository.save(savedUser);

        // Assert
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(savedUser.getId());
        assertThat(updatedUser.getPassword()).isEqualTo("newPassword");
    }

    @Test
    private void testDeleteUser() {
        // Arrange
        User user = User.builder()
                .nickname("estebandev.delete")
                .email("delete@minicloud.com")
                .password("toDelete")
                .build();
        User savedUser = userRepository.save(user);

        // Act
        userRepository.deleteById(savedUser.getId());
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());

        // Assert
        assertThat(deletedUser).isNotPresent();
    }

    @Test
    public void testCountUsers() {
        // Arrange
        User user1 = User.builder()
                .nickname("user1")
                .email("user1@minicloud.com")
                .password("password1")
                .build();

        User user2 = User.builder()
                .nickname("user2")
                .email("user2@minicloud.com")
                .password("password2")
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        // Act
        long userCount = userRepository.count();

        // Assert
        assertThat(userCount).isEqualTo(2);
    }

    @Test
    void testUserMetadata() {
        // Arrange
        User user = User.builder()
                .nickname("estebandev.test")
                .email("test@minicloud.com")
                .password("hola")
                .userMetadata(new ArrayList<>())
                .build();
        UserMetadata userMetadata = UserMetadata.builder()
                .user(user)
                .key("test")
                .value("true")
                .build();
        user.getUserMetadata().add(userMetadata);

        // Act
        User result = userRepository.save(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getNickname()).isEqualTo(user.getNickname());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
        assertThat(result.getUserMetadata().get(0)).isEqualTo(userMetadata);
    }
}
