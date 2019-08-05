import React from 'react';
import { X as CloseIcon } from 'react-feather';

import classes from './header.css';

const Header = props => {
    const { handleCloseCart } = props;
    const title = 'Shopping Cart';
    return (
        <div className={classes.root}>
            <h2 className={classes.title}>{title}</h2>
            <button onClick={handleCloseCart}>
                <CloseIcon />
            </button>
        </div>
    );
};

export default Header;
