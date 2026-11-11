package com.onescompany.Inventa.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.Worker;

@Entity
@Table(name = "app_users")
public class AppUser implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;

    @Column(length = 40)
    private String phoneNumber = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_worker_id", unique = true)
    private Worker payrollWorker;

    protected AppUser() {
    }

    public AppUser(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public AppUser(String username, String password, UserRole role, PlantationField field) {
        this(username, password, role);
        this.field = field;
    }

    public AppUser(String username, String password, UserRole role, String phoneNumber, PlantationField field) {
        this(username, password, role, field);
        this.phoneNumber = phoneNumber;
    }

    public AppUser(
            String username,
            String password,
            UserRole role,
            String phoneNumber,
            PlantationField field,
            Worker payrollWorker) {
        this(username, password, role, phoneNumber, field);
        this.payrollWorker = payrollWorker;
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void deleteFromManagement() {
        this.deleted = true;
        this.enabled = false;
    }

    public void assignField(PlantationField field) {
        this.field = field;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updateCredentials(String username, String password, String phoneNumber) {
        this.username = username;
        if (password != null) {
            this.password = password;
        }
        this.phoneNumber = phoneNumber;
    }

    public String getUsernameValue() { return username; }
    public PlantationField getField() { return field; }
    public String getPhoneNumber() { return phoneNumber; }
    public Worker getPayrollWorker() { return payrollWorker; }
    public boolean isDeleted() { return deleted; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
