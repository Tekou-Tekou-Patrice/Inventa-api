package com.onescompany.Inventa;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.onescompany.Inventa.domain.FieldTaskRepository;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.domain.WorkerRepository;
import com.onescompany.Inventa.domain.Worker;
import com.onescompany.Inventa.domain.StockItemRepository;
import com.onescompany.Inventa.domain.StockMovementRepository;
import com.onescompany.Inventa.domain.StockOrderRepository;
import com.onescompany.Inventa.domain.Payment;
import com.onescompany.Inventa.domain.PaymentRepository;
import com.onescompany.Inventa.domain.PayrollSettings;
import com.onescompany.Inventa.domain.PayrollSettingsRepository;
import com.onescompany.Inventa.domain.PayrollPaymentRequest;
import com.onescompany.Inventa.domain.PayrollPaymentRequestRepository;
import com.onescompany.Inventa.security.FieldDataInitializer;
import com.onescompany.Inventa.security.AppUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PlantationApiTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private WorkerRepository workers;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private PayrollSettingsRepository payrollSettings;

    @Autowired
    private PayrollPaymentRequestRepository payrollPaymentRequests;

    @Autowired
    private StockItemRepository stockItems;

    @Autowired
    private StockMovementRepository stockMovements;

    @Autowired
    private StockOrderRepository stockOrders;

    @Autowired
    private FieldTaskRepository tasks;

    @Autowired
    private AppUserRepository appUsers;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlantationFieldRepository fields;

    @Autowired
    private FieldDataInitializer fieldDataInitializer;

    @Test
    void healthIsPublicButBusinessDataRequiresLogin() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mvc.perform(get("/api/workers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assistantRequestsPayrollDirectorApprovesThenAssistantConfirmsPayment() throws Exception {
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Worker worker = workers.save(new Worker(
                "Paie cumulée " + System.nanoTime(), "Nord", 208_000, manager.getField()));
        PayrollSettings config = payrollSettings.findById(PayrollSettings.SINGLETON_ID)
                .orElseGet(() -> new PayrollSettings(90_000, 208));
        config.update(90_000, 208);
        payrollSettings.save(config);
        LocalDate today = LocalDate.now();
        String date = today.toString();
        mvc.perform(put("/api/attendance/{workerId}?date={date}", worker.getId(), date)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkIn":"%sT07:00:00Z","checkOut":"%sT15:00:00Z"}
                                """.formatted(date, date)))
                .andExpect(status().isOk());

        String through = YearMonth.from(today).toString();
        mvc.perform(get("/api/payroll/due?through={through}", through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")].amount").value(8000));

        mvc.perform(post("/api/payroll/requests")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"through":"%s","scheduledPaymentDate":"%s"}
                                """.formatted(worker.getId(), through, date)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(8000))
                .andExpect(jsonPath("$.status").value("PENDING"));
        long requestId = payrollPaymentRequests
                .findFirstByWorker_IdAndStatusOrderByRequestedAtDesc(
                        worker.getId(), PayrollPaymentRequest.Status.PENDING)
                .orElseThrow()
                .getId();
        mvc.perform(get("/api/payroll/requests")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + requestId + ")].status").exists());

        mvc.perform(put("/api/payroll/requests/{id}/paid", requestId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paidDate":"%s"}
                                """.formatted(date)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/payroll/due?through={through}", through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")].amount").value(8000));

        mvc.perform(put("/api/payroll/requests/{id}/decision", requestId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":true}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/payroll/requests/{id}/decision", requestId)
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/payroll/requests")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"through":"%s","scheduledPaymentDate":"%s"}
                                """.formatted(worker.getId(), through, date)))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/payroll/requests/{id}/paid", requestId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paidDate":"%s"}
                                """.formatted(date)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").value(date));
        mvc.perform(get("/api/payments/monthly?month={month}", through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")].amount").value(8000))
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")].paidThroughEnd")
                        .value(YearMonth.from(today).atEndOfMonth().toString()));
        mvc.perform(get("/api/payments/monthly?month={month}", through)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payments/monthly?month={month}", YearMonth.from(today).minusMonths(1))
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")]").isEmpty());
        mvc.perform(get("/api/payroll/due?through={through}", through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")].amount").value(0));

        mvc.perform(put("/api/payroll/requests/{id}/paid", requestId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paidDate":"%s"}
                                """.formatted(date)))
                .andExpect(status().isConflict());
    }

    @Test
    void assistantCanPayWorkerDirectlyAndCannotRecordPaymentAsAnotherRole() throws Exception {
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Worker worker = workers.save(new Worker(
                "Paiement direct " + System.nanoTime(), "Nord", 208_000, manager.getField()));
        PayrollSettings config = payrollSettings.findById(PayrollSettings.SINGLETON_ID)
                .orElseGet(() -> new PayrollSettings(90_000, 208));
        config.update(90_000, 208);
        payrollSettings.save(config);
        LocalDate today = LocalDate.now();
        String date = today.toString();
        String through = YearMonth.from(today).toString();
        mvc.perform(put("/api/attendance/{workerId}?date={date}", worker.getId(), date)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkIn":"%sT07:00:00Z","checkOut":"%sT15:00:00Z"}
                                """.formatted(date, date)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/payroll/workers/{workerId}/payments?through={through}", worker.getId(), through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value(worker.getId()))
                .andExpect(jsonPath("$.amount").value(8000))
                .andExpect(jsonPath("$.paidThroughEnd").value(YearMonth.from(today).atEndOfMonth().toString()));
        mvc.perform(post("/api/payroll/workers/{workerId}/payments?through={through}", worker.getId(), through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/payroll/workers/{workerId}/payments?through={through}", worker.getId(), through)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payroll/due?through={through}", through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == " + worker.getId() + ")].amount").value(0));
        mvc.perform(get("/api/reports/monthly?month={month}", through)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance[?(@.workerId == " + worker.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$.payments[?(@.workerId == " + worker.getId() + " && @.amount == 8000)]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.validatedDates").isArray());
        mvc.perform(get("/api/reports/monthly?month={month}", through)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/monthly?month={month}", through)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance[?(@.workerId == " + worker.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$.payments[?(@.workerId == " + worker.getId() + " && @.amount == 8000)]")
                        .isNotEmpty());
        mvc.perform(put("/api/reports/daily/validate?date={date}", date)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/monthly?month={month}", through)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance[?(@.workerId == " + worker.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$.payments[?(@.workerId == " + worker.getId() + " && @.amount == 8000)]")
                        .isNotEmpty());
    }

    @Test
    void rolePermissionsAreEnforcedOnTheServer() throws Exception {
        mvc.perform(post("/api/workers")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Koffi Yao","groupName":"Nord","salary":3500,"phoneNumber":"+2250102030405"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/workers")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Koffi Yao","groupName":"Nord","salary":3500,"phoneNumber":"+2250102030405"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Koffi Yao"))
                .andExpect(jsonPath("$.phoneNumber").value("+2250102030405"));

        Long workerId = workers.findAll().get(0).getId();
        mvc.perform(post("/api/payments")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"amount":3500}
                                """.formatted(workerId)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/payments")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"amount":3500}
                                """.formatted(workerId)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/workers/{id}/salary", workerId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salary":90000}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/workers/{id}/salary", workerId)
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salary":90000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(90000))
                .andExpect(jsonPath("$.salaryIsIndividual").value(true));

        mvc.perform(put("/api/workers/{id}/salary", workerId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salary":120000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(120000))
                .andExpect(jsonPath("$.salaryIsIndividual").value(true));
    }

    @Test
    void loginIdentityReportsDatabaseRole() throws Exception {
        mvc.perform(get("/api/auth/me")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FIELD_MANAGER"))
                .andExpect(jsonPath("$.fieldId").exists());
    }

    @Test
    void assistantCanUpdateWorkerPhoneAndArchiveWorkerWithoutDeletingHistory() throws Exception {
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Worker worker = workers.save(new Worker(
                "Assistant contact " + System.nanoTime(),
                "Nord",
                90_000,
                "+2250102030405",
                manager.getField()));

        mvc.perform(get("/api/workers")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + worker.getId() + ")]").exists());
        mvc.perform(put("/api/workers/{id}/contact", worker.getId())
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(""));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/workers/{id}", worker.getId())
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        org.junit.jupiter.api.Assertions.assertTrue(
                workers.findById(worker.getId()).orElseThrow().getPhoneNumber().isEmpty());
        mvc.perform(put("/api/workers/{id}/contact", worker.getId())
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void fieldManagerCannotReadOrRecordPayments() throws Exception {
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Worker worker = workers.save(new Worker(
                "Paie terrain " + System.nanoTime(), "Nord", 3500, manager.getField()));
        payments.save(new Payment(worker, 3500, "assistant"));

        mvc.perform(get("/api/payments")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payroll/due?through=2026-10")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/payments")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"amount":3500}
                                """.formatted(worker.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyTheFirstDirectorCanRegisterWithoutAuthentication() throws Exception {
        var initialDirector = appUsers.findByUsername("director").orElseThrow();
        appUsers.delete(initialDirector);
        appUsers.flush();
        String username = "first-director-" + System.nanoTime();
        String request = """
                {"username":"%s","password":"Directeur42!","phoneNumber":"+2250700000000"}
                """.formatted(username);
        try {
            mvc.perform(get("/api/auth/director-registration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true));

            mvc.perform(post("/api/auth/register-director")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"invalid-short","password":"abcd","phoneNumber":"+2250700000000"}
                                    """))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/api/auth/register-director")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"invalid-long","password":"1234567890123","phoneNumber":"+2250700000000"}
                                    """))
                    .andExpect(status().isBadRequest());

            mvc.perform(post("/api/auth/register-director")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.role").value("DIRECTOR"));

            mvc.perform(get("/api/auth/director-registration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(false));

            mvc.perform(post("/api/auth/register-director")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"second-director","password":"SecondDir42!","phoneNumber":"+2250700000001"}
                                    """))
                    .andExpect(status().isConflict());

            mvc.perform(get("/api/auth/me")
                            .with(httpBasic(username, "Directeur42!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("DIRECTOR"));
        } finally {
            appUsers.findByUsername(username).ifPresent(appUsers::delete);
            appUsers.save(new com.onescompany.Inventa.security.AppUser(
                    "director",
                    passwordEncoder.encode("Director42!!"),
                    com.onescompany.Inventa.security.UserRole.DIRECTOR));
            appUsers.flush();
        }
    }

    @Test
    void fieldManagerCanDeactivateWorkersAndUpdateTaskStatus() throws Exception {
        String credentials = "FieldMgr42!!";
        mvc.perform(post("/api/workers")
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Worker lifecycle test","groupName":"Nord","salary":3500,"phoneNumber":"+2250102030405"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long workerId = workers.findAll().stream()
                .filter(worker -> worker.getName().equals("Worker lifecycle test"))
                .findFirst().orElseThrow().getId();
        Long secondWorkerId = workers.save(
                new Worker("Second task assignee test", "Nord", 3500, defaultField())).getId();

        mvc.perform(put("/api/workers/{id}/active", workerId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(put("/api/workers/{id}/active", workerId)
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/tasks")
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Task status test","assigneeIds":[%d,%d]}
                                """.formatted(workerId, secondWorkerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TO_DO"))
                .andExpect(jsonPath("$.type").value("ASSIGNED_WORKERS"))
                .andExpect(jsonPath("$.assigneeIds.length()").value(2));

        var createdTask = tasks.findAll().stream()
                .filter(task -> task.getTitle().equals("Task status test"))
                .findFirst().orElseThrow();
        Long taskId = createdTask.getId();

        mvc.perform(put("/api/tasks/{id}/status", taskId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/tasks/{id}/status", taskId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE","actualCost":125000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.actualCost").value(125000))
                .andExpect(jsonPath("$.seasonalWorkerCount").value(0))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mvc.perform(post("/api/tasks")
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Seasonal task test","type":"SEASONAL_WORKERS","assigneeIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SEASONAL_WORKERS"))
                .andExpect(jsonPath("$.assigneeIds").isEmpty());

        Long seasonalTaskId = tasks.findAll().stream()
                .filter(task -> task.getTitle().equals("Seasonal task test"))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/tasks/{id}/status", seasonalTaskId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(put("/api/tasks/{id}/status", seasonalTaskId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE","actualCost":50000,"seasonalWorkerCount":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonalWorkerCount").value(9))
                .andExpect(jsonPath("$.actualCost").value(50000));
    }

    @Test
    void monthlyAttendanceReturnsOnlyTheRequestedMonth() throws Exception {
        String credentials = "FieldMgr42!!";
        Long workerId = workers.save(
                new Worker("Monthly attendance test", "Nord", 3500, defaultField())).getId();
        java.time.YearMonth month = java.time.YearMonth.now();
        String date = java.time.LocalDate.now().toString();
        java.time.Instant checkIn = java.time.Instant.now()
                .minusSeconds(3600)
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        java.time.Instant checkOut = java.time.Instant.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        mvc.perform(put("/api/attendance/{workerId}?date={date}", workerId, date)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkIn":"%s"}
                                """.formatted(checkIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIn").value(checkIn.toString()));

        mvc.perform(put("/api/attendance/{workerId}?date={date}", workerId, date)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkOut":"%s"}
                                """.formatted(checkOut)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIn").value(checkIn.toString()))
                .andExpect(jsonPath("$.checkOut").value(checkOut.toString()));

        mvc.perform(put("/api/attendance/{workerId}?date={date}", workerId, month.minusMonths(1).atEndOfMonth())
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ABSENT"}
                                """))
                .andExpect(status().isConflict());

        Long nextMonthWorkerId = workers.save(
                                new Worker("Next month attendance test", "Nord", 3500, defaultField())).getId();
        String nextMonthDate = month.plusMonths(1).atDay(1).toString();
        mvc.perform(put("/api/attendance/{workerId}?date={date}", nextMonthWorkerId, nextMonthDate)
                                        .with(httpBasic("terrain", credentials))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"status":"PRESENT"}
                                                """))
                                .andExpect(status().isOk());

        mvc.perform(get("/api/attendance/month")
                                        .param("month", month.toString())
                                        .with(httpBasic("terrain", credentials)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].workerId").value(workerId.intValue()))
                .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }

    @Test
    void eachRoleCanOnlyReadItsAssignedBusinessData() throws Exception {
        mvc.perform(get("/api/accounts")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/accounts")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/workers")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/attendance?date=2026-10-02")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/payments")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/expenses")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/stock/items")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/stock/items")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/expenses")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/payments")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/attendance?date=2026-10-02")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/payments")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/overtime")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/attendance?date=2026-10-02")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/overtime")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk());
    }

    @Test
    void fieldRequestsMaterialAssistantSendsAndFieldManagerConfirmsReceipt() throws Exception {
        String itemName = "Field material request " + System.nanoTime();
        mvc.perform(post("/api/stock/items")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","unit":"sacs"}
                                """.formatted(itemName)))
                .andExpect(status().isOk());
        var item = stockItems.findAll().stream()
                .filter(value -> value.getName().equals(itemName))
                .findFirst().orElseThrow();
        mvc.perform(put("/api/stock/items/{id}/price", item.getId())
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitPrice":2500}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/items/{id}/price", item.getId())
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitPrice":0}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/stock/items/{id}/price", item.getId())
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitPrice":2500}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(2500));

        mvc.perform(post("/api/stock/orders")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":5,"description":"Demande engrais"}
                                """.formatted(item.getId())))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":5,"description":"Demande engrais"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(5))
                .andExpect(jsonPath("$.status").value("ORDERED"));
        Long orderId = stockOrders.findTop100ByOrderByOrderedAtDesc().get(0).getId();

        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").exists());
        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").doesNotExist());

        mvc.perform(post("/api/stock/movements")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"kind":"RECEIPT","quantity":1,"description":"Réception directe"}
                                """.formatted(item.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/stock/movements")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"kind":"RECEIPT","quantity":1,"description":"Prix manquant"}
                                """.formatted(item.getId())))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/stock/orders/{id}/send", orderId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentQuantity":4}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/orders/{id}/send", orderId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentQuantity":6}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedQuantity").value(6))
                .andExpect(jsonPath("$.paidAmount").value(15000))
                .andExpect(jsonPath("$.unitPriceAtDispatch").value(2500))
                .andExpect(jsonPath("$.status").value("SENT"));
        mvc.perform(put("/api/stock/orders/{id}/send", orderId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentQuantity":4}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/stock/items/{id}/price", item.getId())
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitPrice":3000}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/stock/items/{id}/price-history", item.getId())
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].previousUnitPrice").value(2500))
                .andExpect(jsonPath("$[0].newUnitPrice").value(3000));
        org.junit.jupiter.api.Assertions.assertEquals(
                0, stockItems.findById(item.getId()).orElseThrow().getQuantity());
        mvc.perform(put("/api/stock/orders/{id}/receive", orderId)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/orders/{id}/receive", orderId)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.unitPriceAtDispatch").value(2500))
                .andExpect(jsonPath("$.paidAmount").value(15000));
        mvc.perform(put("/api/stock/orders/{id}/receive", orderId)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")].paidAmount").value(15000));

        org.junit.jupiter.api.Assertions.assertEquals(
                6, stockItems.findById(item.getId()).orElseThrow().getQuantity());
        long receiptCount = stockMovements.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(movement -> movement.getItem().getId().equals(item.getId()))
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, receiptCount);
        var recordedReceipt = stockMovements.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(movement -> movement.getItem().getId().equals(item.getId()))
                .findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(15_000L, recordedReceipt.getPaidAmount());

        mvc.perform(post("/api/stock/movements")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"kind":"USAGE","quantity":1,"description":"Consommation interdite"}
                                """.formatted(item.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/stock/movements")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"kind":"USAGE","quantity":2,"description":"Utilisé au champ"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/stock/movements")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'Utilisé au champ')]").exists());
    }

    @Test
    @Transactional
    void assistantMustValidateDailyReportBeforeDirectorCanReadIt() throws Exception {
        LocalDate reportDate = LocalDate.of(2200, 1, 2);
        String date = reportDate.toString();
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Worker worker = workers.save(new Worker(
                "Validated report " + System.nanoTime(), "Nord", 90_000, manager.getField()));
        mvc.perform(put("/api/attendance/{workerId}?date={date}", worker.getId(), date)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkIn":"%sT07:00:00Z","checkOut":"%sT15:00:00Z"}
                                """.formatted(date, date)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/reports/daily?date={date}", date)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(false))
                .andExpect(jsonPath("$.attendance[0].workerId").value(worker.getId()));
        mvc.perform(get("/api/reports/daily?date={date}", date)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(false))
                .andExpect(jsonPath("$.attendance").isEmpty())
                .andExpect(jsonPath("$.expenses").isEmpty());

        mvc.perform(put("/api/reports/daily/validate?date={date}", date)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/reports/daily/validate?date={date}", date)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/reports/daily/validate?date={date}", date)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true))
                .andExpect(jsonPath("$.validatedBy").value("assistant"));

        mvc.perform(get("/api/reports/daily?date={date}", date)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true))
                .andExpect(jsonPath("$.validatedBy").value("assistant"))
                .andExpect(jsonPath("$.attendance[0].workerId").value(worker.getId()));

        mvc.perform(put("/api/attendance/{workerId}?date={date}", worker.getId(), date)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ABSENT"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/daily?date={date}", date)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(false))
                .andExpect(jsonPath("$.attendance").isEmpty());
    }

    @Test
    @Transactional
    void payrollSettingsAreSharedAndWritableOnlyByDirector() throws Exception {
        mvc.perform(get("/api/payroll/settings")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySalary").value(90000))
                .andExpect(jsonPath("$.monthlyRequiredHours").value(208));

        mvc.perform(put("/api/payroll/settings")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlySalary":120000,"monthlyRequiredHours":160}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/payroll/settings")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlySalary":120000,"monthlyRequiredHours":160}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySalary").value(120000))
                .andExpect(jsonPath("$.monthlyRequiredHours").value(160));

        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Worker defaultSalaryWorker = workers.save(new Worker(
                "Default salary " + System.nanoTime(), "Nord", 90_000, manager.getField()));
        Worker individuallyPaidWorker = new Worker(
                "Individual salary " + System.nanoTime(), "Nord", 90_000, manager.getField());
        individuallyPaidWorker.updateSalary(135_000);
        individuallyPaidWorker = workers.save(individuallyPaidWorker);

        mvc.perform(put("/api/payroll/settings")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlySalary":125000,"monthlyRequiredHours":160}
                                """))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(
                workers.findById(defaultSalaryWorker.getId()).orElseThrow().getSalary()).isEqualTo(125_000);
        Worker unchangedIndividual = workers.findById(individuallyPaidWorker.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(unchangedIndividual.getSalary()).isEqualTo(135_000);
        org.assertj.core.api.Assertions.assertThat(unchangedIndividual.isSalaryIsIndividual()).isTrue();

        mvc.perform(get("/api/payroll/settings")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySalary").value(125000))
                .andExpect(jsonPath("$.monthlyRequiredHours").value(160));
    }

    @Test
    @Transactional
    void fieldManagerAndAssistantCanRecordHarvestSalesForTheDailyReport() throws Exception {
        LocalDate date = LocalDate.now();
        String dateText = date.toString();
        String product = "Fèves test " + System.nanoTime();
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        Long fieldId = manager.getField().getId();

        mvc.perform(post("/api/sales")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"product":"Fèves","quantity":12.5,"totalPrice":48000,"fieldId":%d}
                                """.formatted(fieldId)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/sales")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"product":"%s","quantity":12.5,"totalPrice":48000,"fieldId":%d}
                                """.formatted(product, fieldId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").value(product))
                .andExpect(jsonPath("$.quantity").value(12.5))
                .andExpect(jsonPath("$.totalPrice").value(48000))
                .andExpect(jsonPath("$.fieldId").value(fieldId));

        mvc.perform(get("/api/sales")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.product == '%s')]".formatted(product)).exists());

        mvc.perform(get("/api/reports/daily?date={date}", dateText)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales[0].product").value(product));
        mvc.perform(get("/api/reports/daily?date={date}", dateText)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales").isEmpty());

        mvc.perform(put("/api/reports/daily/validate?date={date}", dateText)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/daily?date={date}", dateText)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales[0].product").value(product));

        mvc.perform(post("/api/sales")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"product":"Fèves assistant","quantity":3,"totalPrice":15000,"fieldId":%d}
                                """.formatted(fieldId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").value("Fèves assistant"));
    }

    @Test
    void directorCanRecruitAndSuspendAssistantAccount() throws Exception {
        String username = "recruited-" + System.nanoTime();
        String accountRequest = """
                {"username":"%s","password":"Recrute2026!","role":"ASSISTANT","phoneNumber":"+2250102030405"}
                """.formatted(username);

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"abcd","role":"ASSISTANT","phoneNumber":"+2250102030405"}
                                """.formatted(username)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"1234567890123","role":"ASSISTANT","phoneNumber":"+2250102030405"}
                                """.formatted(username)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Recrute2026!","role":"ASSISTANT"}
                                """.formatted(username)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.phoneNumber").value("+2250102030405"))
                .andExpect(jsonPath("$.password").doesNotExist());
        Long accountId = appUsers.findByUsername(username).orElseThrow().getId();

        mvc.perform(get("/api/auth/me")
                        .with(httpBasic(username, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ASSISTANT"));

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/accounts/{id}/enabled", accountId)
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(get("/api/auth/me")
                        .with(httpBasic(username, "Recrute2026!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fieldsAreAssignedAndFieldManagersCannotReadOrWriteAcrossFields() throws Exception {
        PlantationField defaultField = defaultField();
        String fieldManager = "manager-" + System.nanoTime();
        mvc.perform(post("/api/fields")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"North field %s","location":"North district","description":"North block"}
                                """.formatted(fieldManager)))
                .andExpect(status().isOk());
        PlantationField secondField = fields.findAll().stream()
                .filter(field -> field.getName().equals("North field " + fieldManager))
                .findFirst().orElseThrow();
        mvc.perform(get("/api/budgets")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/budgets/field")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingLimit").value(0))
                .andExpect(jsonPath("$.fields", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.fields[0].fieldId").value(defaultField.getId().intValue()));

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030405","fieldId":%d}
                                """.formatted(fieldManager, secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()));

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"peer-%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030406","fieldId":%d}
                                """.formatted(fieldManager, secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()));

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing-field-%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030407"}
                                """.formatted(fieldManager)))
                .andExpect(status().isBadRequest());

        String defaultManager = "terrain";
        Long firstWorkerId = workers.save(new Worker(
                "Default field worker " + fieldManager, "North", 3400, defaultField)).getId();
        mvc.perform(post("/api/workers")
                        .with(httpBasic(fieldManager, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second field worker","groupName":"North","salary":3600,"phoneNumber":"+2250102030408"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()));
        Long secondWorkerId = workers.findAll().stream()
                .filter(worker -> worker.getName().equals("Second field worker"))
                .findFirst().orElseThrow().getId();

        mvc.perform(get("/api/workers").with(httpBasic(defaultManager, "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + secondWorkerId + ")]").doesNotExist());
        mvc.perform(get("/api/workers").with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + secondWorkerId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + firstWorkerId + ")]").doesNotExist());

        mvc.perform(put("/api/attendance/{workerId}?date=2026-10-02", secondWorkerId)
                        .with(httpBasic(defaultManager, "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/overtime")
                        .with(httpBasic(defaultManager, "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"hours":2,"description":"cross field"}
                                """.formatted(secondWorkerId)))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/stock/items")
                        .with(httpBasic(fieldManager, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Field-specific fertilizer","unit":"kg"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()));
        mvc.perform(put("/api/budgets/limit")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fundingLimit":1000}
                                """))
                .andExpect(status().isOk());
        mvc.perform(post("/api/budgets/funding")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":300,"note":"Additional harvest funds"}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/budgets/funding")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":300,"note":"Additional harvest funds"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingLimit").value(1300))
                .andExpect(jsonPath("$.unallocatedAmount").value(1300));
        mvc.perform(post("/api/budgets/allocations")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldId":%d,"amount":500}
                                """.formatted(secondField.getId())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/budgets/field")
                                .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingLimit").value(500))
                .andExpect(jsonPath("$.fields", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.fields[0].fieldName").value(secondField.getName()));
        mvc.perform(post("/api/expenses/field-budget")
                                .with(httpBasic(fieldManager, "Recrute2026!"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"description":"Transport des outils","amount":100,"idempotencyKey":"manager-expense-000001"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgeted").value(true))
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()));
        mvc.perform(get("/api/budgets/field")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedExpenses").value(100))
                .andExpect(jsonPath("$.remainingAmount").value(400));
        mvc.perform(post("/api/expenses/field-budget")
                        .with(httpBasic(fieldManager, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Transport des outils","amount":100,"idempotencyKey":"manager-expense-000001"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/budgets/field")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedExpenses").value(100))
                .andExpect(jsonPath("$.remainingAmount").value(400));
        mvc.perform(post("/api/expenses/field-budget")
                                .with(httpBasic(fieldManager, "Recrute2026!"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"amount":100,"idempotencyKey":"manager-expense-000002"}
                                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses")
                        .with(httpBasic(fieldManager, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Dépense hors budget","amount":100,"fieldId":%d}
                                """.formatted(secondField.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/expenses")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"North field supplies","amount":250,"fieldId":%d,"idempotencyKey":"field-expense-test-0001"}
                                """.formatted(secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgeted").value(true))
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()));
        mvc.perform(post("/api/expenses")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"North field supplies","amount":250,"fieldId":%d,"idempotencyKey":"field-expense-test-0001"}
                                """.formatted(secondField.getId())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/budgets")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedExpenses").value(350));
        mvc.perform(post("/api/expenses/offline-sync")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Offline field supplies","amount":600,"fieldId":%d,"idempotencyKey":"offline-field-expense-000001","occurredAt":"2026-10-16T12:00:00Z"}
                                """.formatted(secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationStatus").value("PENDING_RECONCILIATION"));
        mvc.perform(post("/api/expenses/offline-sync")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Offline field supplies","amount":600,"fieldId":%d,"idempotencyKey":"offline-field-expense-000001","occurredAt":"2026-10-16T12:00:00Z"}
                                """.formatted(secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationStatus").value("PENDING_RECONCILIATION"));
        mvc.perform(put("/api/budgets/limit")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fundingLimit":1600}
                                """))
                .andExpect(status().isOk());
        mvc.perform(post("/api/budgets/allocations")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldId":%d,"amount":500}
                                """.formatted(secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingExpenses").value(0))
                .andExpect(jsonPath("$.confirmedExpenses").value(950));
        mvc.perform(get("/api/expenses")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'North field supplies')]").exists());
        mvc.perform(post("/api/expenses")
                        .with(httpBasic(fieldManager, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Forged cross-field expense","amount":250,"fieldId":%d}
                                """.formatted(defaultField.getId())))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/stock/items")
                        .with(httpBasic(defaultManager, "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Field-specific fertilizer')]").doesNotExist());
        mvc.perform(get("/api/stock/items")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Field-specific fertilizer')]").exists());
        mvc.perform(get("/api/expenses")
                        .with(httpBasic(defaultManager, "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'North field supplies')]").doesNotExist());
        mvc.perform(get("/api/expenses")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'North field supplies')]").exists());
        mvc.perform(get("/api/expenses")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'North field supplies')]").exists());
        mvc.perform(get("/api/workers")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + firstWorkerId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + secondWorkerId + ")]").exists());
    }

    @Test
    void legacyManagersAndUnassignedRecordsAreMigratedToDefaultField() {
        PlantationField defaultField = defaultField();
        var manager = appUsers.findByUsername("terrain").orElseThrow();
        manager.assignField(null);
        appUsers.saveAndFlush(manager);
        Worker legacyWorker = workers.saveAndFlush(
                new Worker("Legacy worker " + System.nanoTime(), "Old group", 3000));

        fieldDataInitializer.run();

        org.junit.jupiter.api.Assertions.assertEquals(defaultField.getId(),
                appUsers.findByUsername("terrain").orElseThrow().getField().getId());
        org.junit.jupiter.api.Assertions.assertEquals(defaultField.getId(),
                workers.findById(legacyWorker.getId()).orElseThrow().getField().getId());
        org.junit.jupiter.api.Assertions.assertNotNull(fields.findById(defaultField.getId()).orElseThrow());
    }

    private PlantationField defaultField() {
        Long defaultFieldId = appUsers.findByUsername("terrain").orElseThrow().getField().getId();
        return fields.findById(defaultFieldId).orElseThrow();
    }
}
