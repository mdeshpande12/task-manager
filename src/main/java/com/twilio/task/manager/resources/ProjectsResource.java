package com.twilio.task.manager.resources;

import java.util.List;

import com.codahale.metrics.annotation.Timed;
import com.twilio.task.manager.api.ProjectsApi;
import com.twilio.task.manager.api.model.CreateProjectRequest;
import com.twilio.task.manager.api.model.Project;
import com.twilio.task.manager.db.ProjectDAO;
import com.twilio.task.manager.kafka.KafkaEventPublisher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;

public class ProjectsResource implements ProjectsApi {
    private final ProjectDAO projectDAO;
    private final KafkaEventPublisher kafkaPublisher;
    private final String topic;

    public ProjectsResource(ProjectDAO projectDAO, KafkaEventPublisher kafkaPublisher, String topic) {
        this.projectDAO = projectDAO;
        this.kafkaPublisher = kafkaPublisher;
        this.topic = topic;
    }

    @Override
    @Timed
    public Project createProject(@Valid @NotNull CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwnerId(request.getOwnerId());

        long id = projectDAO.create(project);
        Project created = projectDAO.findById(id).orElse(null);
        kafkaPublisher.publish(topic, String.valueOf(id), created);
        return created;
    }

    @Override
    @Timed
    public void deleteProject(Long projectId) {
        projectDAO.findById(projectId).orElseThrow(NotFoundException::new);
        projectDAO.delete(projectId);
        kafkaPublisher.publish(topic, String.valueOf(projectId), java.util.Map.of("id", projectId, "event", "deleted"));
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
        Project updated = projectDAO.findById(projectId).orElse(null);
        kafkaPublisher.publish(topic, String.valueOf(projectId), updated);
        return updated;
    }
}
