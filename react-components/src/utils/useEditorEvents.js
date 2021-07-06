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

import { useEffect, useState } from 'react';
import { useCookieValue } from './hooks';

/**
 * This hook listens to messages from the AEM Sites editor, when in editing mode
 * and triggers a re-render of components using the hook.
 */
const useEditorEvents = () => {
    const [, setState] = useState(0);
    const [wcmmode] = useCookieValue('wcmmode');

    const onMessage = event => {
        // Drop events from unknown origins
        if (event.origin !== window.location.origin) {
            return;
        }

        // Drop events that are not AEM Sites editor commands
        if (!event.data || !event.data.msg || event.data.msg !== 'cqauthor-cmd') {
            return;
        }

        // Skip commands that do not require a re-render
        if (event.data.data && event.data.data.cmd && event.data.data.cmd === 'toggleClass') {
            return;
        }

        // Update state to force re-render
        setState(state => state + 1);
    };

    useEffect(() => {
        // Only during editing
        if (wcmmode !== 'edit') {
            return;
        }

        window.addEventListener('message', onMessage, false);
        return () => {
            window.removeEventListener('message', onMessage, false);
        };
    }, []);
};

export default useEditorEvents;
