package com.getquer.tasktracker.request;

import com.getquer.tasktracker.Grades.Seniority;

public record SignupRequest(String username,
                            String password,
                            String department,
                            Seniority seniority)
{

}
