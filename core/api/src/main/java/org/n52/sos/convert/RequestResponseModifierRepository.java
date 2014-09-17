/**
 * Copyright (C) 2012-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.convert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.n52.sos.exception.ConfigurationException;
import org.n52.sos.request.AbstractServiceRequest;
import org.n52.sos.response.AbstractServiceResponse;
import org.n52.sos.util.AbstractConfiguringServiceLoaderRepository;

@SuppressWarnings("rawtypes")
public class RequestResponseModifierRepository extends
        AbstractConfiguringServiceLoaderRepository<RequestResponseModifier> {

    private static RequestResponseModifierRepository instance;

    private final Map<RequestResponseModifierKeyType, RequestResponseModifier<?, ?>> requestResponseModifier =
            new HashMap<RequestResponseModifierKeyType, RequestResponseModifier<?, ?>>(0);

    public static RequestResponseModifierRepository getInstance() {
        if (instance == null) {
            instance = new RequestResponseModifierRepository();
        }
        return instance;
    }

    public RequestResponseModifierRepository() {
        super(RequestResponseModifier.class, false);
        load(false);
    }

    @Override
    protected void processConfiguredImplementations(Set<RequestResponseModifier> requestResponseModifier)
            throws ConfigurationException {
        this.requestResponseModifier.clear();
        for (RequestResponseModifier<?, ?> aModifier : requestResponseModifier) {
            for (RequestResponseModifierKeyType modifierKeyType : aModifier.getRequestResponseModifierKeyTypes()) {
                this.requestResponseModifier.put(modifierKeyType, aModifier);
            }
        }
    }
    
    public RequestResponseModifier getRequestResponseModifier(AbstractServiceRequest request) {
        return getRequestResponseModifier(new RequestResponseModifierKeyType(request.getService(), request.getVersion(), request));
    }
    
    public RequestResponseModifier getRequestResponseModifier(AbstractServiceRequest request, AbstractServiceResponse response) {
        return getRequestResponseModifier(new RequestResponseModifierKeyType(response.getService(), response.getVersion(), request, response));
    }
    
    public <T, F>RequestResponseModifier getRequestResponseModifier(RequestResponseModifierKeyType key) {
        return (RequestResponseModifier) requestResponseModifier.get(key);
    }
    
    public boolean hasRequestResponseModifier(AbstractServiceRequest request) {
        return hasRequestResponseModifier(new RequestResponseModifierKeyType(request.getService(), request.getVersion(), request));
    }
    
    public boolean hasRequestResponseModifier(AbstractServiceRequest request, AbstractServiceResponse response) {
        return hasRequestResponseModifier(new RequestResponseModifierKeyType(request.getService(), request.getVersion(), request, response));
    }
    
    public boolean hasRequestResponseModifier(RequestResponseModifierKeyType key) {
        return requestResponseModifier.containsKey(key);
    }

}
