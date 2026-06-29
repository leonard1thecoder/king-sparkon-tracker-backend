package com.king_sparkon_tracker.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import org.junit.jupiter.api.Test;

class ServiceRegistrationForTest {

    @Test
    void mapsRegistrationLabelsToBackendPrivileges() {
        assertThat(ServiceRegistrationFor.from("USER")).isEqualTo(ServiceRegistrationFor.USER);
        assertThat(ServiceRegistrationFor.from("business owner")).isEqualTo(ServiceRegistrationFor.BUSINESS_OWNER);
        assertThat(ServiceRegistrationFor.from("AFFLIATE")).isEqualTo(ServiceRegistrationFor.AFFILIATE);
        assertThat(ServiceRegistrationFor.USER.privilegeRole()).isEqualTo(PrivilegeRole.User);
        assertThat(ServiceRegistrationFor.BUSINESS_OWNER.privilegeRole()).isEqualTo(PrivilegeRole.Owner);
        assertThat(ServiceRegistrationFor.AFFILIATE.privilegeRole()).isEqualTo(PrivilegeRole.Affiliate);
    }

    @Test
    void blankSelectionDefaultsToBusinessOwnerForLegacyPayloads() {
        assertThat(ServiceRegistrationFor.from(null)).isEqualTo(ServiceRegistrationFor.BUSINESS_OWNER);
        assertThat(ServiceRegistrationFor.from(" ")).isEqualTo(ServiceRegistrationFor.BUSINESS_OWNER);
    }

    @Test
    void unsupportedSelectionIsRejected() {
        assertThatThrownBy(() -> ServiceRegistrationFor.from("super-admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported registration privilege: super-admin");
    }
}
