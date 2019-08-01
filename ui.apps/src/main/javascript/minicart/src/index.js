import React from 'react';
import ReactDOM from 'react-dom';

import Cart from './minicart';

window.onload = function() {
    const element = document.getElementById('minicart');
    console.log(element);
    if (!element) {
        console.log('We have no container for this, wtf?');
    } else {
        ReactDOM.render(<Cart />, element);
    }
};
