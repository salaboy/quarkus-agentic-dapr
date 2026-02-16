package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

/**
 * Input for the AgentExecutionActivity.
 *
 * @param plannerId  the planner ID to look up in the registry
 * @param agentIndex the index of the agent in the planner's agent list
 */
public record AgentExecInput(String plannerId, int agentIndex) {
}
