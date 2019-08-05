import React from 'react';
import { Lock as LockIcon } from 'react-feather';

import Button from '../Button';
import classes from './footer.css';

const Footer = props => {
    const placeholderButton = () => {
        return (
            <div className={classes.placeholderButton}>
                <Button priority="high">
                    Checkout <LockIcon />
                </Button>
            </div>
        );
    };

    return placeholderButton();
};

export default Footer;
