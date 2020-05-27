/*******************************************************************************
 *
 *    Sagepath custom cif component
 *
 ******************************************************************************/
import React, { useContext, useState, useCallback, useEffect } from 'react';

import { useUserContext } from '../../context/UserContext';
import { func } from 'prop-types';
import Button from '../Button';
import { useCookieValue } from '../../utils/hooks';

import classes from './orderHistory.css';

const OrderHistory = props => {
    const { showMyAccount } = props;
    const [{ currentUser, customerOrders }] = useUserContext();

    return (
        <div className={classes.root}>
            <p>Here are your orders.</p>
            <div>
                <Button priority="high" onClick={showMyAccount}>
                    {'Back'}
                </Button>
            </div>
            {customerOrders.items.map(item => (
                <div key={item.id}>
                    <p className={classes.orderItem}>Order #{item.order_number}</p>
                    <p className={classes.orderItem}>Order Placed: {item.created_at}</p>
                    <p className={classes.orderItem}>Order Total: ${item.grand_total}</p>
                    <p className={classes.orderItem}>Status: {item.status}</p>
                </div>
            ))}
        </div>
    );

}

OrderHistory.propTypes = {
    showMyAccount: func.isRequired
};

export default OrderHistory;