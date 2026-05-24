package com.twilio.task.manager.resources;

import java.util.List;

import com.codahale.metrics.annotation.Timed;
import com.twilio.task.manager.api.UsersApi;
import com.twilio.task.manager.api.model.CreateUserRequest;
import com.twilio.task.manager.api.model.User;
import com.twilio.task.manager.db.UserDAO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;

public class UsersResource implements UsersApi {
    private final UserDAO userDAO;

    public UsersResource(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    @Timed
    public User createUser(@Valid @NotNull CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        long id = userDAO.create(user);
        return userDAO.findById(id).orElse(null);
    }

    @Override
    @Timed
    public void deleteUser(Long userId) {
        userDAO.findById(userId).orElseThrow(NotFoundException::new);
        userDAO.delete(userId);
    }

    @Override
    @Timed
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    @Override
    @Timed
    public User getUserById(Long userId) {
        return userDAO.findById(userId).orElseThrow(NotFoundException::new);
    }

    @Override
    @Timed
    public User updateUser(Long userId, @Valid @NotNull CreateUserRequest request) {
        userDAO.findById(userId).orElseThrow(NotFoundException::new);

        User user = new User();
        user.setId(userId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        userDAO.update(user);
        return userDAO.findById(userId).orElse(null);
    }
}
