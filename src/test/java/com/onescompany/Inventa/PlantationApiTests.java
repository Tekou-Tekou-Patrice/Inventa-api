package com.onescompany.Inventa;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.onescompany.Inventa.domain.FieldTaskRepository;
import com.onescompany.Inventa.domain.FieldTaskProgressRepository;
import com.onescompany.Inventa.domain.FieldTaskAdvanceRepository;
import com.onescompany.Inventa.domain.SeasonalWorkerRepository;
import com.onescompany.Inventa.domain.EvidenceAttachment;
import com.onescompany.Inventa.domain.EvidenceAttachmentRepository;
import com.onescompany.Inventa.domain.HarvestSaleRepository;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.domain.PlannedWorkRepository;
import com.onescompany.Inventa.domain.WorkerRepository;
import com.onescompany.Inventa.domain.Worker;
import com.onescompany.Inventa.domain.StockItemRepository;
import com.onescompany.Inventa.domain.StockMovementRepository;
import com.onescompany.Inventa.domain.StockOrderRepository;
import com.onescompany.Inventa.domain.FieldMaterialNeedRepository;
import com.onescompany.Inventa.domain.Payment;
import com.onescompany.Inventa.domain.PaymentRepository;
import com.onescompany.Inventa.domain.PayrollSettings;
import com.onescompany.Inventa.domain.PayrollSettingsRepository;
import com.onescompany.Inventa.domain.PayrollPaymentRequest;
import com.onescompany.Inventa.domain.PayrollPaymentRequestRepository;
import com.onescompany.Inventa.domain.ExpenseRepository;
import com.onescompany.Inventa.security.FieldDataInitializer;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.EvidenceStorageService;
import com.onescompany.Inventa.security.BackupService;
import com.onescompany.Inventa.security.PushDeviceTokenRepository;
import com.onescompany.Inventa.security.PushNotificationService;
import com.onescompany.Inventa.security.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
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
    private ExpenseRepository expenses;

    @Autowired
    private StockItemRepository stockItems;

    @Autowired
    private StockMovementRepository stockMovements;

    @Autowired
    private StockOrderRepository stockOrders;

    @Autowired
    private FieldMaterialNeedRepository fieldMaterialNeeds;

    @Autowired
    private FieldTaskRepository tasks;

    @Autowired
    private SeasonalWorkerRepository seasonalWorkers;

    @Autowired
    private FieldTaskProgressRepository taskProgress;

    @Autowired
    private FieldTaskAdvanceRepository taskAdvances;

    @Autowired
    private HarvestSaleRepository harvestSales;

    @Autowired
    private EvidenceAttachmentRepository evidenceAttachments;

    @Autowired
    private EvidenceStorageService evidenceStorage;

    @Autowired
    private BackupService backupService;

    @Autowired
    private PushDeviceTokenRepository pushDeviceTokens;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private AppUserRepository appUsers;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlantationFieldRepository fields;

    @Autowired
    private PlannedWorkRepository plannedWork;

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
    void pushDevicesCanOnlyBeRemovedByTheirRegisteredAccount() throws Exception {
        String token = "test-push-device-" + System.nanoTime();
        try {
            mvc.perform(post("/api/push/devices")
                            .with(httpBasic("assistant", "Assistant42!"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token":"%s","platform":"android"}
                                    """.formatted(token)))
                    .andExpect(status().isOk());
            org.assertj.core.api.Assertions.assertThat(pushDeviceTokens.findByToken(token))
                    .get()
                    .extracting("username")
                    .isEqualTo("assistant");

            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete("/api/push/devices")
                            .with(httpBasic("director", "Director42!!"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token":"%s"}
                                    """.formatted(token)))
                    .andExpect(status().isNotFound());

            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete("/api/push/devices")
                            .with(httpBasic("assistant", "Assistant42!"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token":"%s"}
                                    """.formatted(token)))
                    .andExpect(status().isOk());
        } finally {
            pushDeviceTokens.deleteByToken(token);
        }
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
                .andExpect(jsonPath("$.phoneNumber").value("+2250102030405"))
                .andExpect(jsonPath("$.salary").value(org.hamcrest.Matchers.nullValue()));

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
        org.junit.jupiter.api.Assertions.assertTrue(applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .anyMatch(event -> "field_task_created".equals(event.type())
                        && event.role() == UserRole.FIELD_MANAGER
                        && "terrain".equals(event.excludedUsername())));

        var createdTask = tasks.findAll().stream()
                .filter(task -> task.getTitle().equals("Task status test"))
                .findFirst().orElseThrow();
        Long taskId = createdTask.getId();

        mvc.perform(put("/api/tasks/{id}/status", taskId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE","actualCost":50000,"seasonalWorkerCount":9}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/tasks/{id}/status", taskId)
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.actualCost").doesNotExist())
                .andExpect(jsonPath("$.seasonalWorkerCount").value(0))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mvc.perform(post("/api/tasks")
                        .with(httpBasic("terrain", credentials))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Seasonal task test","type":"SEASONAL_WORKERS","assigneeIds":[],"plannedHectares":10,"pricePerHectare":10000}
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
                                {"status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonalWorkerCount").value(0))
                .andExpect(jsonPath("$.actualCost").doesNotExist());
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
    void assistantCanEditAndArchiveStockProductsWithoutLosingTheirHistory() throws Exception {
        String itemName = "Editable product " + System.nanoTime();
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

        mvc.perform(put("/api/stock/items/{id}", item.getId())
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated product","unit":"kg"}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/items/{id}", item.getId())
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated product","unit":"kg"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated product"))
                .andExpect(jsonPath("$.unit").value("kg"));

        item = stockItems.findById(item.getId()).orElseThrow();
        item.adjustQuantity(3);
        stockItems.save(item);
        mvc.perform(delete("/api/stock/items/{id}", item.getId())
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isConflict());
        item.adjustQuantity(-3);
        stockItems.save(item);

        mvc.perform(delete("/api/stock/items/{id}", item.getId())
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/stock/items/{id}", item.getId())
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertFalse(
                stockItems.findById(item.getId()).orElseThrow().isActive());
        mvc.perform(get("/api/stock/items")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Updated product')]")
                        .value(org.hamcrest.Matchers.empty()));
        mvc.perform(put("/api/stock/items/{id}/price", item.getId())
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitPrice":2500}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void fieldRequestsMaterialAssistantSendsAndFieldManagerConfirmsReceipt() throws Exception {
        String technicalManager = "technical-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Tech42!!","role":"TECHNICAL_MANAGER","phoneNumber":"+2250102030408"}
                                """.formatted(technicalManager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICAL_MANAGER"))
                .andExpect(jsonPath("$.payrollWorkerId").doesNotExist());
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
        mvc.perform(get("/api/stock/items")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + itemName + "')].unitPrice")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
        mvc.perform(get("/api/stock/items")
                        .with(httpBasic(technicalManager, "Tech42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + itemName + "')].unitPrice")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));

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
                .andExpect(jsonPath("$.status").value("PENDING_TECHNICAL_APPROVAL"))
                .andExpect(jsonPath("$.orderedBy").value("terrain"));
        Long orderId = stockOrders.findTop100ByOrderByOrderedAtDesc().get(0).getId();
        mvc.perform(put("/api/stock/orders/{id}", orderId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":6,"description":"Modification interdite"}
                                """.formatted(item.getId())))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")].paidAmount")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").doesNotExist());
        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic(technicalManager, "Tech42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")].unitPriceAtDispatch")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));

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
                .andExpect(status().isConflict());
        mvc.perform(put("/api/stock/orders/{id}/technical-decision", orderId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":true}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/orders/{id}/technical-decision", orderId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TECHNICALLY_APPROVED"))
                .andExpect(jsonPath("$.technicalDecisionBy").value(technicalManager));
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
                .andExpect(jsonPath("$.unitPriceAtDispatch").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.paidAmount").value(org.hamcrest.Matchers.nullValue()));
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

        mvc.perform(post("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":2,"description":"Commande à refuser"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_TECHNICAL_APPROVAL"));
        Long rejectedOrderId = stockOrders.findTop100ByOrderByOrderedAtDesc().get(0).getId();
        mvc.perform(put("/api/stock/orders/{id}/technical-decision", rejectedOrderId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":false,"reason":"Produit non adapté"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TECHNICALLY_REJECTED"))
                .andExpect(jsonPath("$.technicalRejectionReason").value("Produit non adapté"));
        mvc.perform(put("/api/stock/orders/{id}/send", rejectedOrderId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentQuantity":2}
                                """))
                .andExpect(status().isConflict());

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
        var usageNotifications = applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .filter(event -> "stock_usage".equals(event.type()))
                .toList();
        org.junit.jupiter.api.Assertions.assertTrue(usageNotifications.stream()
                .anyMatch(event -> event.role() == UserRole.ASSISTANT));
        org.junit.jupiter.api.Assertions.assertTrue(usageNotifications.stream()
                .anyMatch(event -> event.role() == UserRole.DIRECTOR));
        mvc.perform(get("/api/stock/movements")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'Utilisé au champ')]").exists());
    }

    @Test
    @Transactional
    void fieldRequestsShowTheirFieldAndKeepEditsCancellationAndRevisionsTraceable() throws Exception {
        String technicalManager = "request-tech-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Tech42!!","role":"TECHNICAL_MANAGER","phoneNumber":"+2250102030490"}
                                """.formatted(technicalManager)))
                .andExpect(status().isOk());

        String itemName = "Request revision " + System.nanoTime();
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

        mvc.perform(post("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":2,"description":"Demande initiale"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(item.getField().getId()))
                .andExpect(jsonPath("$.fieldName").value(item.getField().getName()));
        var creationNotifications = applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .filter(event -> "stock_order_created".equals(event.type()))
                .toList();
        org.junit.jupiter.api.Assertions.assertTrue(creationNotifications.stream()
                .anyMatch(event -> event.role() == UserRole.DIRECTOR));
        Long orderId = stockOrders.findTop100ByOrderByOrderedAtDesc().stream()
                .filter(order -> order.getDescription().equals("Demande initiale"))
                .findFirst().orElseThrow().getId();

        mvc.perform(put("/api/stock/orders/{id}", orderId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":3,"description":"Quantité modifiée par le technique"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value(technicalManager))
                .andExpect(jsonPath("$.fieldName").value(item.getField().getName()));
        mvc.perform(put("/api/stock/orders/{id}", orderId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":4,"description":"Quantité confirmée par le terrain"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value("terrain"));

        mvc.perform(post("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":1,"description":"Demande retirée"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk());
        Long cancelledId = stockOrders.findTop100ByOrderByOrderedAtDesc().stream()
                .filter(order -> order.getDescription().equals("Demande retirée"))
                .findFirst().orElseThrow().getId();
        mvc.perform(delete("/api/stock/orders/{id}", cancelledId)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(post("/api/stock/orders")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":1,"description":"Demande à corriger"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk());
        Long rejectedId = stockOrders.findTop100ByOrderByOrderedAtDesc().stream()
                .filter(order -> order.getDescription().equals("Demande à corriger"))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/stock/orders/{id}/technical-decision", rejectedId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":false,"reason":"Quantité à revoir"}
                                """))
                .andExpect(status().isOk());
        var decisionNotifications = applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .filter(event -> "stock_request_decision".equals(event.type()))
                .toList();
        org.junit.jupiter.api.Assertions.assertTrue(decisionNotifications.stream()
                .anyMatch(event -> event.usernames() != null
                        && event.usernames().contains("terrain")));
        org.junit.jupiter.api.Assertions.assertTrue(applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .anyMatch(event -> event.role() == UserRole.DIRECTOR
                        && "stock_technical_decision".equals(event.type())));
        mvc.perform(put("/api/stock/orders/{id}", rejectedId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":2,"description":"Correction interdite en place"}
                                """.formatted(item.getId())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/stock/orders/{id}/revisions", rejectedId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":2,"description":"Demande corrigée"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisedFromId").value(rejectedId))
                .andExpect(jsonPath("$.status").value("PENDING_TECHNICAL_APPROVAL"));
        Long revisionId = stockOrders.findTop100ByOrderByOrderedAtDesc().stream()
                .filter(order -> order.getDescription().equals("Demande corrigée"))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/stock/orders/{id}/technical-decision", revisionId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/stock/orders/{id}/revisions", revisionId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":3,"description":"Révision technique"}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisedFromId").value(revisionId))
                .andExpect(jsonPath("$.status").value("PENDING_TECHNICAL_APPROVAL"));
        mvc.perform(get("/api/stock/orders")
                        .with(httpBasic(technicalManager, "Tech42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + rejectedId + ")].status")
                        .value(org.hamcrest.Matchers.hasItem("TECHNICALLY_REJECTED")))
                .andExpect(jsonPath("$[?(@.id == " + cancelledId + ")].status")
                        .value(org.hamcrest.Matchers.hasItem("CANCELLED")));
    }

    @Test
    @Transactional
    void seasonalTaskProgressAndIndividualAdvancesAreFieldScopedAndCapped() throws Exception {
        String technicalManager = "seasonal-tech-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Tech42!!","role":"TECHNICAL_MANAGER","phoneNumber":"+2250102030590"}
                                """.formatted(technicalManager)))
                .andExpect(status().isOk());

        Long fieldId = defaultField().getId();
        Long workerId = workers.save(new Worker(
                "Saisonnier ouvrier " + System.nanoTime(), "Nord", 3500, defaultField())).getId();
        String profileName = "Saisonnier " + System.nanoTime();
        mvc.perform(post("/api/seasonal-workers")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","phoneNumber":"+2250102030591"}
                                """.formatted(profileName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(fieldId));
        Long profileId = seasonalWorkers.findAll().stream()
                .filter(profile -> profile.getName().equals(profileName))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/seasonal-workers/{id}", profileId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Saisonnier modifié","phoneNumber":"+2250102030592"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Saisonnier modifié"))
                .andExpect(jsonPath("$.phoneNumber").value("+2250102030592"));

        mvc.perform(post("/api/tasks")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Récolte cacao","description":"Récolter la parcelle","priority":"URGENT","type":"SEASONAL_WORKERS","fieldId":%d,"assigneeIds":[%d],"seasonalWorkerIds":[%d],"plannedHectares":10.25,"pricePerHectare":10000}
                                """.formatted(fieldId, workerId, profileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.description").value("Récolter la parcelle"))
                .andExpect(jsonPath("$.plannedHectares").value(10.25))
                .andExpect(jsonPath("$.remainingWorkCost").value(102500));
        Long taskId = tasks.findAll().stream()
                .filter(task -> task.getTitle().equals("Récolte cacao"))
                .findFirst().orElseThrow().getId();

        mvc.perform(post("/api/tasks/{id}/progress", taskId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completedHectares":3.25,"idempotencyKey":"progress-one"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earnedAmount").value(32500));
        var progressNotifications = applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .filter(event -> "seasonal_task_progress".equals(event.type()))
                .toList();
        org.junit.jupiter.api.Assertions.assertTrue(progressNotifications.stream()
                .anyMatch(event -> event.role() == UserRole.ASSISTANT));
        org.junit.jupiter.api.Assertions.assertTrue(progressNotifications.stream()
                .anyMatch(event -> event.role() == UserRole.TECHNICAL_MANAGER));
        Long progressId = taskProgress.findByTaskIdOrderByReportedAtAscIdAsc(taskId)
                .get(0).getId();
        mvc.perform(post("/api/tasks/{id}/progress", taskId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completedHectares":3.25,"idempotencyKey":"progress-one"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(progressId));

        String advanceRequest = """
                {"workerId":%d,"amount":10000,"idempotencyKey":"advance-one"}
                """.formatted(workerId);
        mvc.perform(post("/api/tasks/{id}/advances", taskId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(advanceRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value(workerId))
                .andExpect(jsonPath("$.amount").value(10000));
        mvc.perform(post("/api/tasks/{id}/advances", taskId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(advanceRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(10000));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                taskAdvances.findByTaskIdOrderByPaidAtAscIdAsc(taskId).size());
        mvc.perform(post("/api/tasks/{id}/advances", taskId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seasonalWorkerId":%d,"amount":22501,"idempotencyKey":"advance-too-large"}
                                """.formatted(profileId)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/tasks/{id}/advances", taskId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seasonalWorkerId":%d,"amount":15000,"idempotencyKey":"seasonal-advance-one"}
                                """.formatted(profileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonalWorkerId").value(profileId))
                .andExpect(jsonPath("$.seasonalWorkerName").value("Saisonnier modifié"));
        mvc.perform(post("/api/tasks/{id}/progress", taskId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completedHectares":0.99,"idempotencyKey":"progress-too-low"}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/seasonal-workers/{id}", profileId)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/seasonal-workers")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + profileId + ")]").isEmpty());
        org.junit.jupiter.api.Assertions.assertFalse(
                seasonalWorkers.findById(profileId).orElseThrow().isActive());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                taskAdvances.findByTaskIdOrderByPaidAtAscIdAsc(taskId).stream()
                        .filter(advance -> advance.getSeasonalWorker() != null
                                && advance.getSeasonalWorker().getId().equals(profileId))
                        .count());
        mvc.perform(post("/api/tasks")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tâche interdite","type":"ASSIGNED_WORKERS","fieldId":%d}
                                """.formatted(fieldId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void technicalManagerPublishesFieldMaterialNeedsForTheAssistant() throws Exception {
        String technicalManager = "materials-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Tech42!!","role":"TECHNICAL_MANAGER","phoneNumber":"+2250102030498"}
                                """.formatted(technicalManager)))
                .andExpect(status().isOk());

        Long fieldId = defaultField().getId();
        PlantationField otherField = fields.save(new PlantationField(
                "Material field " + System.nanoTime(), "Abidjan", "Material list test"));
        String request = """
                {"fieldId":%d,"materialName":"Engrais","unit":"sacs","quantity":12,"notes":"Pour la prochaine saison"}
                """;

        mvc.perform(post("/api/stock/material-needs")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.formatted(fieldId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldName").value(defaultField().getName()))
                .andExpect(jsonPath("$.materialName").value("Engrais"))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.createdBy").value(technicalManager))
                .andExpect(jsonPath("$.purchased").value(false));

        Long needId = fieldMaterialNeeds.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(need -> need.getCreatedBy().equals(technicalManager))
                .filter(need -> need.getMaterialName().equals("Engrais"))
                .findFirst()
                .orElseThrow()
                .getId();
        mvc.perform(put("/api/stock/material-needs/{id}/purchased", needId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchased\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(true));

        mvc.perform(post("/api/stock/material-needs")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.formatted(otherField.getId())))
                .andExpect(status().isOk());

        mvc.perform(get("/api/stock/material-needs?fieldId={id}", fieldId)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].materialName").value("Engrais"))
                .andExpect(jsonPath("$[0].purchased").value(true));

        mvc.perform(get("/api/stock/material-needs")
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].fieldId").value(fieldId));

        mvc.perform(get("/api/stock/material-needs?fieldId={id}", otherField.getId())
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        mvc.perform(post("/api/stock/material-needs")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.formatted(fieldId)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/stock/material-needs")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.formatted(fieldId)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/stock/material-needs/{id}/purchased", needId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchased\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void technicalManagerCanPlanWorkAndFieldManagerOnlySeesAssignedPlans() throws Exception {
        String technicalManager = "planner-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Tech42!!","role":"TECHNICAL_MANAGER","phoneNumber":"+2250102030499"}
                                """.formatted(technicalManager)))
                .andExpect(status().isOk());

        Long fieldId = defaultField().getId();
        Long fieldManagerId = appUsers.findByUsername("terrain").orElseThrow().getId();
        mvc.perform(get("/api/work-plans/fields")
                        .with(httpBasic(technicalManager, "Tech42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fieldId))
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].managers").doesNotExist());
        mvc.perform(get("/api/work-plans/fields")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isForbidden());

        PlantationField otherField = fields.save(new PlantationField(
                "Planning field " + System.nanoTime(), "Abidjan", "Calendar test"));
        String otherManagerUsername = "terrain-plan-" + System.nanoTime();
        var otherManager = appUsers.save(new com.onescompany.Inventa.security.AppUser(
                otherManagerUsername,
                passwordEncoder.encode("Field42!!"),
                com.onescompany.Inventa.security.UserRole.FIELD_MANAGER,
                otherField));
        LocalDate workDate = LocalDate.now().plusDays(12);
        String planJson = """
                {
                  "title":"Préparer la parcelle",
                  "fieldId":%d,
                  "fieldManagerAccountId":%d,
                  "startDate":"%s",
                  "endDate":"%s",
                  "startTime":"08:00:00",
                  "endTime":"12:00:00"
                }
                """.formatted(fieldId, fieldManagerId, workDate, workDate);

        mvc.perform(post("/api/work-plans")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/work-plans")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/work-plans")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson.replace(
                                "\"endDate\":\"" + workDate + "\"",
                                "\"endDate\":\"" + workDate.minusDays(1) + "\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/work-plans")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Sans heure",
                                  "fieldId":%d,
                                  "fieldManagerAccountId":%d,
                                  "startDate":"%s",
                                  "endDate":"%s"
                                }
                                """.formatted(fieldId, fieldManagerId, workDate, workDate)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/work-plans")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson.replace("\"fieldId\":" + fieldId,
                                "\"fieldId\":" + otherField.getId())))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/work-plans")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Préparer la parcelle"))
                .andExpect(jsonPath("$.fieldManagerUsername").value("terrain"))
                .andExpect(jsonPath("$.createdBy").value(technicalManager));
        org.junit.jupiter.api.Assertions.assertTrue(applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .anyMatch(event -> "work_plan_assigned".equals(event.type())
                        && event.usernames() != null
                        && event.usernames().contains("terrain")));

        Long planId = plannedWork.findAll().stream()
                .filter(plan -> plan.getTitle().equals("Préparer la parcelle"))
                .findFirst().orElseThrow().getId();
        String planPath = "/api/work-plans?from=%s&to=%s".formatted(workDate, workDate);
        mvc.perform(get(planPath).with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId));
        mvc.perform(get(planPath).with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Préparer la parcelle"));
        mvc.perform(get("/api/work-plans?from=%s&to=%s".formatted(
                        workDate.plusYears(1), workDate.plusYears(1)))
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        String otherPlan = planJson.replace("Préparer la parcelle", "Plan autre équipe")
                .replace("\"fieldId\":" + fieldId, "\"fieldId\":" + otherField.getId())
                .replace("\"fieldManagerAccountId\":" + fieldManagerId,
                        "\"fieldManagerAccountId\":" + otherManager.getId());
        mvc.perform(post("/api/work-plans")
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherPlan))
                .andExpect(status().isOk());
        Long otherPlanId = plannedWork.findAll().stream()
                .filter(plan -> plan.getTitle().equals("Plan autre équipe"))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/work-plans/{id}/complete", otherPlanId)
                        .with(httpBasic(otherManagerUsername, "Field42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedBy").value(otherManagerUsername))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        mvc.perform(put("/api/work-plans/{id}/complete", otherPlanId)
                        .with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertTrue(applicationEvents
                .stream(PushNotificationService.PushNotificationEvent.class)
                .anyMatch(event -> event.role() == UserRole.TECHNICAL_MANAGER
                        && "work_plan_completed".equals(event.type())));
        mvc.perform(get(planPath).with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(planPath).with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(planPath).with(httpBasic(otherManagerUsername, "Field42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Plan autre équipe"));

        mvc.perform(put("/api/work-plans/{id}", planId)
                        .with(httpBasic(technicalManager, "Tech42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson.replace("Préparer la parcelle", "Entretenir la parcelle")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Entretenir la parcelle"))
                .andExpect(jsonPath("$.updatedBy").value(technicalManager));

        mvc.perform(delete("/api/work-plans/{id}", planId)
                        .with(httpBasic(technicalManager, "Tech42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBy").value(technicalManager))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
        mvc.perform(get(planPath).with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cancelledBy").value(technicalManager));
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
    void directorCanOnlySeeTasksAfterAssistantValidatesTheirDailyReport() throws Exception {
        mvc.perform(post("/api/tasks")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Task awaiting assistant validation","type":"SEASONAL_WORKERS","assigneeIds":[],"plannedHectares":10,"pricePerHectare":10000}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/tasks").with(httpBasic("terrain", "FieldMgr42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").exists());
        mvc.perform(get("/api/tasks").with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").exists());
        mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").doesNotExist());

        String today = LocalDate.now().toString();
        mvc.perform(put("/api/reports/daily/validate?date={date}", today)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").exists());

        Long taskId = tasks.findAll().stream()
                .filter(task -> task.getTitle().equals("Task awaiting assistant validation"))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/tasks/{id}/status", taskId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')].status")
                        .value(org.hamcrest.Matchers.contains("TO_DO")));

        mvc.perform(put("/api/reports/daily/validate?date={date}", today)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").value(
                        org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')].status")
                        .value(org.hamcrest.Matchers.contains("TO_DO", "IN_PROGRESS")));

        mvc.perform(post("/api/tasks")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New task not yet validated","type":"SEASONAL_WORKERS","assigneeIds":[],"plannedHectares":10,"pricePerHectare":10000}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").value(
                        org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[?(@.title == 'New task not yet validated')]").doesNotExist());

        mvc.perform(put("/api/reports/daily/validate?date={date}", today)
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Task awaiting assistant validation')]").value(
                        org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$[?(@.title == 'New task not yet validated')]").value(
                        org.hamcrest.Matchers.hasSize(1)));
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
    @Transactional
    void saleEvidenceIsStoredAndOnlyServedToAuthorizedFieldUsers() throws Exception {
        Long fieldId = appUsers.findByUsername("terrain").orElseThrow().getField().getId();
        String product = "Récolte avec preuve " + System.nanoTime();
        mvc.perform(post("/api/sales")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"product":"%s","quantity":2,"totalPrice":1500,"fieldId":%d}
                                """.formatted(product, fieldId)))
                .andExpect(status().isOk());
        Long saleId = harvestSales.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(sale -> sale.getProduct().equals(product))
                .findFirst()
                .orElseThrow()
                .getId();
        byte[] image = {(byte) 0xff, (byte) 0xd8, 1, 2, 3};

        try {
            mvc.perform(multipart("/api/sales/{id}/evidence", saleId)
                            .file(new MockMultipartFile("files", "recu.jpg", "image/jpeg", image))
                            .with(httpBasic("terrain", "FieldMgr42!!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].fileName").value("recu.jpg"))
                    .andExpect(jsonPath("$[0].contentType").value("image/jpeg"));

            EvidenceAttachment attachment = evidenceAttachments
                    .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                            EvidenceAttachment.EntityType.SALE, saleId)
                    .get(0);

            mvc.perform(get("/api/sales").with(httpBasic("assistant", "Assistant42!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].evidence[0].id").value(attachment.getId()));

            mvc.perform(get("/api/evidence/{id}/content", attachment.getId())
                            .with(httpBasic("assistant", "Assistant42!")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(content().bytes(image));

            mvc.perform(get("/api/evidence/{id}/content", attachment.getId())
                            .header("Range", "bytes=1-2")
                            .with(httpBasic("assistant", "Assistant42!")))
                    .andExpect(status().isPartialContent())
                    .andExpect(content().bytes(new byte[] {(byte) 0xd8, 1}));

            mvc.perform(get("/api/evidence/{id}/content", attachment.getId())
                            .with(httpBasic("terrain", "FieldMgr42!!")))
                    .andExpect(status().isOk());

            mvc.perform(multipart("/api/sales/{id}/evidence", saleId)
                            .file(new MockMultipartFile("files", "bad.txt", "text/plain", new byte[] {1}))
                            .with(httpBasic("terrain", "FieldMgr42!!")))
                    .andExpect(status().isUnsupportedMediaType());

            evidenceStorage.delete(attachment.getStorageKey());
        } finally {
            evidenceAttachments
                    .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                            EvidenceAttachment.EntityType.SALE, saleId)
                    .forEach(attachment -> {
                        try {
                            evidenceStorage.delete(attachment.getStorageKey());
                        } catch (java.io.IOException error) {
                            throw new RuntimeException(error);
                        }
                    });
        }
    }

    @Test
    @Transactional
    void assistantCanAttachInvoiceEvidenceToExpenseAndDirectorCanViewIt() throws Exception {
        Long fieldId = defaultField().getId();
        mvc.perform(post("/api/budgets/funding")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":3000,"note":"Invoice evidence test"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(post("/api/budgets/allocations")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldId":%d,"amount":3000}
                                """.formatted(fieldId)))
                .andExpect(status().isOk());
        String description = "Facture test " + System.nanoTime();
        mvc.perform(post("/api/expenses")
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"%s","amount":2800,"fieldId":%d,"idempotencyKey":"invoice-%d"}
                                """.formatted(description, fieldId, System.nanoTime())))
                .andExpect(status().isOk());
        Long expenseId = expenses.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(expense -> expense.getDescription().equals(description))
                .findFirst()
                .orElseThrow()
                .getId();
        byte[] image = {(byte) 0xff, (byte) 0xd8, 4, 5, 6};

        try {
            mvc.perform(multipart("/api/expenses/{id}/evidence", expenseId)
                            .file(new MockMultipartFile("files", "facture.jpg", "image/jpeg", image))
                            .with(httpBasic("assistant", "Assistant42!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].fileName").value("facture.jpg"));

            EvidenceAttachment attachment = evidenceAttachments
                    .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                            EvidenceAttachment.EntityType.EXPENSE, expenseId)
                    .get(0);
            mvc.perform(get("/api/expenses").with(httpBasic("director", "Director42!!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].evidence[0].id").value(attachment.getId()));
            mvc.perform(get("/api/evidence/{id}/content", attachment.getId())
                            .with(httpBasic("director", "Director42!!")))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(image));
            mvc.perform(multipart("/api/expenses/{id}/evidence", expenseId)
                            .file(new MockMultipartFile("files", "facture.jpg", "image/jpeg", image))
                            .with(httpBasic("director", "Director42!!")))
                    .andExpect(status().isForbidden());
        } finally {
            evidenceAttachments
                    .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                            EvidenceAttachment.EntityType.EXPENSE, expenseId)
                    .forEach(attachment -> {
                        try {
                            evidenceStorage.delete(attachment.getStorageKey());
                        } catch (java.io.IOException error) {
                            throw new RuntimeException(error);
                        }
                    });
        }
    }

    @Test
    @Transactional
    void taskEvidenceIsIncludedInTheDirectorsValidatedTaskSnapshot() throws Exception {
        String title = "Tâche avec preuve " + System.nanoTime();
        mvc.perform(post("/api/tasks")
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","type":"SEASONAL_WORKERS","assigneeIds":[],"plannedHectares":10,"pricePerHectare":10000}
                                """.formatted(title)))
                .andExpect(status().isOk());
        Long taskId = tasks.findAll().stream()
                .filter(task -> task.getTitle().equals(title))
                .findFirst()
                .orElseThrow()
                .getId();
        mvc.perform(put("/api/tasks/{id}/status", taskId)
                        .with(httpBasic("terrain", "FieldMgr42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualCost").doesNotExist())
                .andExpect(jsonPath("$.seasonalWorkerCount").value(0));

        byte[] image = {(byte) 0xff, (byte) 0xd8, 4, 5, 6};
        try {
            mvc.perform(multipart("/api/tasks/{id}/evidence", taskId)
                            .file(new MockMultipartFile("files", "travail.jpg", "image/jpeg", image))
                            .with(httpBasic("terrain", "FieldMgr42!!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].contentType").value("image/jpeg"));

            mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title == '%s')]".formatted(title)).doesNotExist());

            String today = LocalDate.now().toString();
            mvc.perform(put("/api/reports/daily/validate?date={date}", today)
                            .with(httpBasic("assistant", "Assistant42!")))
                    .andExpect(status().isOk());

            mvc.perform(get("/api/tasks").with(httpBasic("director", "Director42!!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title == '%s')].evidence[0].contentType"
                            .formatted(title)).value(org.hamcrest.Matchers.contains("image/jpeg")));
        } finally {
            evidenceAttachments
                    .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                            EvidenceAttachment.EntityType.TASK, taskId)
                    .forEach(attachment -> {
                        try {
                            evidenceStorage.delete(attachment.getStorageKey());
                        } catch (java.io.IOException error) {
                            throw new RuntimeException(error);
                        }
                    });
        }
    }

    @Test
    void backupCanBeDownloadedAndRestoresDatabaseRows() throws Exception {
        mvc.perform(get("/api/backups")
                        .with(httpBasic("assistant", "Assistant42!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/backups")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"));

        String marker = "Backup round trip " + System.nanoTime();
        Worker worker = workers.saveAndFlush(new Worker(
                marker, "Backup", 12345, appUsers.findByUsername("terrain").orElseThrow().getField()));
        byte[] evidenceBytes = new byte[] {1, 2, 3, 4};
        var storedEvidence = evidenceStorage.store(new MockMultipartFile(
                "files", "backup-proof.jpg", "image/jpeg", evidenceBytes));
        EvidenceAttachment evidence = evidenceAttachments.saveAndFlush(new EvidenceAttachment(
                EvidenceAttachment.EntityType.SALE,
                987654L,
                null,
                "backup-proof.jpg",
                "image/jpeg",
                storedEvidence.storageKey(),
                "director"));
        java.nio.file.Path file = backupService.createBackup();
        byte[] backup = java.nio.file.Files.readAllBytes(file);
        java.nio.file.Files.deleteIfExists(file);
        boolean foundReadableRows = false;
        boolean foundRestoreData = false;
        java.util.List<String> backupEntries = new java.util.ArrayList<>();
        try (var zip = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(backup))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                backupEntries.add(entry.getName());
                if (entry.getName().equalsIgnoreCase("donnees-csv/workers.csv")) {
                    String csv = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    foundReadableRows = csv.contains(marker);
                }
                if (entry.getName().equalsIgnoreCase("tables/workers.bin")) {
                    foundRestoreData = zip.readAllBytes().length > 0;
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(foundReadableRows)
                .as("Readable worker data missing from backup entries %s", backupEntries)
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(foundRestoreData)
                .as("Binary worker data missing from backup entries %s", backupEntries)
                .isTrue();

        workers.delete(worker);
        workers.flush();
        evidenceAttachments.delete(evidence);
        evidenceStorage.delete(storedEvidence.storageKey());
        org.assertj.core.api.Assertions.assertThat(workers.findAll().stream()
                .noneMatch(value -> value.getName().equals(marker))).isTrue();

        backupService.restore(new MockMultipartFile("backup", "inventa.zip", "application/zip", backup));

        org.assertj.core.api.Assertions.assertThat(workers.findAll().stream()
                .anyMatch(value -> value.getName().equals(marker))).isTrue();
        org.assertj.core.api.Assertions.assertThat(evidenceAttachments
                .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                        EvidenceAttachment.EntityType.SALE, 987654L))
                .hasSize(1);
        org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.readAllBytes(
                evidenceStorage.resolve(storedEvidence.storageKey())))
                .containsExactly(evidenceBytes);
        evidenceAttachments
                .findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                        EvidenceAttachment.EntityType.SALE, 987654L)
                .forEach(evidenceAttachments::delete);
        workers.delete(workers.findAll().stream()
                .filter(value -> value.getName().equals(marker))
                .findFirst()
                .orElseThrow());
        workers.flush();
        evidenceStorage.delete(storedEvidence.storageKey());
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
    void directorCanUpdateAndDeleteManagedAccountsWithoutDeletingPayrollHistory() throws Exception {
        String username = "editable-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Recrute2026!","role":"ASSISTANT","phoneNumber":"+2250102030405"}
                                """.formatted(username)))
                .andExpect(status().isOk());
        Long accountId = appUsers.findByUsername(username).orElseThrow().getId();
        String updatedUsername = username + "-updated";

        mvc.perform(put("/api/accounts/{id}", accountId)
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","phoneNumber":"+2250708090102","password":"Nouveau2026!"}
                                """.formatted(updatedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(updatedUsername))
                .andExpect(jsonPath("$.phoneNumber").value("+2250708090102"));
        mvc.perform(get("/api/auth/me")
                        .with(httpBasic(updatedUsername, "Nouveau2026!")))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/accounts/{id}", accountId)
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/accounts")
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == '" + updatedUsername + "')]").isEmpty());
        mvc.perform(get("/api/auth/me")
                        .with(httpBasic(updatedUsername, "Nouveau2026!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fieldManagerCanPointAcrossFieldsAndResumeAfterAutomaticFiveHourCheckout() throws Exception {
        PlantationField otherField = fields.saveAndFlush(new PlantationField(
                "Attendance field " + System.nanoTime(), "Other district", ""));
        String username = "attendance-manager-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030405","fieldId":%d,"monthlySalary":120000,"monthlyRequiredHours":160}
                                """.formatted(username, defaultField().getId())))
                .andExpect(status().isOk());
        Long accountId = appUsers.findByUsername(username).orElseThrow().getId();
        String updatedUsername = username + "-updated";
        mvc.perform(put("/api/accounts/{id}", accountId)
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","phoneNumber":"+2250708090102","fieldId":%d,"monthlySalary":150000,"monthlyRequiredHours":180}
                                """.formatted(updatedUsername, defaultField().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySalary").value(150000))
                .andExpect(jsonPath("$.monthlyRequiredHours").value(180));
        username = updatedUsername;
        Worker worker = workers.saveAndFlush(
                new Worker("Other-field worker " + username, "Team", 4000, otherField));
        String date = LocalDate.now().toString();
        Instant staleArrival = Instant.now().minusSeconds(6 * 60 * 60).truncatedTo(
                java.time.temporal.ChronoUnit.SECONDS);

        mvc.perform(get("/api/attendance/team")
                        .param("date", date)
                        .with(httpBasic(username, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[?(@.id == " + otherField.getId() + ")]").exists())
                .andExpect(jsonPath("$.workers[?(@.id == " + worker.getId() + ")]").exists());

        mvc.perform(put("/api/attendance/{workerId}?date={date}&fieldId={fieldId}",
                                worker.getId(), date, otherField.getId())
                        .with(httpBasic(username, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkIn":"%s"}
                                """.formatted(staleArrival)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions.length()").value(1))
                .andExpect(jsonPath("$.sessions[0].checkOut").value(
                        staleArrival.plusSeconds(5 * 60 * 60).toString()));

        Instant nextArrival = Instant.now().minusSeconds(60).truncatedTo(
                java.time.temporal.ChronoUnit.SECONDS);
        mvc.perform(put("/api/attendance/{workerId}?date={date}&fieldId={fieldId}",
                                worker.getId(), date, otherField.getId())
                        .with(httpBasic(username, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkIn":"%s"}
                                """.formatted(nextArrival)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions.length()").value(2))
                .andExpect(jsonPath("$.checkOut").doesNotExist());

        mvc.perform(put("/api/attendance/{workerId}?date={date}&fieldId={fieldId}",
                                worker.getId(), date, otherField.getId())
                        .with(httpBasic(username, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PRESENT","checkOut":"%s"}
                                """.formatted(Instant.now().truncatedTo(
                                        java.time.temporal.ChronoUnit.SECONDS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions.length()").value(2))
                .andExpect(jsonPath("$.sessions[1].checkOut").isNotEmpty());
    }

    @Test
    void deletingAFieldArchivesItAndSuspendsItsManager() throws Exception {
        PlantationField field = fields.saveAndFlush(new PlantationField(
                "Archived field " + System.nanoTime(), "District", ""));
        String username = "archived-manager-" + System.nanoTime();
        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030405","fieldId":%d,"monthlySalary":120000,"monthlyRequiredHours":160}
                                """.formatted(username, field.getId())))
                .andExpect(status().isOk());
        Long workerId = workers.findAll().stream()
                .filter(worker -> worker.getName().equals(username))
                .findFirst().orElseThrow().getId();

        mvc.perform(delete("/api/fields/{id}", field.getId())
                        .with(httpBasic("director", "Director42!!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        org.junit.jupiter.api.Assertions.assertFalse(
                appUsers.findByUsername(username).orElseThrow().isEnabled());
        org.junit.jupiter.api.Assertions.assertFalse(
                workers.findById(workerId).orElseThrow().isActive());
        org.junit.jupiter.api.Assertions.assertTrue(fields.findById(field.getId()).isPresent());
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
                                {"username":"%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030405","fieldId":%d,"monthlySalary":120000,"monthlyRequiredHours":160}
                                """.formatted(fieldManager, secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()))
                .andExpect(jsonPath("$.monthlySalary").value(120000))
                .andExpect(jsonPath("$.monthlyRequiredHours").value(160));
        Long managerWorkerId = workers.findAll().stream()
                .filter(worker -> worker.getName().equals(fieldManager))
                .findFirst()
                .orElseThrow()
                .getId();
        mvc.perform(post("/api/attendance/self/check-in")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value(managerWorkerId))
                .andExpect(jsonPath("$.checkIn").isNotEmpty());
        mvc.perform(post("/api/attendance/self/check-in")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/attendance/self/check-out")
                        .with(httpBasic(fieldManager, "Recrute2026!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkOut").isNotEmpty());
        mvc.perform(put("/api/workers/{id}/salary", managerWorkerId)
                        .with(httpBasic("assistant", "Assistant42!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salary":150000}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/overtime")
                        .with(httpBasic(fieldManager, "Recrute2026!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"hours":2.5,"description":"Travail après l’horaire"}
                                """.formatted(managerWorkerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value(managerWorkerId));

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"peer-%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030406","fieldId":%d,"monthlySalary":90000,"monthlyRequiredHours":208}
                                """.formatted(fieldManager, secondField.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(secondField.getId().intValue()))
                .andExpect(jsonPath("$.fieldName").value(secondField.getName()));

        mvc.perform(post("/api/accounts")
                        .with(httpBasic("director", "Director42!!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing-field-%s","password":"Recrute2026!","role":"FIELD_MANAGER","phoneNumber":"+2250102030407","monthlySalary":90000,"monthlyRequiredHours":208}
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
