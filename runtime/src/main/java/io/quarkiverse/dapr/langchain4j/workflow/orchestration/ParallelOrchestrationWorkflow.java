package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import java.util.ArrayList;
import java.util.List;

import io.dapr.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow that executes all agents in parallel and waits for all to complete.
 */
@ApplicationScoped
public class ParallelOrchestrationWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {
            OrchestrationInput input = ctx.getInput(OrchestrationInput.class);
            List<Task<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < input.agentCount(); i++) {
                tasks.add(ctx.callActivity(AgentExecutionActivity.class.getName(),
                        new AgentExecInput(input.plannerId(), i), Void.class));
            }
            ctx.allOf(tasks).await();
            // Signal planner that the workflow has completed
            DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
            if (planner != null) {
                planner.signalWorkflowComplete();
            }
        };
    }
}
