/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
'use strict';

const e = require('child_process');
const fs = require('fs');
const path = require('path');

module.exports = class CI {

    /**
     * Print build context to stdout.
     */
    context() {
        this.sh('java -version');
        this.sh('mvn -v');
        console.log("Node version: %s", process.version);
        this.sh('printf "NPM version: $(npm --version)"', false, false);
    };

    /**
     * Switch working directory for the scope of the given function.
     */
    dir(dir, func) {
        let currentDir = process.cwd();
        process.chdir(dir);
        console.log('// Changed directory to: ' + process.cwd());
        try {
            func();
        } finally {
            process.chdir(currentDir);
            console.log('// Changed directory back to: ' + currentDir);
        }
    };

    /**
     * Checkout git repository with the given branch into the given folder.
     */
    checkout(repo, branch = 'master', folder = '') {
        this.sh('git clone -b ' + branch + ' ' + repo + ' ' + folder);
    };

    /**
     * Run shell command and attach to process stdio.
     */
    sh(command, returnStdout = false, print = true) {
        if (print) {
            console.log(command);
        }
        if (returnStdout) {
            return e.execSync(command).toString().trim();
        }
        return e.execSync(command, {stdio: 'inherit'});
    };

    /**
     * Return value of given environment variable.
     */
    env(key) {
        return process.env[key];
    }

    /**
     * Print stage name.
     */
    stage(name) {
        console.log("\n------------------------------\n" +
            "--\n" +
            "-- %s\n" +
            "--\n" +
            "------------------------------\n", name);
    };

    /**
     * Configure a git impersonation for the scope of the given function.
     */
    gitImpersonate(user, mail, func) {
        try {
            this.sh('git config --local user.name ' + user + ' && git config --local user.email ' + mail, false, false);
            func()
        } finally {
            this.sh('git config --local --unset user.name && git config --local --unset user.email', false, false);
        }
    };

    /**
     * Configure git credentials for the scope of the given function.
     */
    gitCredentials(repo, func) {
        try {
            this.sh('git config credential.helper \'store --file .git-credentials\'');
            fs.writeFileSync('.git-credentials', repo);
            console.log('// Created file .git-credentials.');
            func()
        } finally {
            this.sh('git config --unset credential.helper');
            fs.unlinkSync('.git-credentials');
            console.log('// Deleted file .git-credentials.');
        }
    };

    /**
     * Writes given content to a file.
     */
    writeFile(fileName, content) {
        console.log(`// Write to file ${fileName}`);
        fs.writeFileSync(fileName, content, { 'encoding': 'utf8' });
    }

    collectConfiguration() {
        let configuration = {
            modules: {}
        };
        
        let folders = [process.cwd()];
        process.stdout.write("Collecting project configuration");
        while(folders.length) {
            let folder = folders.shift();
            let files = fs.readdirSync(folder, { withFileTypes: true });
        
            for(let file of files) {
                if (file.isDirectory()) {
                    folders.push(path.resolve(folder, file.name));
                    continue;
                }
        
                if (file.name !== 'pom.xml') {
                    continue;
                }
        
                let pomPath = path.resolve(folder, file.name);
                let metaData = this.sh('printf \'${project.groupId}|${project.artifactId}|${project.name}|${project.version}|${project.packaging}\' | mvn -f ' + pomPath + ' help:evaluate --non-recursive | grep -Ev "(Download|\\[)"', true, false).split('|');
                configuration.modules[metaData[1]] = {
                    groupId: metaData[0],
                    artifactId: metaData[1],
                    name: metaData[2],
                    version: metaData[3],
                    packaging: metaData[4],
                    path: folder
                };
                process.stdout.write('.');
            }
        }
        process.stdout.write(require('os').EOL);
        fs.writeFileSync('configuration.json', JSON.stringify(configuration, null, 4));

        return configuration;
    }

    restoreConfiguration() {
        let configuration = fs.readFileSync('configuration.json');
        return JSON.parse(configuration);
    }

    addQpFileDependency(module) {
        let output = '--install-file ';

        let filename = `${module.artifactId}-${module.version}`;
        if (module.packaging == 'content-package') { 
            filename += '.zip';
        } else if (module.packaging == 'bundle') {
            filename += '.jar';
        }

        output += path.resolve(module.path, 'target', filename);

        return output;
    }

    parsePom() {
        const metaData = this.sh('printf \'${project.groupId}|${project.artifactId}|${project.name}|${project.version}|${project.packaging}\' | mvn help:evaluate --non-recursive | grep -Ev "(Download|\\[)"', true, false).split('|');
        return {
            groupId: metaData[0],
            artifactId: metaData[1],
            name: metaData[2],
            version: metaData[3],
            packaging: metaData[4]
        };
    }

};