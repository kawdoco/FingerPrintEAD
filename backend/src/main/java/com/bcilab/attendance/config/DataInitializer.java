package com.bcilab.attendance.config;

import com.bcilab.attendance.model.AdminUser;
import com.bcilab.attendance.model.Department;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.model.Person;
import com.bcilab.attendance.model.enums.AdminRole;
import com.bcilab.attendance.model.enums.DeviceStatus;
import com.bcilab.attendance.model.enums.PersonType;
import com.bcilab.attendance.repository.AdminUserRepository;
import com.bcilab.attendance.repository.DepartmentRepository;
import com.bcilab.attendance.repository.FingerprintDeviceRepository;
import com.bcilab.attendance.repository.PersonRepository;
import com.bcilab.attendance.service.impl.DeviceServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates the first admin login, the first kiosk record and (optionally) demo
 * people when the tables are empty.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppProperties appProperties;
    private final AdminUserRepository adminUserRepository;
    private final FingerprintDeviceRepository deviceRepository;
    private final DepartmentRepository departmentRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        AppProperties.Bootstrap cfg = appProperties.getBootstrap();

        if (adminUserRepository.count() == 0) {
            adminUserRepository.save(AdminUser.builder()
                    .username(cfg.getAdminUsername())
                    .passwordHash(passwordEncoder.encode(cfg.getAdminPassword()))
                    .role(AdminRole.SUPER_ADMIN)
                    .build());
            log.info("Created first admin login '{}'", cfg.getAdminUsername());
            if ("ChangeMe123!".equals(cfg.getAdminPassword())) {
                log.warn("Admin is using the DEFAULT password - set APP_ADMIN_PASSWORD before real use.");
            }
        }

        if (deviceRepository.count() == 0) {
            String key = (cfg.getDeviceApiKey() == null || cfg.getDeviceApiKey().isBlank())
                    ? DeviceServiceImpl.generateApiKey()
                    : cfg.getDeviceApiKey();
            deviceRepository.save(FingerprintDevice.builder()
                    .deviceCode(cfg.getDeviceCode())
                    .location(cfg.getDeviceLocation())
                    .apiKey(key)
                    .status(DeviceStatus.OFFLINE)
                    .build());
            log.info("Created first device '{}'. Its API key (put this in the ESP32 firmware): {}",
                    cfg.getDeviceCode(), key);
        }

        if (cfg.isSeedDemoData() && personRepository.count() == 0) {
            seedDemoPeople();
        }
    }

    private void seedDemoPeople() {
        List<Object[]> rows = List.of(
                new Object[]{PersonType.STUDENT, "BCI/2024/001", "Nadeesha Perera", "Neuro Systems"},
                new Object[]{PersonType.STUDENT, "BCI/2024/002", "Kasun Fernando", "Signal Processing"},
                new Object[]{PersonType.STUDENT, "BCI/2024/003", "Achini Silva", "Neuro Systems"},
                new Object[]{PersonType.STUDENT, "BCI/2024/004", "Ravindu Jayasuriya", "Robotics"},
                new Object[]{PersonType.STUDENT, "BCI/2024/005", "Hiruni Wickramasinghe", "Data Science"},
                new Object[]{PersonType.LECTURER, "STAFF/012", "Dr. Tharindu Bandara", "Signal Processing"},
                new Object[]{PersonType.REV_FATHER, null, "Rev. Fr. Anton Perera", null},
                new Object[]{PersonType.GUEST, null, "Sanduni Rathnayake (Visitor)", null});

        int slot = 1;
        for (Object[] r : rows) {
            PersonType type = (PersonType) r[0];
            String idNumber = (String) r[1];
            String name = (String) r[2];
            String deptName = (String) r[3];

            Department dept = null;
            if (deptName != null) {
                dept = departmentRepository.findByNameIgnoreCase(deptName)
                        .orElseGet(() -> departmentRepository.save(Department.builder().name(deptName).build()));
            }

            personRepository.save(Person.builder()
                    .personType(type)
                    .idNumber(idNumber)
                    .fullName(name)
                    .department(dept)
                    .fingerprintTemplateId(slot++)
                    .active(true)
                    .build());
        }
        log.info("Seeded {} demo people (fingerprint slots 1-{})", rows.size(), rows.size());
    }
}
