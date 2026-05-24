package com.twilio.task.manager.db;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.twilio.task.manager.api.model.Project;

import java.util.Optional;

@RegisterBeanMapper(Project.class)
public interface ProjectDAO {
    @SqlQuery("SELECT * FROM projects WHERE id = :id")
    Optional<Project> findById(@Bind("id") Long id);

    @SqlQuery("SELECT * FROM projects WHERE owner_id = :ownerId")
    List<Project> findByOwnerId(@Bind("ownerId") Long ownerId);

    @SqlQuery("SELECT * FROM projects")
    List<Project> findAll();

    @SqlUpdate("INSERT INTO projects (name, description, owner_id) "
             + "VALUES (:name, :description, :ownerId)")
    @GetGeneratedKeys
    long create(@BindBean Project project);

    @SqlUpdate("UPDATE projects SET name = :name, description = :description, "
             + "owner_id = :ownerId, updated_at = NOW() "
             + "WHERE id = :id")
    void update(@BindBean Project project);

    @SqlUpdate("DELETE FROM projects WHERE id = :id")
    void delete(@Bind("id") Long id);
}
