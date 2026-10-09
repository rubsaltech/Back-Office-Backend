package com.backoffice.pos;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** TEMPORARY — verifies generic product search (name/SKU/barcode) on Neon. Read-only. Delete after. */
@SpringBootTest
@ActiveProfiles("local")
class ProductSearchTest {

    @Autowired ProductRepository products;

    @Test
    void genericSearch() {
        Product sample = products.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().length() >= 3)
                .findFirst().orElseThrow(() -> new IllegalStateException("no product with a sku"));
        Long storeId = sample.getStoreId();

        String skuFrag = sample.getSku().substring(0, Math.min(4, sample.getSku().length()));
        var bySku = products.searchByStore(storeId, skuFrag, PageRequest.of(0, 20));
        System.out.println(">>> SRCH sku('" + skuFrag + "') matched=" + bySku.getTotalElements());
        assertTrue(bySku.getContent().stream().anyMatch(p -> p.getId().equals(sample.getId())),
                "search by SKU fragment must find the product");

        String nameFrag = sample.getName().substring(0, Math.min(3, sample.getName().length()));
        var byName = products.searchByStore(storeId, nameFrag, PageRequest.of(0, 20));
        System.out.println(">>> SRCH name('" + nameFrag + "') matched=" + byName.getTotalElements());
        assertTrue(byName.getTotalElements() > 0, "search by name fragment must find something");

        Product withBarcode = products.findAll().stream()
                .filter(p -> p.getBarcode() != null && !p.getBarcode().isBlank())
                .findFirst().orElse(null);
        if (withBarcode != null) {
            var byBc = products.searchByStore(withBarcode.getStoreId(), withBarcode.getBarcode(), PageRequest.of(0, 20));
            System.out.println(">>> SRCH barcode matched=" + byBc.getTotalElements());
            assertTrue(byBc.getContent().stream().anyMatch(p -> p.getId().equals(withBarcode.getId())),
                    "search by exact barcode must find the product");
        } else {
            System.out.println(">>> SRCH no product has a barcode to test");
        }
        System.out.println(">>> SRCH PASSED");
    }
}
