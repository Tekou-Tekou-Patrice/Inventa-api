package com.onescompany.Inventa.domain;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OperatingBudgetInitializer implements ApplicationRunner {
    private final OperatingBudgetRepository budgets;

    public OperatingBudgetInitializer(OperatingBudgetRepository budgets) {
        this.budgets = budgets;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!budgets.existsById(OperatingBudget.SINGLETON_ID)) {
            budgets.save(new OperatingBudget(0, "system"));
        }
    }
}
