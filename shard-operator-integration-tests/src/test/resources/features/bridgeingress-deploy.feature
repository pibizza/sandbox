Feature: BridgeIngress deploy and undeploy

  Scenario: BridgeIngress is in phase AVAILABLE
     Given a BridgeIngress with: 
     | id            | my-id        | 
     | customerId    | customer     | 
     | bridgeName    | myBridge     | 
     | image         | todo:latest  | 

     When deploy BridgeIngress
     
     Then the BridgeIngress exists
     And the Deployment is "ready"
     And the Service exists
     And the Ingress is ready
     And the BridgeIngress is in phase "AVAILABLE"
      
  Scenario: BridgeIngress gets deleted
     Given a BridgeIngress with: 
     |  id          | my-id       | 
     | customerId   | customer    | 
     | bridgeName   | myBridge    | 
     | image        | todo:latest | 

     When delete BridgeIngress
     
     Then the BridgeIngress does not exist
     And no Deployment exists
     And no Service exists
     And no Ingress exists      
