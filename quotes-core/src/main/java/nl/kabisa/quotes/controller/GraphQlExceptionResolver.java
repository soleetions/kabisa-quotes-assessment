package nl.kabisa.quotes.controller;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import nl.kabisa.quotes.exception.ResourceNotFoundException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Custom GraphQL exception resolver to map exceptions to GraphQL errors.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        var errorBuilder = GraphqlErrorBuilder.newError()
            .message(ex.getMessage())
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation());

        if (ex instanceof ResourceNotFoundException) {
            errorBuilder.errorType(ErrorType.NOT_FOUND);
        }
        return errorBuilder.build();
    }
}

