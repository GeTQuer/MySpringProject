package com.getquer.tasktracker.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String loginPage() {
        return "login.html";
    }

    @GetMapping("/tasks")
    public String tasksPage() {
        return "tasks.html";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "/register.html";
    }
    @GetMapping("/admin")
    public String toAdminPanel(){
        return "/admin-panel.html";
    }

    @GetMapping("/admin/tasks")
    public String toAdminTasksPage() {
        return "/admin-tasks.html";
    }


}