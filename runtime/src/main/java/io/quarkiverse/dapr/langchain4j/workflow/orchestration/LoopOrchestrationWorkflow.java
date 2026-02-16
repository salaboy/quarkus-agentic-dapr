package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.ExitConditionCheckActivity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow that loops through agents repeatedly until an exit condition
 * is met or the maximum number of iterations is reached.
 */
@ApplicationScoped
public class LoopOrchestrationWorkflow implements Workflow {

    @Override
    public WorkflowStub create() {
        return ctx -> {
            OrchestrationInput input = ctx.getInput(OrchestrationInput.class);

            for (int iter = 0; iter < input.maxIterations(); iter++) {
                // Check exit condition at loop start (unless configured to check at end)
                if (!input.testExitAtLoopEnd()) {
                    boolean exit = ctx.callActivity(ExitConditionCheckActivity.class.getName(),
                            new ExitConditionCheckInput(input.plannerId(), iter),
                            Boolean.class).await();
                    if (exit) {
                        break;
                    }
                }

                // Execute all agents sequentially within this iteration
                for (int i = 0; i < input.agentCount(); i++) {
                    ctx.callActivity(AgentExecutionActivity.class.getName(),
                            new AgentExecInput(input.plannerId(), i), Void.class).await();
                }

                // Check exit condition at loop end (if configured)
                if (input.testExitAtLoopEnd()) {
                    boolean exit = ctx.callActivity(ExitConditionCheckActivity.class.getName(),
                            new ExitConditionCheckInput(input.plannerId(), iter),
                            Boolean.class).await();
                    if (exit) {
                        break;
                    }
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
