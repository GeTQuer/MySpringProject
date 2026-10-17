package com.getquer.tasktracker.controllers;


import com.getquer.tasktracker.requestDTO.ReportRequest;
import com.getquer.tasktracker.responseDTO.DepartmentReportDTO;
import com.getquer.tasktracker.responseDTO.AssigneeDTO;
import com.getquer.tasktracker.responseDTO.ReportJobCreatedDTO;
import com.getquer.tasktracker.responseDTO.ReportJobStatusDTO;
import com.getquer.tasktracker.responseDTO.UserDTO;
import com.getquer.tasktracker.service.ReportService;
import com.getquer.tasktracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final ReportService reportService;
    public UserController(UserService userService, ReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
    }


    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(
                userService.getCurrentUser(authentication.getName())
        );
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/department")
    public ResponseEntity<Page<UserDTO>> getAllUsersForManager(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ){
        String username = authentication.getName();
        return ResponseEntity.ok(userService.findAllUsersByDepartment(username,page,size));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/assignable")
    public ResponseEntity<List<AssigneeDTO>> getAssignableUsers(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userService.getAssignableUsers(authentication.getName())
        );
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/reports/{jobId}/result")
    public ResponseEntity<DepartmentReportDTO> getReport(
            @PathVariable UUID jobId,
            Authentication authentication
            ){
        return ResponseEntity.ok(reportService.getReport(jobId, authentication.getName()));
    }
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/reports")
    public ResponseEntity<ReportJobCreatedDTO> createReport(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication
    ) {
        ReportJobCreatedDTO job = reportService.requestReport(
                request,
                authentication.getName()
        );

        return ResponseEntity.accepted().body(job);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/reports/{jobId}")
    public ResponseEntity<ReportJobStatusDTO> getReportStatus(
            @PathVariable UUID jobId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reportService.getStatus(jobId, authentication.getName()));
    }
}
