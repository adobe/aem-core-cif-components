import React from 'react';

import classes from './emptyMiniCartBody.css';

const EmptyMinicartBody = props => {
    return (
        <div className={classes.root}>
            <h3 className={classes.emptyTitle}>There are no items in your shopping cart</h3>
        </div>
    );
};

export default EmptyMinicartBody;
