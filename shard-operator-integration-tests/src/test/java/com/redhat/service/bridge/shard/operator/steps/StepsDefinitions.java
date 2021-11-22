package com.redhat.service.bridge.shard.operator.steps;

import java.io.FileNotFoundException;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.openshift.client.OpenShiftClient;

public class StepsDefinitions {

    private static OpenShiftClient oc;

    @BeforeAll
    public static void setupAll() {
        // oc = new DefaultOpenShiftClient();
    }

    @AfterAll
    public static void teardownAll() throws FileNotFoundException {
        // oc.load(new
        // FileInputStream("/home/ksuta/src/github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/shard-operator/target/kubernetes/minikube.yml")).delete();
    }

    @When("^install shard-operator$")
    public void installShardOperator() throws FileNotFoundException {
        // oc.load(new
        // FileInputStream("/home/ksuta/src/github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/shard-operator/target/kubernetes/minikube.yml")).createOrReplace();
    }

    // @Then("Deployment \"{string}\" has {int} available (?:pod|pods) within {int}
    // (?:minute|minutes)")
    @Then("^Deployment \"([^\"]*)\" has (\\d+) available (?:pod|pods) within (\\d+) (?:minute|minutes)$")
    public void deploymentHasAvailablePodsWithinMinutes(String string, Integer int1, Integer int2)
            throws FileNotFoundException {
        // oc.load(new
        // FileInputStream("/home/ksuta/src/github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/shard-operator/target/kubernetes/minikube.yml")).createOrReplace();
    }

    @Given("a BridgeIngress with:")
    public void a_bridge_ingress_with(io.cucumber.datatable.DataTable dataTable) {
        // Write code here that turns the phrase above into concrete actions
        // For automatic transformation, change DataTable to one of
        // E, List<E>, List<List<E>>, List<Map<K,V>>, Map<K,V> or
        // Map<K, List<V>>. E,K,V must be a String, Integer, Float,
        // Double, Byte, Short, Long, BigInteger or BigDecimal.
        //
        // For other transformations you can register a DataTableType.
        System.out.println("Stub for Given a BridgeIngress");
    }

    @When("deploy BridgeIngress")
    public void deployBridgeIngress() {
        System.out.println("Stub for deploy BridgeIngress");
    }

    @Then("the BridgeIngress exists")
    public void the_bridge_ingress_exists() {
        System.out.println("Stub for then the BridgeIngress exists");
    }

    @Then("the Deployment is {string}")
    public void theDeploymentIs(String statusDeployment) {
        System.out.println("Stub for the status deployment " + statusDeployment);
    }

    @Then("the Service exists")
    public void theServiceExists() {
        System.out.println("Stub for the service exist");
    }

    @Then("the Ingress is ready")
    public void theIngressIsReady() {
        System.out.println("Stub for the Ingress is ready");
    }

    @Then("the BridgeIngress is in phase {string}")
    public void theBridgeIngressIsInPhase(String ingressPhase) {
        System.out.println("Stub for the BridgeIngress is in phase " + ingressPhase);
    }

    @When("delete BridgeIngress")
    public void deleteBridgeIngress() {
        System.out.println("Stub for delete BridgeIngress");

    }

    @Then("the BridgeIngress does not exist")
    public void theBridgeIngressDoesNotExist() {
        System.out.println("Stub for the BridgeIngress does not exist");
    }

    @Then("no Deployment exists")
    public void noDeploymentExists() {
        System.out.println("Stub for no Deployment exists");
    }

    @Then("no Service exists")
    public void noServiceExists() {
        System.out.println("Stub for no Service exists");
    }

    @Then("no Ingress exists")
    public void noIngressExists() {
        System.out.println("Stub for no Ingress exists");
    }

}
