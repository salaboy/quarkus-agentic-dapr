package io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities;

import java.util.concurrent.CompletableFuture;

import dev.langchain4j.agentic.planner.AgentInstance;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.AgentExecInput;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr WorkflowActivity that bridges the Dapr Workflow execution to the
 * Langchain4j planner. When invoked by a workflow, it:
 * <ol>
 *   <li>Looks up the planner from the registry</li>
 *   <li>Gets the agent instance at the specified index</li>
 *   <li>Submits the agent to the planner's exchange queue</li>
 *   <li>Blocks until the planner completes the agent execution</li>
 * </ol>
 */
@ApplicationScoped
public class AgentExecutionActivity implements WorkflowActivity {

    @Override
    public Object run(WorkflowActivityContext ctx) {
        AgentExecInput input = ctx.getInput(AgentExecInput.class);
        DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
        if (planner == null) {
            throw new IllegalStateException("No planner found for ID: " + input.plannerId()
                    + ". Registered IDs: " + DaprPlannerRegistry.getRegisteredIds());
        }
        AgentInstance agent = planner.getAgent(input.agentIndex());
        CompletableFuture<Void> future = planner.executeAgent(agent);
        future.join(); // blocks until planner processes this agent
        return null;
    }
}
