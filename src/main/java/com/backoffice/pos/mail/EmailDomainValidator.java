package com.backoffice.pos.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Best-effort check that an email's DOMAIN can receive mail (has MX or A DNS
 * records). This catches typos like "gmail.con" or made-up domains. It cannot
 * prove a specific mailbox exists — that's what the verification link does — so
 * on any DNS error it is lenient and lets the address through.
 */
@Component
public class EmailDomainValidator {

    private static final Logger log = LoggerFactory.getLogger(EmailDomainValidator.class);

    public boolean hasMailDomain(String email) {
        if (email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1).trim();
        if (domain.isEmpty() || !domain.contains(".")) {
            return false;
        }
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ctx = new InitialDirContext(env);
            try {
                Attributes attrs = ctx.getAttributes(domain, new String[]{"MX", "A"});
                return attrs.get("MX") != null || attrs.get("A") != null;
            } finally {
                ctx.close();
            }
        } catch (NameNotFoundException e) {
            return false; // domain genuinely does not exist
        } catch (Exception e) {
            // DNS unreachable / timeout — don't block signup on infrastructure flakiness.
            log.warn("Could not verify mail domain {}: {}", domain, e.getMessage());
            return true;
        }
    }
}
