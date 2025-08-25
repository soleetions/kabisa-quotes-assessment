# Quotes service

This service can be used to get random quotes.<br/>
The source of the quotes is the Dummy JSON API (https://dummyjson.com/quotes).

The service has the following functionalities:

- Get a random quote
- Vote for a quote
- Get the top 10 voted quotes

There is an in-memory repository to cache the returned quotes and store the votes.<br/>
This repository is also used as a fallback if the Dummy JSON API is not reachable.

To interact with the service, you can use either a REST API or a GraphQL API.

## Endpoints

### Rest API

- `GET /api/rest/quotes/random`: Get a random quote
- `POST /api/rest/quotes/vote`: Vote for a quote
- `GET /api/rest/quotes/ranking`: Get the top 10 voted quotes

### GraphQL API

- `POST /api/graphql`: GraphQL endpoint

## Documentation

The service is documented using Swagger and GraphQL Playground.<br/>
These pages can also be used to interact with the service.

- Swagger UI: `http://localhost:8080/docs/rest/swagger-ui.html`
- API Docs: `http://localhost:8080/docs/rest/api-docs`
- GraphQL Playground: `http://localhost:8080/docs/graphiql?path=/api/graphql`

## Testing

The `quotes-core` module contains SpringBootTest integration tests to test the core functionality of the service.

The `quotes-rest` module contains Cucumber tests to test the REST API and GraphQL API.

To run the Cucumber tests, you can use the following command:

```
./mvnw clean test -pl quotes-test
```

Note: The Cucumber tests require the service to be running on `http://localhost:8080` and a running internet connection to reach the Dummy JSON API.<br/>

Both test sets can also be run in an IDE like IntelliJ.
