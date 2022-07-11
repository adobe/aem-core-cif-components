/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
'use strict';

import Gallery from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v3/product/clientlib/js/gallery.js';

describe('Product', () => {
    describe('Gallery', () => {
        let galleryRoot;
        let pageRoot;
        let body;
        let galleryItems = [
            {
                path: 'http://hostname.tld/image-a.jpg',
                label: 'Image A'
            },
            {
                path: 'http://hostname.tld/image-b.jpg',
                label: 'Image B'
            }
        ];

        before(() => {
            body = document.querySelector('body');
            pageRoot = document.createElement('div');
            body.appendChild(pageRoot);
        });

        beforeEach(() => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-gallery-role="galleryroot">
                    <img src="" data-gallery-role="currentimage" />
                    <div class="thumbnailList__root">
                        <button data-gallery-role="galleryitem" data-gallery-index="0"></button>
                        <button data-gallery-role="galleryitem" data-gallery-index="1"></button>
                    </div>
                    <button data-gallery-role="moveleft"></button>
                    <button data-gallery-role="moveright"></button>
                </div>`
            );

            galleryRoot = pageRoot.querySelector(Gallery.selectors.galleryRoot);
        });

        after(() => {
            pageRoot.parentNode.removeChild(pageRoot);
        });

        it('initializes a Gallery component', () => {
            let gallery = new Gallery({ galleryItems });

            assert.equal(gallery._rootNode, galleryRoot);
            // First thumbnail is selected
            let first = galleryRoot.querySelector(Gallery.selectors.galleryThumbnail);
            assert.isTrue(first.classList.contains('thumbnail__rootSelected'));
        });

        it('initializes an empty Gallery component', () => {
            let gallery = new Gallery({});

            assert.equal(gallery._galleryItems.length, 0);
        });

        it('updates gallery on variant change', () => {
            let gallery = new Gallery({ galleryItems });

            let newGalleryItems = [
                {
                    path: 'http://hostname.tld/image-c.jpg',
                    label: 'Image C'
                },
                {
                    path: 'http://hostname.tld/image-d.jpg',
                    label: 'Image D'
                }
            ];

            // Send event
            let changeEvent = new CustomEvent(Gallery.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: {
                        assets: newGalleryItems
                    }
                }
            });
            galleryRoot.dispatchEvent(changeEvent);

            assert.deepEqual(gallery._galleryItems, newGalleryItems);

            // Check DOM
            let thumbnails = galleryRoot.querySelectorAll(Gallery.selectors.galleryThumbnail);
            assert.equal(thumbnails.length, 2);

            let [first, second] = thumbnails;
            assert.equal(first.querySelector('img').src, newGalleryItems[0].path);
            assert.equal(first.querySelector('img').alt, newGalleryItems[0].label);
            assert.equal(second.querySelector('img').src, newGalleryItems[1].path);
            assert.equal(second.querySelector('img').alt, newGalleryItems[1].label);
        });

        it('switches to a new image using a click', () => {
            new Gallery({ galleryItems });
            let [first, second] = galleryRoot.querySelectorAll(Gallery.selectors.galleryThumbnail);

            second.click();

            assert.isTrue(second.classList.contains('thumbnail__rootSelected'));
            assert.isFalse(first.classList.contains('thumbnail__rootSelected'));

            let imageContainer = galleryRoot.querySelector(Gallery.selectors.currentImageContainer);
            assert.equal(imageContainer.src, galleryItems[1].path);
        });

        it('switches to a new image using the left arrow', () => {
            new Gallery({ galleryItems });
            let [first, second] = galleryRoot.querySelectorAll(Gallery.selectors.galleryThumbnail);

            let leftArrow = galleryRoot.querySelector(Gallery.selectors.leftArrow);

            leftArrow.click();
            assert.isTrue(second.classList.contains('thumbnail__rootSelected'));
            assert.isFalse(first.classList.contains('thumbnail__rootSelected'));

            let imageContainer = galleryRoot.querySelector(Gallery.selectors.currentImageContainer);
            assert.equal(imageContainer.src, galleryItems[1].path);

            // Click again to select first element again
            leftArrow.click();
            assert.isTrue(first.classList.contains('thumbnail__rootSelected'));
            assert.isFalse(second.classList.contains('thumbnail__rootSelected'));
            assert.equal(imageContainer.src, galleryItems[0].path);
        });

        it('switches to a new image using the right arrow', () => {
            new Gallery({ galleryItems });
            let [first, second] = galleryRoot.querySelectorAll(Gallery.selectors.galleryThumbnail);

            let rightArrow = galleryRoot.querySelector(Gallery.selectors.rightArrow);

            rightArrow.click();
            assert.isTrue(second.classList.contains('thumbnail__rootSelected'));
            assert.isFalse(first.classList.contains('thumbnail__rootSelected'));

            let imageContainer = galleryRoot.querySelector(Gallery.selectors.currentImageContainer);
            assert.equal(imageContainer.src, galleryItems[1].path);

            // Click again to select first element again
            rightArrow.click();
            assert.isTrue(first.classList.contains('thumbnail__rootSelected'));
            assert.isFalse(second.classList.contains('thumbnail__rootSelected'));
            assert.equal(imageContainer.src, galleryItems[0].path);
        });
    });
});
