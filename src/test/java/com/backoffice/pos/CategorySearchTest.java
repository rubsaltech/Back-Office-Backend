package com.backoffice.pos;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** TEMPORARY — verifies product search also matches category name (and keeps null-category products). Read-only. Delete after. */
@SpringBootTest
@ActiveProfiles("local")
class CategorySearchTest {

    @Autowired ProductRepository products;

    @Test
    void searchByCategory() {
        // A product that HAS a category — searching its category name must find it.
        Product withCat = products.findAll().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getName() != null
                        && p.getCategory().getName().length() >= 3)
                .findFirst().orElse(null);
        if (withCat != null) {
            String catFrag = withCat.getCategory().getName().substring(0, 4);
            var byCat = products.searchByStore(withCat.getStoreId(), catFrag, PageRequest.of(0, 50));
            System.out.println(">>> CAT search('" + catFrag + "') matched=" + byCat.getTotalElements());
            assertTrue(byCat.getContent().stream().anyMatch(p -> p.getId().equals(withCat.getId())),
                    "searching a category name must return products in that category");
        } else {
            System.out.println(">>> CAT no categorized product found to test");
        }

        // A product with NO category must still be found by its own name (LEFT JOIN).
        Product noCat = products.findAll().stream()
                .filter(p -> p.getCategory() == null && p.getName() != null && p.getName().length() >= 3)
                .findFirst().orElse(null);
        if (noCat != null) {
            String nameFrag = noCat.getName().substring(0, 3);
            var byName = products.searchByStore(noCat.getStoreId(), nameFrag, PageRequest.of(0, 50));
            System.out.println(">>> CAT null-category product by name('" + nameFrag + "') matched=" + byName.getTotalElements());
            assertTrue(byName.getContent().stream().anyMatch(p -> p.getId().equals(noCat.getId())),
                    "a product without a category must still be searchable by name");
        } else {
            System.out.println(">>> CAT no null-category product to test (fine)");
        }
        System.out.println(">>> CAT PASSED");
    }
}
