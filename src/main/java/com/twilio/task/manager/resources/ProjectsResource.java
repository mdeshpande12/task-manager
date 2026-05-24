package com.twilio.task.manager.resources;

import java.util.List;

import com.codahale.metrics.annotation.Timed;
import com.twilio.task.manager.api.ProjectsApi;
import com.twilio.task.manager.api.model.CreateProjectRequest;
import com.twilio.task.manager.api.model.Project;
import com.twilio.task.manager.db.ProjectDAO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;

public class ProjectsResource implements ProjectsApi {
    private final ProjectDAO projectDAO;

    public ProjectsResource(ProjectDAO projectDAO) {
        this.projectDAO = projectDAO;
    }

    @Override
    @Timed
    public Project createProject(@Valid @NotNull CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwnerId(request.getOwnerId());

        long id = projectDAO.create(project);
        return projectDAO.findById(id).orElse(null);
    }

    @Override
    @Timed
    public void deleteProject(Long projectId) {
        projectDAO.findById(projectId).orElseThrow(NotFoundException::new);
        projectDAO.delete(projectId);
    }

    @Override
    @Timed
    public List<Project> getAllProjects() {
        return projectDAO.findAll();
    }

    @Override
    @Timed
    public Project getProjectById(Long projectId) {
        return projectDAO.findById(projectId).orElseThrow(NotFoundException::new);
    }

    @Override
    @Timed
    public Project updateProject(Long projectId, @Valid @NotNull CreateProjectRequest request) {
        projectDAO.findById(projectId).orElseThrow(NotFoundException::new);

        Project project = new Project();
        project.setId(projectId);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwnerId(request.getOwnerId());

        projectDAO.update(project);
        return projectDAO.findById(projectId).orElse(null);
    }
}
