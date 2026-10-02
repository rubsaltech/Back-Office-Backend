package com.backoffice.pos.label;

/**
 * The category of a custom label, deciding how its value is captured on a
 * product/service form:
 * <ul>
 *   <li>{@code INPUT} — a free-text value typed at attach time (e.g. IMEI).</li>
 *   <li>{@code SINGLE_SELECT} — exactly one of the label's options.</li>
 *   <li>{@code MULTI_SELECT} — one or more of the label's options.</li>
 * </ul>
 */
public enum LabelType {
    INPUT,
    SINGLE_SELECT,
    MULTI_SELECT
}
