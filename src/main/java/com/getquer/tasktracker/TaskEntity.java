package com.getquer.tasktracker;

import jakarta.persistence.*;
import org.springframework.scheduling.config.Task;


@Entity
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "ID")
    private Long id;

    @Column(name = "Content")
    private String content;

    @Column(name = "employee")
    private String fullNameEmployee;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    private TaskStatus status;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId()
    {
        return id;
    }
    public void setId(Long id)
    {
        this.id = id;
    }


    public void setContent(String content)
    {
        this.content = content;
    }
    public String getContent(){
        return content;
    }


    public void setEmployee(String name){
        this.fullNameEmployee = name;
    }
    public String getEmployee(){
        return this.fullNameEmployee;
    }

    public TaskStatus getStatus(){
        return this.status;
    }
    public void setStatus(TaskStatus status)
    {
        this.status = status;
    }


}
