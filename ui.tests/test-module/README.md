Sample Tests Project
====================

Sample UI tests using [Webdriver.IO](https://webdriver.io/) framework


* `lib/config.js` Builds configuration object from Environment variables sent by EaaS
* `package.json` Project definition: dependencies, npm scripts, ...
* `wdio.conf.js` WDIO configuration: selenium endpoint, reporters, browser capabilities, ...
* `specs/venia` Tests


* [Node.js LTS](https://nodejs.org/en/)


```
npm install
```



* AEM instance (example: `http://localhost:4502`)

  > For local testing we suggest to use the [AEM as a Cloud Service SDK](https://docs.adobe.com/content/help/en/experience-manager-cloud-service/implementing/developing/aem-as-a-cloud-service-sdk.html)

* Chrome or Firefox browser installed locally in default location


* Chrome
  ```
  npm run test-local-chrome
  ```

* Firefox
  ```
  npm run test-local-firefox
  ```

After execution, reports and logs are available in `reports` folder
