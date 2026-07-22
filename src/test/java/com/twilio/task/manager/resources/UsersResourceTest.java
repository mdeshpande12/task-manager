package com.twilio.task.manager.resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.twilio.task.manager.api.model.CreateUserRequest;
import com.twilio.task.manager.api.model.User;
import com.twilio.task.manager.db.UserDAO;
import com.twilio.task.manager.kafka.KafkaEventPublisher;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UsersResourceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private KafkaEventPublisher kafkaPublisher;

    private UsersResource resource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new UsersResource(userDAO, kafkaPublisher, "users");
    }

    @Test
    void getAllUsers_returnsListFromDAO() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        when(userDAO.findAll()).thenReturn(List.of(user));

        List<User> result = resource.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("john", result.get(0).getUsername());
    }

    @Test
    void getUserById_found_returnsUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        when(userDAO.findById(1L)).thenReturn(Optional.of(user));

        User result = resource.getUserById(1L);

        assertEquals("john", result.getUsername());
    }

    @Test
    void getUserById_notFound_throws404() {
        when(userDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> resource.getUserById(99L));
    }

    @Test
    void createUser_setsFieldsAndReturnsCreated() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setFullName("John Doe");

        User saved = new User();
        saved.setId(1L);
        saved.setUsername("john");

        when(userDAO.create(any(User.class))).thenReturn(1L);
        when(userDAO.findById(1L)).thenReturn(Optional.of(saved));

        User result = resource.createUser(request);

        assertEquals(1L, result.getId());
        assertEquals("john", result.getUsername());
        verify(userDAO).create(any(User.class));
    }

    @Test
    void deleteUser_found_deletes() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(new User()));

        resource.deleteUser(1L);

        verify(userDAO).delete(1L);
    }

    @Test
    void deleteUser_notFound_throws404() {
        when(userDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> resource.deleteUser(99L));
        verify(userDAO, never()).delete(any());
    }

    @Test
    void updateUser_found_updatesAndReturns() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(new User()));

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("john_updated");
        request.setEmail("john_new@example.com");
        request.setFullName("John Updated");

        User updated = new User();
        updated.setId(1L);
        updated.setUsername("john_updated");
        when(userDAO.findById(1L)).thenReturn(Optional.of(updated));

        User result = resource.updateUser(1L, request);

        assertEquals("john_updated", result.getUsername());
        verify(userDAO).update(any(User.class));
    }

    @Test
    void updateUser_notFound_throws404() {
        when(userDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                resource.updateUser(99L, new CreateUserRequest()));
        verify(userDAO, never()).update(any());
    }

    @Test
    void getAllUsers_daoThrowsException_propagates500() {
        when(userDAO.findAll()).thenThrow(new RuntimeException("DB connection failed"));

        assertThrows(RuntimeException.class, () -> resource.getAllUsers());
    }

    @Test
    void getUserById_daoThrowsException_propagates500() {
        when(userDAO.findById(1L)).thenThrow(new RuntimeException("DB timeout"));

        assertThrows(RuntimeException.class, () -> resource.getUserById(1L));
    }

    @Test
    void createUser_daoThrowsException_propagates500() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");

        when(userDAO.create(any(User.class))).thenThrow(new RuntimeException("DB write failed"));

        assertThrows(RuntimeException.class, () -> resource.createUser(request));
    }

    @Test
    void deleteUser_daoThrowsOnDelete_propagates500() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(new User()));
        doThrow(new RuntimeException("DB error")).when(userDAO).delete(1L);

        assertThrows(RuntimeException.class, () -> resource.deleteUser(1L));
    }

    @Test
    void updateUser_daoThrowsOnUpdate_propagates500() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(new User()));
        doThrow(new RuntimeException("DB error")).when(userDAO).update(any(User.class));

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");

        assertThrows(RuntimeException.class, () -> resource.updateUser(1L, request));
    }

    @Test
    void createUser_createdButNotFoundOnFetch_returnsNull() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");

        when(userDAO.create(any(User.class))).thenReturn(1L);
        when(userDAO.findById(1L)).thenReturn(Optional.empty());

        User result = resource.createUser(request);

        assertNull(result);
    }
}
