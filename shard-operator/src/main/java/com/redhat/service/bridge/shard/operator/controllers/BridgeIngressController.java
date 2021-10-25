package com.redhat.service.bridge.shard.operator.controllers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngressStatus;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

/**
 * To be implemented on <a href="https://issues.redhat.com/browse/MGDOBR-93">MGDOBR-93</a>
 */
@ApplicationScoped
@Controller
public class BridgeIngressController implements ResourceController<BridgeIngress> {

    // TODO: not using now, but let it here to make sure our configuration is ok and the object is being injected.
    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public UpdateControl<BridgeIngress> createOrUpdateResource(BridgeIngress bridgeIngress, Context<BridgeIngress> context) {
        // simplistic reconciliation to check with IT
        if (bridgeIngress.getStatus() == null) {
            bridgeIngress.setStatus(new BridgeIngressStatus());
        }

        if (bridgeIngress.getStatus().getStatus() == null || bridgeIngress.getStatus().getStatus().isEmpty()) {
            bridgeIngress.getStatus().setStatus("OK");
            return UpdateControl.updateStatusSubResource(bridgeIngress);
        }
        return UpdateControl.noUpdate();
    }
}