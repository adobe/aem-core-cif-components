# DRAFT: Customizing CIF Core Components

This document describes customization of CIF Core Components on the layers of Sling models and data retrieval via GraphQL. For all other customizations, please follow the patters described in [Customizing Core Components](https://docs.adobe.com/content/help/en/experience-manager-core-components/using/developing/customizing.html).


## Use Cases

1. **Use Case A**

    Change of logic inside the component.

    *Example: Change the formatting of a product price.*

1. **Use Case B**

    Use Magento GraphQL schema to query one or more additional properties and display them.

    *Example: Display a custom product property.*

1. **Use Case C**

    Use a custom GraphQL schema and create a custom query to implement the component.

    *Example: Include a custom inventory system in your schema to display similar information as provided by the Magento schema.*

1. **Use Case D**

    Use a custom GraphQL schema and a custom query to implement and extend the component.

    *Example: Include addition data like "payment in installments" that is not present in the Magento schema or in the Sling model.*

## Customization Layers
1. Sightly Templates
1. Sling Model Interface
1. Sling Model Implementation
1. Data Layer (GraphQL Query)
1. GraphQL Schema (Model Classes)

## Use Case to Layer Mapping

| Use Case | Template | Interface | Implementation | Data Layer | Schema |
| -------- | -------- | --------- | -------------- | ---------- | ------ |
| A        | ✔️       | ✔️        | ❌            | ✔️          | ✔️    |
| B        | ❌       | ❌        | ❌            | ❌          | ✔️    |
| C        | ✔️       | ✔️        | ❌            | ❌          | ❌    |
| D        | ❌       | ❌        | ❌            | ❌          | ❌    |

✔️ no adaption needed, ❌adaption needed
