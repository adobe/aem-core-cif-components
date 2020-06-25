/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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
import QUERY_CUSTOMER_ORDERS from '../../queries/query_customer_orders.graphql';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@apollo/react-hooks';
import classes from './customerOrders.css';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TablePagination from '@material-ui/core/TablePagination';
import TableRow from '@material-ui/core/TableRow';

const CustomerOrders = props => {
    const [t] = useTranslation(['account']);
    const { data: customerOrdersData } = useQuery(QUERY_CUSTOMER_ORDERS);
    const [page, setPage] = React.useState(0);
    const [rowsPerPage] = React.useState(10);

    const handleChangePage = (event, newPage) => {
        setPage(newPage);
    };

    if (customerOrdersData) {
        return (
            <Paper className={classes.root}>
                <TableContainer component={Paper}>
                    <Table className={classes.table} aria-label="simple table">
                        <TableHead>
                            <TableRow>
                                <TableCell>{t('customer-order-number', 'Order #')}</TableCell>
                                <TableCell align="right">{t('customer-order-created-at', 'Date')}</TableCell>
                                <TableCell align="right">{t('customer-order-grand-total', 'Total')}</TableCell>
                                <TableCell align="right">{t('customer-order-status', 'Status')}</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {customerOrdersData.customerOrders.items
                                .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((row) => (
                                    <TableRow key={row.order_number}>
                                        <TableCell component="th" scope="row">
                                            {row.order_number}
                                        </TableCell>
                                        <TableCell align="right">{row.created_at.substring(0, 10)}</TableCell>
                                        <TableCell align="right">{row.grand_total}</TableCell>
                                        <TableCell align="right">{row.status}</TableCell>
                                    </TableRow>
                                ))}
                        </TableBody>
                    </Table>
                </TableContainer>
                <TablePagination
                    rowsPerPageOptions={[]}
                    component="div"
                    count={customerOrdersData.customerOrders.items.length}
                    rowsPerPage={rowsPerPage}
                    page={page}
                    onChangePage={handleChangePage}
                    labelRowsPerPage={""}
                />
            </Paper>
        );
    } else {
        return "";
    }
}

export default CustomerOrders;
