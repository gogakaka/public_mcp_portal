package com.umg.adapter;

import com.umg.domain.enums.ToolType;
import org.springframework.stereotype.Component;

/**
 * Factory that returns the correct {@link ToolExecutor} implementation
 * based on the tool's {@link ToolType}.
 */
@Component
public class ToolExecutorFactory {

    private final N8nAdapter n8nAdapter;
    private final CubeJsAdapter cubeJsAdapter;
    private final AwsRemoteMcpProxyAdapter awsRemoteMcpProxyAdapter;

    public ToolExecutorFactory(N8nAdapter n8nAdapter,
                               CubeJsAdapter cubeJsAdapter,
                               AwsRemoteMcpProxyAdapter awsRemoteMcpProxyAdapter) {
        this.n8nAdapter = n8nAdapter;
        this.cubeJsAdapter = cubeJsAdapter;
        this.awsRemoteMcpProxyAdapter = awsRemoteMcpProxyAdapter;
    }

    /**
     * Returns the appropriate executor for the given tool type.
     *
     * @param toolType the type of tool backend
     * @return the matching ToolExecutor implementation
     * @throws IllegalArgumentException if the tool type is not supported
     */
    public ToolExecutor getExecutor(ToolType toolType) {
        return switch (toolType) {
            case N8N -> n8nAdapter;
            case CUBE_JS -> cubeJsAdapter;
            case AWS_REMOTE -> awsRemoteMcpProxyAdapter;
        };
    }
}
