package com.developer.service.bridge.k8s;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesResourceType;
import com.redhat.service.bridge.infra.k8s.ResourceEvent;
import com.redhat.service.bridge.ingress.IngressService;

import io.fabric8.kubernetes.api.model.LoadBalancerIngressBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatusBuilder;

@ApplicationScoped
public class NetworkManagerImpl implements NetworkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkManagerImpl.class);

    private final Map<String, Ingress> ingressEndpointMap = new HashMap<>();

    @Inject
    Event<ResourceEvent> event;

    @Inject
    DeploymentsManager deploymentsManager;

    @Inject
    IngressService ingressService;

    @Override
    public void createOrUpdate(Ingress ingress) {
        String name = ingress.getMetadata().getName();

        IngressStatus status = new IngressStatusBuilder()
                .withLoadBalancer(new LoadBalancerStatusBuilder()
                        .withIngress(new LoadBalancerIngressBuilder()
                                .withIp("127.0.0.1")
                                .build())
                        .build())
                .build();

        ingress.setStatus(status);

        Deployment deployment = deploymentsManager.getDeployment(name);
        if (deployment == null || deployment.getStatus().getConditions().stream().noneMatch(x -> x.getStatus().equals("Ready"))) {
            LOGGER.warn("[k8s] Illegal state: ingress is deployed but not the Deployment. Since this is a mock, this should not happen.");
            return;
        }

        String type = KubernetesUtils.extractLabelFromMetadata(ingress, K8SBridgeConstants.METADATA_TYPE);
        Action action;
        if (type.equals(K8SBridgeConstants.BRIDGE_TYPE)) {
            ingressService.deploy(name);
            if (ingressEndpointMap.containsKey(name)) {
                ingressEndpointMap.replace(name, ingress);
                action = Action.MODIFIED;
            } else {
                ingressEndpointMap.put(name, ingress);
                action = Action.ADDED;
            }
        } else {
            throw new IllegalStateException("[k8s] It's possible to create an Ingress only for Bridge Ingress applications.");
        }

        event.fire(new ResourceEvent(KubernetesResourceType.INGRESS, type, name, action));
    }

    @Override
    public void delete(String id) {
        if (ingressEndpointMap.containsKey(id)) {
            Ingress ingress = ingressEndpointMap.get(id);
            ingressEndpointMap.remove(id);
            ingressService.undeploy(id);
            event.fire(new ResourceEvent(KubernetesResourceType.INGRESS, KubernetesUtils.extractLabelFromMetadata(ingress, K8SBridgeConstants.METADATA_TYPE), id, Action.DELETED));
        }
    }

    @Override
    public Ingress getIngress(String name) {
        return ingressEndpointMap.get(name);
    }
}
