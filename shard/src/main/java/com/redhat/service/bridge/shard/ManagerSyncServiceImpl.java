package com.redhat.service.bridge.shard;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesClient;
import com.redhat.service.bridge.infra.k8s.crds.BridgeCustomResource;
import com.redhat.service.bridge.infra.k8s.crds.ProcessorCustomResource;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.exceptions.DeserializationException;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    @Inject
    ObjectMapper mapper;

    @Inject
    WebClient webClientManager;

    @Inject
    KubernetesClient kubernetesClient;

    @Scheduled(every = "30s")
    void syncUpdatesFromManager() {
        LOGGER.info("[Shard] Fetching updates from Manager for Bridges and Processors to deploy and delete");
        fetchAndProcessBridgesToDeployOrDelete().subscribe().with(
                success -> processingComplete(BridgeDTO.class),
                failure -> processingFailed(BridgeDTO.class, failure));

        fetchAndProcessProcessorsToDeployOrDelete().subscribe().with(
                success -> processingComplete(ProcessorDTO.class),
                failure -> processingFailed(ProcessorDTO.class, failure));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO) {
        LOGGER.info("[shard] Notifying manager about the new status of the Bridge '{}'", bridgeDTO.getId());
        return webClientManager.put(APIConstants.SHARD_API_BASE_PATH).sendJson(bridgeDTO);
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO) {
        return webClientManager.put(APIConstants.SHARD_API_BASE_PATH + "processors").sendJson(processorDTO);
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeployOrDelete() {
        return webClientManager.get(APIConstants.SHARD_API_BASE_PATH).send()
                .onItem().transform(this::getBridges)
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    if (y.getStatus().equals(BridgeStatus.REQUESTED)) { // Bridges to deploy
                                        y.setStatus(BridgeStatus.PROVISIONING);
                                        return notifyBridgeStatusChange(y).subscribe().with(
                                                success -> deployBridgeCustomResource(y),
                                                failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    if (y.getStatus().equals(BridgeStatus.DELETION_REQUESTED)) { // Bridges to delete
                                        y.setStatus(BridgeStatus.DELETED);
                                        deleteBridgeCustomResource(y);
                                        return notifyBridgeStatusChange(y).subscribe().with(
                                                success -> LOGGER.info("[shard] Delete notification for Bridge '{}' has been sent to the manager successfully", y.getId()),
                                                failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    LOGGER.warn("[shard] Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                                    return Uni.createFrom().voidItem();
                                }).collect(Collectors.toList())));
    }

    @Override
    public Uni<Object> fetchAndProcessProcessorsToDeployOrDelete() {
        return webClientManager.get(APIConstants.SHARD_API_BASE_PATH + "processors")
                .send().onItem().transform(this::getProcessors)
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream()
                        .map(y -> {
                            if (BridgeStatus.REQUESTED.equals(y.getStatus())) {
                                y.setStatus(BridgeStatus.PROVISIONING);
                                return notifyProcessorStatusChange(y).subscribe().with(
                                        success -> deployProcessorCustomResource(y),
                                        failure -> failedToSendUpdateToManager(y, failure));
                            }
                            if (BridgeStatus.DELETION_REQUESTED.equals(y.getStatus())) { // Processor to delete
                                y.setStatus(BridgeStatus.DELETED);
                                deleteProcessorCustomResource(y);
                                return notifyProcessorStatusChange(y).subscribe().with(
                                        success -> LOGGER.info("[shard] Delete notification for Bridge '{}' has been sent to the manager successfully", y.getId()),
                                        failure -> failedToSendUpdateToManager(y, failure));
                            }
                            return Uni.createFrom().voidItem();
                        }).collect(Collectors.toList())));
    }

    // Create the custom resource, and let the controller create what it needs
    protected void deployBridgeCustomResource(BridgeDTO bridgeDTO) {
        kubernetesClient.createOrUpdateCustomResource(bridgeDTO.getId(), BridgeCustomResource.fromDTO(bridgeDTO), K8SBridgeConstants.BRIDGE_TYPE);
    }

    protected void deleteBridgeCustomResource(BridgeDTO bridgeDTO) {
        kubernetesClient.deleteCustomResource(bridgeDTO.getId(), K8SBridgeConstants.BRIDGE_TYPE);
    }

    // Create the custom resource, and let the controller create what it needs
    protected void deployProcessorCustomResource(ProcessorDTO processorDTO) {
        kubernetesClient.createOrUpdateCustomResource(processorDTO.getId(), ProcessorCustomResource.fromDTO(processorDTO), K8SBridgeConstants.PROCESSOR_TYPE);
    }

    protected void deleteProcessorCustomResource(ProcessorDTO processorDTO) {
        kubernetesClient.deleteCustomResource(processorDTO.getId(), K8SBridgeConstants.PROCESSOR_TYPE);
    }

    private List<ProcessorDTO> getProcessors(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<List<ProcessorDTO>>() {
        });
    }

    private List<BridgeDTO> getBridges(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<List<BridgeDTO>>() {
        });
    }

    private <T> List<T> deserializeResponseBody(HttpResponse<Buffer> httpResponse, TypeReference<List<T>> typeReference) {
        try {
            return mapper.readValue(httpResponse.bodyAsString(), typeReference);
        } catch (JsonProcessingException e) {
            LOGGER.warn("[shard] Failed to deserialize response from Manager", e);
            throw new DeserializationException("Failed to deserialize response from Manager.", e);
        }
    }

    private void failedToSendUpdateToManager(Object entity, Throwable t) {
        LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", entity.getClass().getSimpleName(), t);
    }

    private void processingFailed(Class<?> entity, Throwable t) {
        LOGGER.error("[shard] Failure processing entities '{}' to be deployed or deleted", entity.getSimpleName(), t);
    }

    private void processingComplete(Class<?> entity) {
        LOGGER.info("[shard] Successfully processed all entities '{}' to deploy or delete", entity.getSimpleName());
    }
}
