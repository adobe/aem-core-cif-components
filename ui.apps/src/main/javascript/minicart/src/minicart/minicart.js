import React, { useEffect } from 'react';
import classes from './minicart.css';

import Header from './header';
import Body from './body';
import Footer from './footer';
import Mask from '../Mask';

const MiniCart = props => {
    const { isOpen, handleCloseCart } = props;
    const rootClass = isOpen ? classes.root_open : classes.root;

    console.log(`Root class is ${rootClass}`);

    return (
        <>
            <Mask isActive={isOpen} dismiss={handleCloseCart} />
            <aside className={rootClass}>
                <Header handleCloseCart={handleCloseCart} />
                <Body />
                <Footer />
            </aside>
        </>
    );
};

export default MiniCart;
