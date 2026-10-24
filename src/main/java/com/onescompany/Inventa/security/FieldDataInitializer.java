package com.onescompany.Inventa.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.onescompany.Inventa.domain.ExpenseRepository;
import com.onescompany.Inventa.domain.FieldTaskRepository;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.domain.StockItemRepository;
import com.onescompany.Inventa.domain.WorkerRepository;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FieldDataInitializer implements CommandLineRunner {
    private final PlantationFieldRepository fields;
    private final AppUserRepository users;
    private final WorkerRepository workers;
    private final StockItemRepository stockItems;
    private final FieldTaskRepository tasks;
    private final ExpenseRepository expenses;

    public FieldDataInitializer(
            PlantationFieldRepository fields,
            AppUserRepository users,
            WorkerRepository workers,
            StockItemRepository stockItems,
            FieldTaskRepository tasks,
            ExpenseRepository expenses) {
        this.fields = fields;
        this.users = users;
        this.workers = workers;
        this.stockItems = stockItems;
        this.tasks = tasks;
        this.expenses = expenses;
    }

    @Override
    @Transactional
    public void run(String... args) {
        PlantationField defaultField = fields.findAll().stream().findFirst().orElseGet(
                () -> fields.save(new PlantationField(
                        "Parcelle principale", "À définir", "Champ créé automatiquement pour les données historiques.")));

        users.findAll().stream()
                .filter(user -> user.getRole() == UserRole.FIELD_MANAGER && user.getField() == null)
                .forEach(user -> user.assignField(defaultField));
        workers.findAll().stream()
                .filter(worker -> worker.getField() == null)
                .forEach(worker -> worker.assignField(defaultField));
        stockItems.findAll().stream()
                .filter(item -> item.getField() == null)
                .forEach(item -> item.assignField(defaultField));
        tasks.findAll().stream()
                .filter(task -> task.getField() == null)
                .forEach(task -> task.assignField(defaultField));
        expenses.findAll().stream()
                .filter(expense -> expense.getField() == null)
                .forEach(expense -> expense.assignField(defaultField));
    }
}
