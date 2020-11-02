# CIF Core Components Historical System Requirements

See below for a full list of minimum system requirements for historical versions of the CIF Core Components:

| CIF Core Components | AEM as a Cloud Service | AEM 6.4 | AEM 6.5 | Magento            | Java |
| ------------------- | ---------------------- | ------- | ------- | ------------------ | ---- |
| 1.5.0               | Continual              | 6.4.4.0 | 6.5.7   | 2.4.0              | 1.8  |
| 1.4.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.4.0              | 1.8  |
| 1.3.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.5              | 1.8  |
| 1.2.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.5              | 1.8  |
| 1.1.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.4 / 2.3.5      | 1.8  |
| 1.0.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.4 / 2.3.5      | 1.8  |
| 0.10.1              | Continual              | 6.4.4.0 | 6.5.0   | 2.3.4              | 1.8  |
| 0.10.0              | Continual              | 6.4.4.0 | 6.5.0   | 2.3.4              | 1.8  |
| 0.9.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.4              | 1.8  |
| 0.7.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.3              | 1.8  |
| 0.6.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.2<sup>\*</sup> / 2.3.3 | 1.8  |
| 0.5.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.2<sup>\*</sup> / 2.3.3 | 1.8  |
| 0.4.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.2<sup>\*</sup> | 1.8  |
| 0.3.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.1 / 2.3.2      | 1.8  |
| 0.2.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.1 / 2.3.2      | 1.8  |
| 0.1.0               | Continual              | 6.4.4.0 | 6.5.0   | 2.3.1              | 1.8. |

<sup>\*</sup> With version 0.4.0 we drop support for Magento 2.3.1 and only support 2.3.2. For version 2.3.2 we currently require the following 4 Magento GraphQL patches ([#758](https://github.com/magento/graphql-ce/issues/758), [#665](https://github.com/magento/graphql-ce/pull/665), [#666](https://github.com/magento/graphql-ce/pull/666), [#906](https://github.com/magento/graphql-ce/pull/906)) installed on the Magento instance. Most of them will be included in upcomming Magento 2.3.3. release.
