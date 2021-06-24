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
import React from 'react';
import { useQuery } from '@apollo/client';

import QUERY_STOREFRONT_INSTANCE_CONTEXT from '../../queries/query_storefront_instance_context.graphql';

import classes from './ProductRecsGallery.css';

const ProductRecsGallery = () => {
    const { loading, data, error } = useQuery(QUERY_STOREFRONT_INSTANCE_CONTEXT);

    if (loading) {
        return <div>Loading...</div>;
    }

    if (error) {
        console.error(error);
        return <div>Error</div>;
    }

    console.log('data', data);
    return <div className={classes.root}>Loaded</div>;
};

export default ProductRecsGallery;
