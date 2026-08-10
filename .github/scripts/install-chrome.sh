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
# Ensures a matched Chrome + chromedriver pair exists as system binaries inside the qp
# container before running selenium specs. The qp image already ships google-chrome
# but not chromedriver, so in practice only chromedriver gets installed (matched to the
# image's own Chrome version); the Chrome apt install runs only if a future image drops
# it. wdio.conf.local.js ("DO NOT MODIFY") resolves chromedriver via
# `command -v chromedriver`, not an npm module, so this - not adding a chromedriver
# npm dependency - is the actual fix.
#
# Ported from aem-cif-guides-venia's .github/ci/install-chrome.sh, which hit and
# solved this exact problem first.

set -euo pipefail

# Only install Chrome if the image doesn't already ship it. The qp image already carries
# a google-chrome, so an apt-get install here would just upgrade it - wasting a ~134 MB
# download to replace a browser that already works. Honor whatever Chrome is present and
# only match chromedriver to it below. This block runs only if a future image drops Chrome.
if ! command -v google-chrome >/dev/null 2>&1; then
    if command -v apt-get >/dev/null 2>&1; then
        # The base image may already have an unsigned google-chrome apt source configured,
        # which makes apt-get update fail before we get a chance to install the signing key
        # below (which overwrites that source with a properly signed one).
        sudo apt-get update || true
        command -v wget >/dev/null 2>&1 || sudo apt-get install -y wget
        command -v gpg >/dev/null 2>&1 || sudo apt-get install -y gnupg
        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg
        echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list
        sudo apt-get update
        sudo apt-get install -y google-chrome-stable
    fi
fi

# Install a chromedriver matching whatever Chrome is present - the image's own version if
# the block above was skipped, or the freshly installed stable otherwise. The qp image
# ships Chrome but not chromedriver, so this is normally the only part that actually runs.
if ! command -v chromedriver >/dev/null 2>&1; then
    chrome_version="$(google-chrome --version | awk '{print $3}' | cut -d. -f1)"
    driver_version="$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${chrome_version}")"
    curl -sSL "https://storage.googleapis.com/chrome-for-testing-public/${driver_version}/linux64/chromedriver-linux64.zip" -o /tmp/chromedriver.zip
    sudo unzip -o /tmp/chromedriver.zip -d /tmp
    sudo mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver
    sudo chmod +x /usr/local/bin/chromedriver
fi

# run-containerized-test.sh runs this container as root (needed to write into the
# bind-mounted workspace), but Chrome's sandbox refuses to run as root, and
# wdio.conf.local.js ("DO NOT MODIFY") never passes --no-sandbox. Without it every
# headless session fails instantly with "DevToolsActivePort file doesn't exist",
# which fails every single spec.
#
# Wrapping only the "google-chrome"/"google-chrome-stable" launcher script is NOT
# enough: selenium-standalone's chromedriver launches the real leaf binary
# (/opt/google/chrome/chrome) directly, bypassing a wrapper placed on the launcher.
# So wrap every Chrome entry point chromedriver might exec -- the launcher script(s)
# AND the underlying leaf binary -- idempotently, so the flags apply no matter which
# path is invoked. (A double --no-sandbox from chained wrappers is harmless.)
wrap_with_no_sandbox() {
    local target="$1"
    [[ -n "${target}" && -f "${target}" ]] || return 0
    # Idempotency guard: presence of the ".real" backup means we already wrapped it.
    # (Do NOT grep the target for "--no-sandbox" -- Chrome's leaf binary embeds that
    # switch name as a string, which would be a false positive and skip the binary
    # that actually needs wrapping.)
    [[ -f "${target}.real" ]] && return 0
    sudo cp "${target}" "${target}.real"
    sudo tee "${target}" >/dev/null <<WRAPPER
#!/usr/bin/env bash
exec "${target}.real" --no-sandbox --disable-dev-shm-usage "\$@"
WRAPPER
    sudo chmod +x "${target}"
}

if [[ "$(id -u)" -eq 0 ]]; then
    chrome_launcher="$(readlink -f "$(command -v google-chrome-stable 2>/dev/null || command -v google-chrome)")"
    wrap_with_no_sandbox "${chrome_launcher}"
    # The launcher script exec's this sibling leaf binary; chromedriver may also
    # launch it directly. This is the one that actually needs --no-sandbox as root.
    wrap_with_no_sandbox "$(dirname "${chrome_launcher}")/chrome"
fi

google-chrome --version
chromedriver --version
