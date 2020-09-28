/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import Icon from '../Icon';
import { X as CloseIcon } from 'react-feather';
import { string, shape, func, bool, object } from 'prop-types';
import { Form } from 'informed';

import { Portal } from '../Portal';
import Button from '../Button';

import classes from './dialog.css';

const Dialog = props => {
    const {
        cancelText,
        children,
        confirmText,
        formProps,
        isModal,
        isOpen,
        onCancel,
        onConfirm,
        shouldDisableAllButtons,
        shouldDisableConfirmButton,
        title,
        rootContainerSelector
    } = props;

    const rootClass = isOpen ? classes.root_open : classes.root;
    const isMaskDisabled = shouldDisableAllButtons || isModal;
    const confirmButtonDisabled = shouldDisableAllButtons || shouldDisableConfirmButton;

    const confirmButtonClasses = {
        root_highPriority: classes.confirmButton
    };

    const maybeCloseXButton = !isModal ? (
        <button className={classes.headerButton} disabled={shouldDisableAllButtons} onClick={onCancel} type="reset">
            <Icon src={CloseIcon} />
        </button>
    ) : null;

    return (
        <Portal selector={rootContainerSelector}>
            <aside className={rootClass}>
                <Form className={classes.form} onSubmit={onConfirm} {...formProps}>
                    {/* The Mask. */}
                    <button className={classes.mask} disabled={isMaskDisabled} onClick={onCancel} type="reset" />
                    {/* The Dialog. */}
                    <div className={classes.dialog}>
                        <div className={classes.header}>
                            <span className={classes.headerText}>{title}</span>
                            {maybeCloseXButton}
                        </div>
                        <div className={classes.body}>
                            <div className={classes.contents}>{children}</div>
                            <div className={classes.buttons}>
                                <Button
                                    disabled={shouldDisableAllButtons}
                                    onClick={onCancel}
                                    priority="low"
                                    type="reset">
                                    {cancelText}
                                </Button>
                                <Button
                                    classes={confirmButtonClasses}
                                    disabled={shouldDisableAllButtons || confirmButtonDisabled}
                                    priority="high"
                                    type="submit">
                                    {confirmText}
                                </Button>
                            </div>
                        </div>
                    </div>
                </Form>
            </aside>
        </Portal>
    );
};

export default Dialog;

Dialog.propTypes = {
    cancelText: string,
    classes: shape({
        body: string,
        cancelButton: string,
        confirmButton: string,
        container: string,
        contents: string,
        header: string,
        headerText: string,
        headerButton: string,
        mask: string,
        root: string,
        root_open: string
    }),
    confirmText: string,
    formProps: object,
    rootContainerSelector: string,
    isModal: bool,
    isOpen: bool,
    onCancel: func,
    onConfirm: func,
    shouldDisableAllButtons: bool,
    shouldDisableConfirmButton: bool,
    title: string
};

Dialog.defaultProps = {
    cancelText: 'Cancel',
    confirmText: 'Confirm',
    isModal: false
};
