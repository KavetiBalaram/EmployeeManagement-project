package com.example.EmployeeManagementSystem.dto;

import com.example.EmployeeManagementSystem.entity.Department;

public record DepartmentResponse(Long id, String name, long employeeCount) {

    public static DepartmentResponse from(Department department, long employeeCount) {
        return new DepartmentResponse(department.getId(), department.getName(), employeeCount);
    }
}
