const path = require('path');
const fs = require('fs');

const DEST_PATH = '../../content/jcr_root/apps/core/cif/components/commerce/minicart/v1/minicart/clientlib/dist';
const SRC_PATH = path.resolve('.', 'dist');
console.log(SRC_PATH);
const FILE_NAME = 'main';

fs.copyFile(`${SRC_PATH}/${FILE_NAME}.js`, `${DEST_PATH}/${FILE_NAME}.js`, err => {
    if (err) {
        throw err;
    }
    console.error(`Output copied to ${DEST_PATH}`);
});
fs.copyFile(`${SRC_PATH}/${FILE_NAME}.js.map`, `${DEST_PATH}/${FILE_NAME}.js.map`, err => {
    if (err) {
        throw err;
    }
    console.error(`Output copied to ${DEST_PATH}`);
});
