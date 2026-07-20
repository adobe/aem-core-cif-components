#!/usr/bin/env bash
#
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Copyright 2026 Adobe
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
# Runs run-integration-test.sh (which installs Chrome/chromedriver for selenium specs,
# then runs it-tests.js) inside the QuickProvider (qp) client image, against an AEM server
# image started as a sibling container. Both containers use --network host
# so that "localhost" means the same thing to both - required because QuickProvider's RMI
# protocol advertises "localhost" as its own callback address (see aem-cif-guides-venia's
# .github/ci/run-containerized-test.sh, which hit and solved this exact problem first).
#
# GitHub Actions' `container:` + `services:` keys can't express this: combining
# --network host with a `services:` container crashes the runner before the job even
# starts ("Error: Value cannot be null. (Parameter 'ContainerId')"). So this script
# drives both containers directly via the Docker CLI on the bare runner instead.
#
# KEEP_AEM_CONTAINER=true skips removing the aem container on exit, so a later tmate SSH
# step in the same job can `docker exec` into it while it's still running, instead of only
# being able to inspect the runner/workspace after it's already gone.

set -euo pipefail

: "${QP_IMAGE:?QP_IMAGE must be set}"
: "${AEM_IMAGE:?AEM_IMAGE must be set}"
: "${ARTIFACTORY_CLOUD_USER:?}"
: "${ARTIFACTORY_CLOUD_PASS:?}"
: "${GITHUB_WORKSPACE:?}"

registry="${QP_IMAGE%%/*}"
echo "${ARTIFACTORY_CLOUD_PASS}" | docker login "${registry}" -u "${ARTIFACTORY_CLOUD_USER}" --password-stdin

aem_container="aem-${GITHUB_RUN_ID:-local}-${GITHUB_JOB:-job}"
echo "aem_container=${aem_container}" >> "${GITHUB_ENV:-/dev/null}"

cleanup() {
    echo "::group::aem service container logs (${aem_container})"
    docker logs "${aem_container}" || true
    echo "::endgroup::"
    if [[ "${KEEP_AEM_CONTAINER:-false}" == "true" ]]; then
        echo "KEEP_AEM_CONTAINER=true - leaving ${aem_container} running for the SSH debug"
        echo "session later in this job. From inside it: docker exec -it ${aem_container} bash"
        echo "(a later step force-removes it once that session ends)."
    else
        docker rm -f "${aem_container}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

docker pull "${AEM_IMAGE}"
docker run -d --network host --name "${aem_container}" "${AEM_IMAGE}"

docker pull "${QP_IMAGE}"
# Bind-mount the host's own ~/.m2 to the container's /root/.m2 (it runs as --user root)
# so Maven's downloads actually land on the runner's filesystem, at a path the
# actions/cache steps (which run directly on the runner, not in this container) can
# read/write. Without this mount, /root/.m2/repository only ever exists inside the
# container's own ephemeral layer - gone the instant --rm removes it - while the *host's*
# /root is the real root user's home (mode 700), which the cache steps (running as the
# unprivileged default runner user) can't access at all: "EACCES: permission denied,
# lstat '/root/.m2/repository'".
mkdir -p "${HOME}/.m2"
docker run --rm --network host --user root \
    -e AEM -e TYPE -e BROWSER \
    -e ARTIFACTORY_CLOUD_USER -e ARTIFACTORY_CLOUD_PASS \
    -e COMMERCE_ENDPOINT -e COMMERCE_INTEGRATION_TOKEN \
    -e GITHUB_WORKSPACE \
    -v "${GITHUB_WORKSPACE}:${GITHUB_WORKSPACE}" -w "${GITHUB_WORKSPACE}" \
    -v "${HOME}/.m2:/root/.m2" \
    "${QP_IMAGE}" bash .github/scripts/run-integration-test.sh
