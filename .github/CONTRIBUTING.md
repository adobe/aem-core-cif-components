# Contributing

Thanks for choosing to contribute!

The following are a set of guidelines to follow when contributing to this project.

## Code Of Conduct

This project adheres to the Adobe [code of conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to the team.

## Contributor License Agreement

All third-party contributions to this project must be accompanied by a signed contributor license agreement. This gives Adobe permission to redistribute your contributions as part of the project. [Sign our CLA](http://opensource.adobe.com/cla.html). You only need to submit an Adobe CLA one time, so if you have submitted one previously, you are good to go!

## How to contribute

New code contributions should be made primarily using GitHub pull requests. This involves creating a fork of the project in your personal space, adding your new code in a branch and triggering a pull request.

See how to perform pull requests at https://help.github.com/articles/using-pull-requests.

Please follow the [pull request template](PULL_REQUEST_TEMPLATE.md) when submitting a pull request!

To ease the review process of pull requests, we ask you to please follow a number of guidelines and recommendations. This will speed up the review process and ensure that a pull request does not break any existing functionality.
* **Keep it small!** Reviewing a large pull request is very difficult and time consuming. Try to keep your contribution small, maybe to a maximum of a dozen files with maximum a few hundred lines of code in total. Do not combine multiple new features or bug fixes into one single pull request: if one PR needs another one, simply create multiple PRs, and open "nested PRs" that depend on eachother.
* **Do not remove tests!** If your feature breaks a test, do NOT remove that test, unless there is a very good reason that the test is no longer needed. If there is a test for something, there is usually a good reason for that. If you break a test, make sure that you fix the test, but make sure that the original feature still does what it is expected to do!
* **Add your own tests!**: We will not consider pull requests that do not include a minimum of 80% test coverage. Make sure that your tests follow the same design and format than similar tests, and be consistent with our existing tests.
* **Do not "pollute" your pull request!**: Avoid unneeded changes in your pull request, for example, code formatting changes or changes not related to your feature or bug fix. Make sure that your IDE is configured to **not** reformat the entire files you are editing, but ony the lines you change. This will ensure that different formatting rules will not affect code that you do not change!
* **Follow master!**: Make sure that your pull request is up-to-date with respect to our `master` branch. It is your responsibility to ensure that the latest commits in our `master` branch are always merged into your code, and that merge conflicts are resolved. Please make sure that your pull request follows our recommendations, in order to speed up the review process and hopefully reduce the number of times you will have to merge our latest changes into your branch.
* **Format your code!**: Our maven build can automatically format java files, make sure you do that. For other file formats like `.js`, `.xml` and `.html`, make sure that you use a 4-space indentation, do not use tabs. For `.json` files, we use a 2-space indentation.
* **Use common sense!**: Use common sense to increase the quality of your contribution. Do not duplicate code, use constants instead of hard-coded strings where appropriate, add javadoc documentation, use comments (sparingly!) where the code could be difficult to understand, and keep in mind that the CIF components can be reused and extended by others so make sure the code is readable and follows the latest AEM development guidelines.

## New Feature request
Please follow the [feature template](ISSUE_TEMPLATE/FEATURE_REQUEST.md) to open new feature requests. 


## Issues

Please follow the [issue template](ISSUE_TEMPLATE/BUG_REPORT.md) to open new [issues](https://github.com/adobe/aem-core-cif-components/issues) and join the conversations to provide feedback. 
