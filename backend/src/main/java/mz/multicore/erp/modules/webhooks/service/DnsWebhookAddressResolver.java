package mz.multicore.erp.modules.webhooks.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Resolves every address of a host through the JVM resolver. */
@Component
public class DnsWebhookAddressResolver implements WebhookAddressResolver {

    @Override
    public List<InetAddress> resolve(String host) throws UnknownHostException {
        return List.of(InetAddress.getAllByName(host));
    }
}
