package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.service.V;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.SequentialOrchestrationWorkflow;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Composite agent that orchestrates {@link CreativeWriter} and {@link StyleEditor}
 * in sequence, backed by a Dapr Workflow via {@link DaprWorkflowPlanner}.
 * <p>
 * Uses {@code @PlannerAgent} with a custom {@code @PlannerSupplier} to wire in the
 * Dapr Workflow-based planner instead of the default {@code SequentialPlanner}.
 */
public interface StoryCreator {

    @PlannerAgent(
            outputKey = "story",
            subAgents = { CreativeWriter.class, StyleEditor.class })
    String write(@V("topic") String topic, @V("style") String style);

    @PlannerSupplier
    static Planner planner() {
        DaprWorkflowClient client = CDI.current().select(DaprWorkflowClient.class).get();
        return new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class,
                "story-planner",
                AgenticSystemTopology.SEQUENCE,
                client);
    }
}
