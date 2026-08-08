package com.backoffice.pos.staff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The default permission catalog + starter roles seeded for every new business. */
public final class DefaultPermissions {

    private DefaultPermissions() {
    }

    /** key -> human description */
    public static final Map<String, String> CATALOG = buildCatalog();

    /** Role name -> permission keys it grants ("*" means all). */
    public static final Map<String, List<String>> ROLE_TEMPLATES = Map.of(
            "Manager", List.of("*"),
            "Cashier", List.of(
                    "dashboard.view", "product.view", "category.view", "service.view",
                    "floor.view", "table.view",
                    "order.view", "order.create", "order.pay"),
            "Kitchen", List.of("order.view")
    );

    private static Map<String, String> buildCatalog() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("dashboard.view", "View the dashboard");
        put(m, "store", "store");
        put(m, "category", "category");
        put(m, "product", "product");
        put(m, "service", "service");
        m.put("inventory.view", "View inventory");
        m.put("inventory.edit", "Edit inventory");
        put(m, "employee", "employee");
        put(m, "role", "role");
        put(m, "permission", "permission");
        put(m, "floor", "floor");
        put(m, "table", "table");
        m.put("order.view", "View orders");
        m.put("order.create", "Create orders");
        m.put("order.void", "Void orders");
        m.put("order.pay", "Take payment for orders");
        return m;
    }

    private static void put(Map<String, String> m, String resource, String label) {
        m.put(resource + ".view", "View " + label + "s");
        m.put(resource + ".create", "Create " + label);
        m.put(resource + ".edit", "Edit " + label);
        m.put(resource + ".delete", "Delete " + label);
    }
}
