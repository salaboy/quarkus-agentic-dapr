package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.ConditionCheckActivity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow that conditionally executes agents based on runtime predicates.
 * For each agent, a condition check activity determines whether to execute it.
 */
@ApplicationScoped
public class ConditionalOrchestrationWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {
            OrchestrationInput input = ctx.getInput(OrchestrationInput.class);

            for (int i = 0; i < input.agentCount(); i++) {
                boolean shouldExec = ctx.callActivity(ConditionCheckActivity.class.getName(),
                        new ConditionCheckInput(input.plannerId(), i),
                        Boolean.class).await();
                if (shouldExec) {
                    ctx.callActivity(AgentExecutionActivity.class.getName(),
                            new AgentExecInput(input.plannerId(), i), Void.class).await();
                }
            }
            // Signal planner that the workflow has completed
            DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
            if (planner != null) {
                planner.signalWorkflowComplete();
            }
        };
    }
}
