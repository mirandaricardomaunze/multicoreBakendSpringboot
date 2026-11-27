package mz.multicore.erp.modules.webhooks.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** DNS port used by the anti-SSRF check (§4.1.1), so tests resolve host names without the network. */
public interface WebhookAddressResolver {

    List<InetAddress> resolve(String host) throws UnknownHostException;
}
