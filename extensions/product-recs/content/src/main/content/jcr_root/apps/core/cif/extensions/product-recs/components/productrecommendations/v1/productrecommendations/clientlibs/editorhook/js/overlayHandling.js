/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~ http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

(function(ns, $, channel) {
    function removeOverlayInContentFrame(e) {
        const productCount = (e.detail || []).length;
        if (productCount > 0) {
            // remove overlay el
            const overlay = e.target.parentElement.querySelector('.cq-placeholder');
            if (overlay) {
                overlay.remove();
            }
            // get component path
            const cq = e.target.parentElement.querySelector('[data-path]');
            const { path } = cq ? cq.dataset : {};

            new ns.MessageChannel('cqauthor', window.parent).postMessage('cif-destroy-overlay', path, -1);
        }
    }

    function removeOverlayInEditorFrame(m) {
        const path = m.data;
        for (let editable of ns.editables.find(path)) {
            Granite.author.overlayManager.recreate(editable);
        }
    }

    channel.on('cq-content-frame-loaded', function() {
        ns.ContentFrame.contentWindow.document.addEventListener(
            'aem.cif.product-recs-loaded',
            removeOverlayInContentFrame
        );
        if (ns.ContentFrame.messageChannel) {
            ns.ContentFrame.messageChannel.subscribeRequestMessage('cif-destroy-overlay', removeOverlayInEditorFrame);
        } else if (ns.ContentFrame.subscribeRequestMessage) {
            // support for 6.5
            ns.ContentFrame.subscribeRequestMessage('cif-destroy-overlay', removeOverlayInEditorFrame);
        }
    });
})(Granite.author, jQuery, jQuery(document));
