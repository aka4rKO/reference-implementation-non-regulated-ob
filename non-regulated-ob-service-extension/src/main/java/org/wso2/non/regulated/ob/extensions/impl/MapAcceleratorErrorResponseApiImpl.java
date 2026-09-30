/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com). All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.non.regulated.ob.extensions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.non.regulated.ob.extensions.model.Error;
import org.wso2.non.regulated.ob.extensions.model.ErrorMapperData;
import org.wso2.non.regulated.ob.extensions.model.ErrorMapperRequestBody;
import org.wso2.non.regulated.ob.extensions.utils.ErrorMappingUtil;

import javax.ws.rs.core.Response;

/**
 * Routes an accelerator error to the rules that rewrite it into the published error format.
 */
public class MapAcceleratorErrorResponseApiImpl {

    private static final Log log = LogFactory.getLog(MapAcceleratorErrorResponseApiImpl.class);

    /**
     * Maps an error raised by the accelerator to the format the APIs publish.
     *
     * @param errorMapperRequestBody the accelerator's request, carrying the error
     * @return the mapped error response
     */
    public static Response handleErrorMapping(ErrorMapperRequestBody errorMapperRequestBody) {

        String requestId = errorMapperRequestBody.getRequestId();
        ErrorMapperData data = errorMapperRequestBody.getData();
        Error error = data.getError();

        log.error("Mapping accelerator error for operation: " + (error == null ? null : error.getOperation()));

        return Response.status(Response.Status.OK)
                .entity(ErrorMappingUtil.mapAcceleratorError(error, requestId).toString())
                .build();
    }

}
