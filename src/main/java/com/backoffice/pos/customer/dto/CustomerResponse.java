package com.backoffice.pos.customer.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.customer.Customer;
import com.backoffice.pos.customer.CustomerTransaction;
import com.backoffice.pos.customer.CustomerTxnType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String city,
        String country,
        String note,
        CatalogStatus status,
        BigDecimal balance,
        BigDecimal totalCredit,
        BigDecimal totalDebit,
        Instant createdAt,
        List<TxnInfo> transactions
) {
    public record TxnInfo(
            Long id,
            CustomerTxnType type,
            BigDecimal amount,
            String note,
            Long orderId,
            Instant createdAt
    ) {
    }

    /** Summary for lists — no ledger lines. */
    public static CustomerResponse summary(Customer c) {
        return build(c, List.of(), false);
    }

    /** Full detail including the ledger + credit/debit totals. */
    public static CustomerResponse detail(Customer c, List<CustomerTransaction> txns) {
        return build(c, txns, true);
    }

    private static CustomerResponse build(Customer c, List<CustomerTransaction> txns, boolean includeTxns) {
        BigDecimal credit = BigDecimal.ZERO;
        BigDecimal debit = BigDecimal.ZERO;
        for (CustomerTransaction tx : txns) {
            BigDecimal amt = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
            if (tx.getType() == CustomerTxnType.CREDIT) credit = credit.add(amt);
            else debit = debit.add(amt);
        }
        List<TxnInfo> lines = includeTxns
                ? txns.stream().map(tx -> new TxnInfo(tx.getId(), tx.getType(), tx.getAmount(), tx.getNote(), tx.getOrderId(), tx.getCreatedAt())).toList()
                : null;
        return new CustomerResponse(
                c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAddress(), c.getCity(), c.getCountry(),
                c.getNote(), c.getStatus(), c.getBalance() == null ? BigDecimal.ZERO : c.getBalance(),
                credit, debit, c.getCreatedAt(), lines);
    }
}
