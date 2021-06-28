/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
import React, { useEffect, useState } from 'react';

import classes from './ProductRecsGallery.css';
import { useStorefrontEvents } from '@adobe/aem-core-cif-react-components';
import { useStorefrontInstanceContext } from '../../context/StorefrontInstanceContext';

// TODO: Add npm link to components react components
// TODO: Update version updating for releases

const ProductRecsGallery = () => {
    const storefrontInstance = useStorefrontInstanceContext();
    const mse = useStorefrontEvents();

    if (!storefrontInstance) {
        return <div>Loading...</div>;
    }

    // TODO: Init recs SDK

    // TODO: Get rec units and render

    return <div className={classes.root}>Loaded.</div>;
};

export default ProductRecsGallery;
