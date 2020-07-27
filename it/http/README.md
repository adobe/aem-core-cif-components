# Integration tests

This folder contains integration tests that test all the components based on the sample content of the [CIF components library](../../examples). To execute the tests with a local AEM instance, simply setup and install the CIF components library. Make sure you also install the GraphQL client version >= `1.6.1` so you no longer have to setup HTTPS.

To execute the tests, simply run `mvn clean verify -Ptest-all`

You can also execute the tests in your favorite IDE.