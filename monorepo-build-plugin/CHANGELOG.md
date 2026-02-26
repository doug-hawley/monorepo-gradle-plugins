# Changelog

## [0.3.0](https://github.com/doug-hawley/monorepo-gradle-plugins/compare/v0.2.0...v0.3.0) (2026-02-26)


### ⚠ BREAKING CHANGES

* task names changed from printChangedProjects / buildChangedProjects to printChangedProjectsFromBranch / buildChangedProjectsFromBranch
* plugin ID, DSL block name, extension class, and Kotlin package have all changed

### Features

* add printChangedProjectsFromRef and buildChangedProjectsFromRef tasks ([a688051](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/a688051d3cccfd5a57138326b764b5b2f7962313))
* add writeChangedProjectsFromRef task for CI/CD pipeline consumption (closes [#52](https://github.com/doug-hawley/monorepo-gradle-plugins/issues/52)) ([c05cee1](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/c05cee1109bfe89468853a03d502be7d5d239449))
* reformat printChangedProjects output as annotated project hierarchy ([c381828](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/c381828a1ed976f0fd29770afa22818333e0b46f))


### Bug Fixes

* disable configuration cache in TestKit builds to prevent stale state ([6a4f753](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/6a4f75352072e672f5875d1a01dafe66690762a3))
* downgrade internal computation logs from lifecycle to info ([ffddcfb](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/ffddcfb7a574b4b5d576e84e7c38afe106b34627))
* fail fast with clear error when configuration cache is requested ([cd5a92c](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/cd5a92c374fb30a1eee2c0319024f75253d42fb1))
* map changed files to deepest matching project, not all ancestors ([f922941](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/f922941a06a167ecdd03b6b8107061d0290be4e5))
* remove getChangedProjects/getAllProjects returning non-unique simple names ([f6a7a51](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/f6a7a51501bea5bcbd3b3b791589ff4e76e17814))
* **test:** isolate inner builds from CI init scripts via GRADLE_USER_HOME override ([8e77d1c](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/8e77d1c8e973fe40fb6cc15f1a260acf0c1cf47e)), closes [#48](https://github.com/doug-hawley/monorepo-gradle-plugins/issues/48)
* **test:** strip Develocity env vars from inner builds to prevent CI init script injection ([88179cd](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/88179cd73e78b864c42fe7d25f615bd64455bb03))
* **test:** update hierarchy node tests to use renamed task and remove diagnostics ([7aecd49](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/7aecd499e2ed29c3738d6e96d4b484137057b38c))
* **test:** use --no-daemon in inner TestKit builds to prevent daemon state accumulation ([cbbfeb9](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/cbbfeb95167ea59e88504bf6f332a9535f948fb4))


### Reverts

* restore GradleRunner to simple --stacktrace-only form ([7e1d3b8](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/7e1d3b8bdf00c84926c491f62662f92fbaf3d6bd))


### Documentation

* improve README tasks section and default commitRef to HEAD~1 ([71326b4](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/71326b400b93b68d9c79f6283143421ac6b8055a))


### Miscellaneous Chores

* reset version to 0.2.0 pre-release state ([6650a65](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/6650a65f9b3279839bb776ea372f8e0485bd5816))
* **test:** add diagnostic output to hierarchy-node tests and enable showStandardStreams ([bacd000](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/bacd0005df5f18b66e0bef52879c51eccc4e12e7))
* **test:** add forwardOutput to GradleRunner for CI diagnostics ([71de853](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/71de85352e859a268332fa22da277bfd970b8278))


### Code Refactoring

* consolidate all plugin tasks into a single "monorepo" task group ([2944923](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/29449232c6d64cdaea8190c0d72a218832234092))
* extract ChangedProjectsPrinter to eliminate duplicated output logic ([1fe6d9b](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/1fe6d9baedd89422d0abbad4cdc576c0d0e47517))
* introduce GitRepository abstraction and mock-based unit tests ([91d34a4](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/91d34a459b7d503cac9153d5df7c7611be29ad46))
* promote MonorepoProjects to first-class domain object ([6ada2f4](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/6ada2f4771e56ebee364d1a4940e2901cfd09b9e))
* rename projectExcludes extension to monorepoProjectConfig ([c554d49](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/c554d49af9f7fbf46d7ae0f9bc666c7b0bb8a671))
* rename tasks to printChangedProjectsFromBranch and buildChangedProjectsFromBranch ([b02be5e](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/b02be5edef142ee430c9445073f90381d83512ff))
* reorganize classes into task/, git/, and domain/ subpackages ([07d3730](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/07d3730c46765c682ea7100b730b0dfa8025c89b))
* restructure as monorepo-build-plugin subproject within monorepo-gradle-plugins ([ffc4a38](https://github.com/doug-hawley/monorepo-gradle-plugins/commit/ffc4a385605c56969418610135bc30a303a84b1f))
