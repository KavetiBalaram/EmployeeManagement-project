package com.example.EmployeeManagementSystem.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EmployeeManagementSystem.dto.DepartmentRequest;
import com.example.EmployeeManagementSystem.dto.DepartmentResponse;
import com.example.EmployeeManagementSystem.entity.Department;
import com.example.EmployeeManagementSystem.exception.ConflictException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
import com.example.EmployeeManagementSystem.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentResponse> getAll() {
        Map<Long, Long> counts = employeeCounts();
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(d -> DepartmentResponse.from(d, counts.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    public DepartmentResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Department already exists: " + name);
        }

        Department department = new Department();
        department.setName(name);
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = find(id);
        String name = request.name().trim();

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Department already exists: " + name);
        }

        department.setName(name);
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public void delete(Long id) {
        Department department = find(id);

        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException(
                    "Department '" + department.getName()
                            + "' still has employees. Move or delete them first.");
        }

        departmentRepository.delete(department);
    }

    private Department find(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Map<Long, Long> employeeCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : employeeRepository.countByDepartment()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.from(
                department, employeeCounts().getOrDefault(department.getId(), 0L));
    }
}
