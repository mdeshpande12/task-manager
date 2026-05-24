package com.twilio.task.manager.resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.twilio.task.manager.api.model.CreateProjectRequest;
import com.twilio.task.manager.api.model.Project;
import com.twilio.task.manager.db.ProjectDAO;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProjectsResourceTest {

    @Mock
    private ProjectDAO projectDAO;

    private ProjectsResource resource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new ProjectsResource(projectDAO);
    }

    @Test
    void getAllProjects_returnsListFromDAO() {
        Project project = new Project();
        project.setId(1L);
        project.setName("My Project");
        when(projectDAO.findAll()).thenReturn(List.of(project));

        List<Project> result = resource.getAllProjects();

        assertEquals(1, result.size());
        assertEquals("My Project", result.get(0).getName());
    }

    @Test
    void getProjectById_found_returnsProject() {
        Project project = new Project();
        project.setId(1L);
        project.setName("My Project");
        when(projectDAO.findById(1L)).thenReturn(Optional.of(project));

        Project result = resource.getProjectById(1L);

        assertEquals("My Project", result.getName());
    }

    @Test
    void getProjectById_notFound_throws404() {
        when(projectDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> resource.getProjectById(99L));
    }

    @Test
    void createProject_setsFieldsAndReturnsCreated() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("New Project");
        request.setDescription("A description");
        request.setOwnerId(1L);

        Project saved = new Project();
        saved.setId(1L);
        saved.setName("New Project");

        when(projectDAO.create(any(Project.class))).thenReturn(1L);
        when(projectDAO.findById(1L)).thenReturn(Optional.of(saved));

        Project result = resource.createProject(request);

        assertEquals(1L, result.getId());
        assertEquals("New Project", result.getName());
        verify(projectDAO).create(any(Project.class));
    }

    @Test
    void deleteProject_found_deletes() {
        when(projectDAO.findById(1L)).thenReturn(Optional.of(new Project()));

        resource.deleteProject(1L);

        verify(projectDAO).delete(1L);
    }

    @Test
    void deleteProject_notFound_throws404() {
        when(projectDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> resource.deleteProject(99L));
        verify(projectDAO, never()).delete(any());
    }

    @Test
    void updateProject_found_updatesAndReturns() {
        when(projectDAO.findById(1L)).thenReturn(Optional.of(new Project()));

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Updated Project");
        request.setDescription("Updated desc");
        request.setOwnerId(2L);

        Project updated = new Project();
        updated.setId(1L);
        updated.setName("Updated Project");
        when(projectDAO.findById(1L)).thenReturn(Optional.of(updated));

        Project result = resource.updateProject(1L, request);

        assertEquals("Updated Project", result.getName());
        verify(projectDAO).update(any(Project.class));
    }

    @Test
    void updateProject_notFound_throws404() {
        when(projectDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                resource.updateProject(99L, new CreateProjectRequest()));
        verify(projectDAO, never()).update(any());
    }

    @Test
    void getAllProjects_daoThrowsException_propagates500() {
        when(projectDAO.findAll()).thenThrow(new RuntimeException("DB connection failed"));

        assertThrows(RuntimeException.class, () -> resource.getAllProjects());
    }

    @Test
    void getProjectById_daoThrowsException_propagates500() {
        when(projectDAO.findById(1L)).thenThrow(new RuntimeException("DB timeout"));

        assertThrows(RuntimeException.class, () -> resource.getProjectById(1L));
    }

    @Test
    void createProject_daoThrowsException_propagates500() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Project");
        request.setOwnerId(1L);

        when(projectDAO.create(any(Project.class))).thenThrow(new RuntimeException("DB write failed"));

        assertThrows(RuntimeException.class, () -> resource.createProject(request));
    }

    @Test
    void deleteProject_daoThrowsOnDelete_propagates500() {
        when(projectDAO.findById(1L)).thenReturn(Optional.of(new Project()));
        doThrow(new RuntimeException("DB error")).when(projectDAO).delete(1L);

        assertThrows(RuntimeException.class, () -> resource.deleteProject(1L));
    }

    @Test
    void updateProject_daoThrowsOnUpdate_propagates500() {
        when(projectDAO.findById(1L)).thenReturn(Optional.of(new Project()));
        doThrow(new RuntimeException("DB error")).when(projectDAO).update(any(Project.class));

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Updated");
        request.setOwnerId(1L);

        assertThrows(RuntimeException.class, () -> resource.updateProject(1L, request));
    }

    @Test
    void createProject_createdButNotFoundOnFetch_returnsNull() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Project");
        request.setOwnerId(1L);

        when(projectDAO.create(any(Project.class))).thenReturn(1L);
        when(projectDAO.findById(1L)).thenReturn(Optional.empty());

        Project result = resource.createProject(request);

        assertNull(result);
    }
}
