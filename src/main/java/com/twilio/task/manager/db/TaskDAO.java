package com.twilio.task.manager.db;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.twilio.task.manager.api.model.Task;

import java.util.Optional;

@RegisterBeanMapper(Task.class)
public interface TaskDAO {
    @SqlQuery("SELECT * FROM tasks WHERE id = :id")
    Optional<Task> findById(@Bind("id") Long id);

    @SqlQuery("SELECT * FROM tasks")
    List<Task> findAll();

    @SqlQuery("SELECT * FROM tasks WHERE project_id = :projectId")
    Optional<List<Task> >findByProjectId(@Bind("projectId") Long projectId);

    @SqlQuery("SELECT * FROM tasks WHERE assignee_id = :assigneeId")
    Optional<List<Task> >findByAssigneeId(@Bind("assigneeId") Long assigneeId);

    @SqlQuery("SELECT * FROM tasks WHERE creator_id = :creatorId")
    Optional<List<Task> >findByCreatorId(@Bind("creatorId") Long creatorId);

    @SqlUpdate("INSERT INTO tasks (title, description, status, priority, project_id, assignee_id, creator_id, due_date) "
             + "VALUES (:title, :description, :status, :priority, :projectId, :assigneeId, :creatorId, :dueDate)")
    @GetGeneratedKeys
    long create(@BindBean Task task);

    @SqlUpdate("UPDATE tasks SET title = :title, description = :description, status = :status, "
             + "priority = :priority, project_id = :projectId, assignee_id = :assigneeId, "
             + "creator_id = :creatorId, due_date = :dueDate, updated_at = NOW() "
             + "WHERE id = :id")
    void update(@BindBean Task task);

    @SqlUpdate("DELETE FROM tasks WHERE id = :id")
    void delete(@Bind("id") Long id);
}