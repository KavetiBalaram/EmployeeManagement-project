package com.example.EmployeeManagementSystem.dto;

import com.example.EmployeeManagementSystem.entity.Department;

public record DepartmentSummary(Long id, String name) {

    public static DepartmentSummary from(Department department) {
        return department == null ? null : new DepartmentSummary(department.getId(), department.getName());
    }
}
