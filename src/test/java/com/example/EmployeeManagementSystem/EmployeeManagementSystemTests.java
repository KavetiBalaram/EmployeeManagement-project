package com.example.EmployeeManagementSystem;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeManagementSystemTests {

    @Autowired
    private MockMvc mockMvc;

    // ---------------- departments ----------------

    @Test
    void createAndListDepartments() throws Exception {
        createDepartment("Engineering");

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[0].employeeCount").value(0));
    }

    @Test
    void duplicateDepartmentNameIsRejectedIgnoringCase() throws Exception {
        createDepartment("Engineering");

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"engineering\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void blankDepartmentNameIsRejected() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void updateDepartmentAndMissingDepartment() throws Exception {
        long id = createDepartment("Sales");

        mockMvc.perform(put("/api/departments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Global Sales\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Global Sales"));

        mockMvc.perform(get("/api/departments/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void departmentWithEmployeesCannotBeDeletedUntilEmpty() throws Exception {
        long deptId = createDepartment("Support");
        long empId = createEmployee("Ann", "Lee", "ann@acme.com", "4000", deptId);

        mockMvc.perform(delete("/api/departments/" + deptId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/employees/" + empId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/departments/" + deptId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/departments/" + deptId))
                .andExpect(status().isNotFound());
    }

    // ---------------- employees ----------------

    @Test
    void createEmployeeWithDepartment() throws Exception {
        long deptId = createDepartment("Engineering");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ann", "Lee", "  Ann.Lee@Acme.com ", "5000.50", deptId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("ann.lee@acme.com"))
                .andExpect(jsonPath("$.salary").value(5000.5))
                .andExpect(jsonPath("$.department.name").value("Engineering"));
    }

    @Test
    void invalidEmployeeReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("", "Lee", "not-an-email", "-5", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDepartmentReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ann", "Lee", "ann@acme.com", "4000", 999999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("Department not found")));
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        createEmployee("Ann", "Lee", "ann@acme.com", "4000", null);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Anna", "Lee", "ANN@acme.com", "4000", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateEmployeeMovesBetweenDepartmentsAndCanClearDepartment() throws Exception {
        long eng = createDepartment("Engineering");
        long hr = createDepartment("HR");
        long empId = createEmployee("Ann", "Lee", "ann@acme.com", "4000", eng);

        mockMvc.perform(put("/api/employees/" + empId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ann", "Lee", "ann@acme.com", "4500", hr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department.name").value("HR"))
                .andExpect(jsonPath("$.salary").value(4500.0));

        mockMvc.perform(put("/api/employees/" + empId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ann", "Lee", "ann@acme.com", "4500", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").isEmpty());
    }

    @Test
    void updateCannotTakeAnotherEmployeesEmailButCanKeepItsOwn() throws Exception {
        createEmployee("Ann", "Lee", "a@acme.com", "4000", null);
        long bobId = createEmployee("Bob", "Stone", "b@acme.com", "4000", null);

        mockMvc.perform(put("/api/employees/" + bobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Bob", "Stone", "a@acme.com", "4000", null)))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/employees/" + bobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Robert", "Stone", "b@acme.com", "4000", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Robert"));
    }

    @Test
    void deleteEmployeeThenItIsGone() throws Exception {
        long empId = createEmployee("Ann", "Lee", "ann@acme.com", "4000", null);

        mockMvc.perform(delete("/api/employees/" + empId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/" + empId))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/employees/" + empId))
                .andExpect(status().isNotFound());
    }

    // ---------------- search, filter, paging ----------------

    @Test
    void searchFilterSortAndPaginate() throws Exception {
        long eng = createDepartment("Engineering");
        long hr = createDepartment("HR");
        createEmployee("Ann", "Lee", "ann@acme.com", "5000", eng);
        createEmployee("Bob", "Stone", "bob@acme.com", "4000", eng);
        createEmployee("Cara", "Lane", "cara@acme.com", "4500", hr);

        mockMvc.perform(get("/api/employees").param("departmentId", String.valueOf(eng)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/api/employees").param("q", "STONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Bob"));

        mockMvc.perform(get("/api/employees")
                        .param("q", "cara")
                        .param("departmentId", String.valueOf(eng)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mockMvc.perform(get("/api/employees")
                        .param("size", "2")
                        .param("page", "0")
                        .param("sort", "firstName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].firstName").value("Ann"))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/employees")
                        .param("size", "2")
                        .param("page", "1")
                        .param("sort", "firstName,asc"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Cara"));
    }

    @Test
    void invalidSortFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees").param("sort", "password,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void departmentListShowsEmployeeCounts() throws Exception {
        long eng = createDepartment("Engineering");
        createDepartment("Empty Dept");
        createEmployee("Ann", "Lee", "ann@acme.com", "5000", eng);
        createEmployee("Bob", "Stone", "bob@acme.com", "4000", eng);

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Engineering')].employeeCount[0]").value(2))
                .andExpect(jsonPath("$[?(@.name=='Empty Dept')].employeeCount[0]").value(0));
    }

    // ---------------- helpers ----------------

    private long createDepartment(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createEmployee(String first, String last, String email,
                                String salary, Long deptId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(first, last, email, salary, deptId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String employeeJson(String first, String last, String email,
                                String salary, Long deptId) {
        return """
                {"firstName":"%s","lastName":"%s","email":"%s","salary":%s,"departmentId":%s}
                """.formatted(first, last, email, salary, deptId);
    }
}
