package com.backoffice.pos.catalog;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link CatalogStatus} as its uppercase name, and reads it
 * case-insensitively. This hardens against manually/bulk-inserted rows that
 * store the status in the wrong case (e.g. {@code 'active'}), which would
 * otherwise blow up entity hydration with "No enum constant …". Applied
 * automatically to every {@code CatalogStatus} attribute via {@code autoApply}.
 */
@Converter(autoApply = true)
public class CatalogStatusConverter implements AttributeConverter<CatalogStatus, String> {

    @Override
    public String convertToDatabaseColumn(CatalogStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public CatalogStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        return CatalogStatus.valueOf(dbValue.trim().toUpperCase());
    }
}
