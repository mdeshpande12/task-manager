package com.twilio.task.manager.resources;

import java.util.List;

import com.codahale.metrics.annotation.Timed;
import com.twilio.task.manager.api.TasksApi;
import com.twilio.task.manager.api.model.CreateTaskRequest;
import com.twilio.task.manager.api.model.Task;
import com.twilio.task.manager.db.TaskDAO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;

public class TasksResource implements TasksApi {
    private final TaskDAO taskDAO;

    public TasksResource(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    @Override
    @Timed
    public Task createTask(@Valid @NotNull CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null
                ? Task.StatusEnum.fromValue(request.getStatus().value())
                : Task.StatusEnum.TODO);
        task.setPriority(request.getPriority() != null
                ? Task.PriorityEnum.fromValue(request.getPriority().value())
                : Task.PriorityEnum.MEDIUM);
        task.setProjectId(request.getProjectId());
        task.setAssigneeId(request.getAssigneeId());
        task.setCreatorId(request.getCreatorId());
        task.setDueDate(request.getDueDate());

        long id = taskDAO.create(task);
        return taskDAO.findById(id).orElse(null);
    }

    @Override
    @Timed
    public void deleteTask(Long taskId) {
        taskDAO.findById(taskId).orElseThrow(NotFoundException::new);
        taskDAO.delete(taskId);
    }

    @Override
    @Timed
    public List<Task> getAllTasks() {
        return taskDAO.findAll();
    }

    @Override
    @Timed
    public Task getTaskById(Long taskId) {
        return taskDAO.findById(taskId).orElseThrow(NotFoundException::new);
    }

    @Override
    @Timed
    public Task updateTask(Long taskId, @Valid @NotNull CreateTaskRequest request) {
        taskDAO.findById(taskId).orElseThrow(NotFoundException::new);

        Task task = new Task();
        task.setId(taskId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null
                ? Task.StatusEnum.fromValue(request.getStatus().value())
                : Task.StatusEnum.TODO);
        task.setPriority(request.getPriority() != null
                ? Task.PriorityEnum.fromValue(request.getPriority().value())
                : Task.PriorityEnum.MEDIUM);
        task.setProjectId(request.getProjectId());
        task.setAssigneeId(request.getAssigneeId());
        task.setCreatorId(request.getCreatorId());
        task.setDueDate(request.getDueDate());

        taskDAO.update(task);
        return taskDAO.findById(taskId).orElse(null);
    }
}
