package com.twilio.task.manager.db;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.twilio.task.manager.api.model.User;

import java.util.Optional;

@RegisterBeanMapper(User.class)
public interface UserDAO {
    @SqlQuery("SELECT * FROM users WHERE id = :id")
    Optional<User> findById(@Bind("id") Long id);

    @SqlQuery("SELECT * FROM users WHERE username = :username")
    Optional<User> findByUsername(@Bind("username") String username);

    @SqlQuery("SELECT * FROM users WHERE email = :email")
    Optional<User> findByEmail(@Bind("email") String email);

    @SqlQuery("SELECT * FROM users")
    List<User> findAll();

    @SqlUpdate("INSERT INTO users (username, email, full_name) "
             + "VALUES (:username, :email, :fullName)")
    @GetGeneratedKeys
    long create(@BindBean User user);

    @SqlUpdate("UPDATE users SET username = :username, email = :email, "
             + "full_name = :fullName, updated_at = NOW() "
             + "WHERE id = :id")
    void update(@BindBean User user);

    @SqlUpdate("DELETE FROM users WHERE id = :id")
    void delete(@Bind("id") Long id);
}
