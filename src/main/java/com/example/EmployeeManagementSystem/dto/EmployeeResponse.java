package com.example.EmployeeManagementSystem.dto;

import java.math.BigDecimal;

import com.example.EmployeeManagementSystem.entity.Employee;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String imageUrl,
        BigDecimal salary,
        DepartmentSummary department) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmail(),
                e.getImageUrl(),
                e.getSalary(),
                DepartmentSummary.from(e.getDepartment()));
    }
}
