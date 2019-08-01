import React, { useEffect, useState } from 'react';
import Minicart from './minicart';

const Container = props => {
    const [isOpen, setIsOpen] = useState(false);

    useEffect(() => {
        document.addEventListener('aem.cif.open-cart', event => {
            setIsOpen(true);
        });
    });

    const handleCloseCart = () => {
        setIsOpen(false);
    };

    return <Minicart isOpen={isOpen} handleCloseCart={handleCloseCart} />;
};

export default Container;
