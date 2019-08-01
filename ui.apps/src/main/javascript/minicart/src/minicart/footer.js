import React from 'react';
import { Lock as LockIcon } from 'react-feather';

import classes from './footer.css';

const Footer = props => {
    const placeholderButton = () => {
        return (
            <div className={classes.placeholderButton}>
                <button>
                    Checkout <LockIcon />
                </button>
            </div>
        );
    };

    return placeholderButton();
};

export default Footer;
