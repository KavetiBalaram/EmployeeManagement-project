package com.example.EmployeeManagementSystem.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EmployeeManagementSystem.dto.EmployeeRequest;
import com.example.EmployeeManagementSystem.dto.EmployeeResponse;
import com.example.EmployeeManagementSystem.entity.Department;
import com.example.EmployeeManagementSystem.entity.Employee;
import com.example.EmployeeManagementSystem.exception.BadRequestException;
import com.example.EmployeeManagementSystem.exception.ConflictException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
import com.example.EmployeeManagementSystem.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "firstName", "lastName", "email", "salary");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<EmployeeResponse> search(String query, Long departmentId, Pageable pageable) {
        validateSort(pageable.getSort());
        return employeeRepository
                .findAll(buildSpec(query, departmentId), pageable)
                .map(EmployeeResponse::from);
    }

    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(find(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use: " + email);
        }

        Employee employee = new Employee();
        apply(employee, request, email);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = find(id);

        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email already in use: " + email);
        }

        apply(employee, request, email);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(find(id));
    }

    // ---- helpers ----

    private Employee find(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private void apply(Employee employee, EmployeeRequest request, String email) {
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setImageUrl(blankToNull(request.imageUrl()));
        employee.setSalary(request.salary());
        employee.setDepartment(resolveDepartment(request.departmentId()));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found: " + departmentId));
    }

    private Specification<Employee> buildSpec(String query, Long departmentId) {
        Specification<Employee> spec = (root, cq, cb) -> cb.conjunction();

        if (departmentId != null) {
            spec = spec.and((root, cq, cb) ->
                    cb.equal(root.get("department").get("id"), departmentId));
        }

        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like),
                    cb.like(cb.lower(root.get("email")), like)));
        }

        return spec;
    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Cannot sort by '" + order.getProperty()
                                + "'. Allowed: " + SORTABLE_FIELDS);
            }
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
