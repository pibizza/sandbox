package com.redhat.service.bridge.shard.operator.providers;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Route;

@ApplicationScoped
public class TemplateProviderImpl implements TemplateProvider {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String BRIDGE_DEPLOYMENT_PATH = TEMPLATES_DIR + "/bridge-ingress-deployment.yaml";
    private static final String BRIDGE_SERVICE_PATH = TEMPLATES_DIR + "/bridge-ingress-service.yaml";
    private static final String BRIDGE_OPENSHIFT_ROUTE_PATH = TEMPLATES_DIR + "/bridge-ingress-openshift-route.yaml";
    private static final String BRIDGE_KUBERNETES_INGRESS_PATH = TEMPLATES_DIR + "/bridge-ingress-kubernetes-ingress.yaml";

    @Override
    public Deployment loadBridgeDeploymentTemplate(BridgeIngress bridgeIngress) {
        Deployment deployment = loadYaml(Deployment.class, BRIDGE_DEPLOYMENT_PATH);
        updateMetadata(bridgeIngress, deployment.getMetadata());
        return deployment;
    }

    @Override
    public Service loadBridgeServiceTemplate(BridgeIngress bridgeIngress) {
        Service service = loadYaml(Service.class, BRIDGE_SERVICE_PATH);
        updateMetadata(bridgeIngress, service.getMetadata());
        return service;
    }

    @Override
    public Route loadBridgeOpenshiftRouteTemplate(BridgeIngress bridgeIngress) {
        Route route = loadYaml(Route.class, BRIDGE_OPENSHIFT_ROUTE_PATH);
        updateMetadata(bridgeIngress, route.getMetadata());
        return route;
    }

    @Override
    public Ingress loadBridgeKubernetesIngressTemplate(BridgeIngress bridgeIngress) {
        Ingress ingress = loadYaml(Ingress.class, BRIDGE_KUBERNETES_INGRESS_PATH);
        updateMetadata(bridgeIngress, ingress.getMetadata());
        return ingress;
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = TemplateProviderImpl.class.getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

    private void updateMetadata(BridgeIngress bridgeIngress, ObjectMeta meta) {
        // Name and namespace
        meta.setName(bridgeIngress.getMetadata().getName());
        meta.setNamespace(bridgeIngress.getMetadata().getNamespace());

        // Owner reference
        meta.getOwnerReferences().get(0).setKind(bridgeIngress.getKind());
        meta.getOwnerReferences().get(0).setName(bridgeIngress.getMetadata().getName());
        meta.getOwnerReferences().get(0).setApiVersion(bridgeIngress.getApiVersion());
        meta.getOwnerReferences().get(0).setUid(bridgeIngress.getMetadata().getUid());
    }
}
