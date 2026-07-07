package com.king_sparkon_tracker.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class HybridBarcodeModelTest {

    @Test
    void productBarcodeCanBeSharedAcrossManyStockUnitsWhileUnitCodesStayDistinct() {
        Product product = new Product("Coca-Cola 500ml", ProductCategory.Alcohol, BigDecimal.valueOf(20), 2);
        product.setProductBarcode("5449000000996");

        ProductBarcode firstUnit = new ProductBarcode("KST-UNIT-000001", "5449000000996");
        ProductBarcode secondUnit = new ProductBarcode("KST-UNIT-000002", "5449000000996");

        product.addBarcode(firstUnit);
        product.addBarcode(secondUnit);

        assertThat(product.getProductBarcode()).isEqualTo("5449000000996");
        assertThat(product.getBarcodes())
                .extracting(ProductBarcode::getBarcode)
                .containsExactly("5449000000996", "5449000000996");
        assertThat(product.getBarcodes())
                .extracting(ProductBarcode::getUnitCode)
                .containsExactly("KST-UNIT-000001", "KST-UNIT-000002");
    }
}
