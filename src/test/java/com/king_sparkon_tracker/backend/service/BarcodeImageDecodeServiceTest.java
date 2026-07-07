package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class BarcodeImageDecodeServiceTest {

    private final BarcodeImageDecodeService service = new BarcodeImageDecodeService();

    @Test
    void decodeRejectsUnsupportedImageType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "barcode.svg",
                "image/svg+xml",
                "<svg/>".getBytes()
        );

        assertThatThrownBy(() -> service.decode(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PNG, JPG, and JPEG");
    }

    @Test
    void decodeRejectsEmptyImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "barcode.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> service.decode(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode image is required");
    }
}
