package com.twilio.task.manager.resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.twilio.task.manager.api.model.CreateTaskRequest;
import com.twilio.task.manager.api.model.Task;
import com.twilio.task.manager.db.TaskDAO;
import com.twilio.task.manager.kafka.KafkaEventPublisher;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TasksResourceTest {

    @Mock
    private TaskDAO taskDAO;

    @Mock
    private KafkaEventPublisher kafkaPublisher;

    private TasksResource resource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new TasksResource(taskDAO, kafkaPublisher, "tasks");
    }

    @Test
    void getAllTasks_returnsListFromDAO() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        when(taskDAO.findAll()).thenReturn(List.of(task));

        List<Task> result = resource.getAllTasks();

        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
        verify(taskDAO).findAll();
    }

    @Test
    void getTaskById_found_returnsTask() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        when(taskDAO.findById(1L)).thenReturn(Optional.of(task));

        Task result = resource.getTaskById(1L);

        assertEquals("Test Task", result.getTitle());
    }

    @Test
    void getTaskById_notFound_throws404() {
        when(taskDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> resource.getTaskById(99L));
    }

    @Test
    void createTask_setsFieldsAndReturnsCreated() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");
        request.setDescription("A description");
        request.setCreatorId(1L);

        Task saved = new Task();
        saved.setId(1L);
        saved.setTitle("New Task");

        when(taskDAO.create(any(Task.class))).thenReturn(1L);
        when(taskDAO.findById(1L)).thenReturn(Optional.of(saved));

        Task result = resource.createTask(request);

        assertEquals(1L, result.getId());
        assertEquals("New Task", result.getTitle());
        verify(taskDAO).create(any(Task.class));
    }

    @Test
    void createTask_defaultsStatusAndPriority() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Task");
        request.setCreatorId(1L);

        when(taskDAO.create(any(Task.class))).thenReturn(1L);
        when(taskDAO.findById(1L)).thenReturn(Optional.of(new Task()));

        resource.createTask(request);

        verify(taskDAO).create(argThat(task ->
                task.getStatus() == Task.StatusEnum.TODO &&
                task.getPriority() == Task.PriorityEnum.MEDIUM));
    }

    @Test
    void deleteTask_found_deletes() {
        when(taskDAO.findById(1L)).thenReturn(Optional.of(new Task()));

        resource.deleteTask(1L);

        verify(taskDAO).delete(1L);
    }

    @Test
    void deleteTask_notFound_throws404() {
        when(taskDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> resource.deleteTask(99L));
        verify(taskDAO, never()).delete(any());
    }

    @Test
    void updateTask_found_updatesAndReturns() {
        Task existing = new Task();
        existing.setId(1L);
        when(taskDAO.findById(1L)).thenReturn(Optional.of(existing));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Updated");
        request.setDescription("Updated desc");
        request.setCreatorId(1L);

        Task updated = new Task();
        updated.setId(1L);
        updated.setTitle("Updated");
        when(taskDAO.findById(1L)).thenReturn(Optional.of(updated));

        Task result = resource.updateTask(1L, request);

        assertEquals("Updated", result.getTitle());
        verify(taskDAO).update(any(Task.class));
    }

    @Test
    void updateTask_notFound_throws404() {
        when(taskDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                resource.updateTask(99L, new CreateTaskRequest()));
        verify(taskDAO, never()).update(any());
    }

    @Test
    void getAllTasks_daoThrowsException_propagates500() {
        when(taskDAO.findAll()).thenThrow(new RuntimeException("DB connection failed"));

        assertThrows(RuntimeException.class, () -> resource.getAllTasks());
    }

    @Test
    void getTaskById_daoThrowsException_propagates500() {
        when(taskDAO.findById(1L)).thenThrow(new RuntimeException("DB timeout"));

        assertThrows(RuntimeException.class, () -> resource.getTaskById(1L));
    }

    @Test
    void createTask_daoThrowsException_propagates500() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Task");
        request.setCreatorId(1L);

        when(taskDAO.create(any(Task.class))).thenThrow(new RuntimeException("DB write failed"));

        assertThrows(RuntimeException.class, () -> resource.createTask(request));
    }

    @Test
    void deleteTask_daoThrowsOnDelete_propagates500() {
        when(taskDAO.findById(1L)).thenReturn(Optional.of(new Task()));
        doThrow(new RuntimeException("DB error")).when(taskDAO).delete(1L);

        assertThrows(RuntimeException.class, () -> resource.deleteTask(1L));
    }

    @Test
    void updateTask_daoThrowsOnUpdate_propagates500() {
        when(taskDAO.findById(1L)).thenReturn(Optional.of(new Task()));
        doThrow(new RuntimeException("DB error")).when(taskDAO).update(any(Task.class));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Updated");
        request.setCreatorId(1L);

        assertThrows(RuntimeException.class, () -> resource.updateTask(1L, request));
    }

    @Test
    void createTask_createdButNotFoundOnFetch_returnsNull() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Task");
        request.setCreatorId(1L);

        when(taskDAO.create(any(Task.class))).thenReturn(1L);
        when(taskDAO.findById(1L)).thenReturn(Optional.empty());

        Task result = resource.createTask(request);

        assertNull(result);
    }
}
