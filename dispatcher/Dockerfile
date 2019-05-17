################################################################################
#
#    Copyright 2019 Adobe. All rights reserved.
#    This file is licensed to you under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License. You may obtain a copy
#    of the License at http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software distributed under
#    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
#    OF ANY KIND, either express or implied. See the License for the specific language
#    governing permissions and limitations under the License.
#
################################################################################

FROM httpd:2.4

ENV DISPATCHER_VERSION 4.3.2

# Download and unpack dispatcher
RUN mkdir -p /tmp/dispatcher
ADD --chown=root:www-data "http://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-${DISPATCHER_VERSION}.tar.gz" /tmp/dispatcher/
RUN cd /tmp/dispatcher && \
    tar -xzvf "dispatcher-apache2.4-linux-x86_64-${DISPATCHER_VERSION}.tar.gz" && \
    chown -R root:www-data *

# Install mod_dispatcher
RUN ln -s "/tmp/dispatcher/dispatcher-apache2.4-${DISPATCHER_VERSION}.so" "${HTTPD_PREFIX}/modules/mod_dispatcher.so" && \
    sed -i '/#LoadModule info_module modules\/mod_info.so/a LoadModule dispatcher_module modules\/mod_dispatcher.so' "${HTTPD_PREFIX}/conf/httpd.conf" > /dev/null && \
    sed -i '/<Directory "\/usr\/local\/apache2\/htdocs">/a <IfModule disp_apache2.c> \n\
    ModMimeUsePathInfo On \n\
    SetHandler dispatcher-handler \n\
</IfModule>' "${HTTPD_PREFIX}/conf/httpd.conf" > /dev/null

# Add httpd-dispatcher.conf
RUN cp /tmp/dispatcher/conf/httpd-dispatcher.conf "${HTTPD_PREFIX}/conf/extra/" && \
    echo 'Include conf/extra/httpd-dispatcher.conf' >> "${HTTPD_PREFIX}/conf/httpd.conf" && \
    cat "${HTTPD_PREFIX}/conf/httpd.conf"

# Add dispatcher.any
COPY conf/dispatcher.any "${HTTPD_PREFIX}/conf/"

# Add Magento reverse config
COPY conf/magento-proxy.conf "${HTTPD_PREFIX}/conf/"
RUN echo 'Include conf/magento-proxy.conf' >> "${HTTPD_PREFIX}/conf/httpd.conf"

# Go back to apache root
RUN cd $HTTPD_PREFIX

# Fix permissions for dispatcher caching folder
RUN chown daemon:www-data htdocs