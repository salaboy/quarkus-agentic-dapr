package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow that executes agents sequentially, one after another.
 * Each agent step is a call to the {@link AgentExecutionActivity}.
 */
@ApplicationScoped
public class SequentialOrchestrationWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {
            OrchestrationInput input = ctx.getInput(OrchestrationInput.class);
            for (int i = 0; i < input.agentCount(); i++) {
                ctx.callActivity(AgentExecutionActivity.class.getName(),
                        new AgentExecInput(input.plannerId(), i), Void.class).await();
            }
            // Signal planner that the workflow has completed
            DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
            if (planner != null) {
                planner.signalWorkflowComplete();
            }
        };
    }
}
