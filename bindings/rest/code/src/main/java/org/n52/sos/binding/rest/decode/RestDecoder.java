/*
 * Copyright (C) 2012-2017 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.binding.rest.decode;

import static org.n52.svalbard.util.CodingHelper.decoderKeysForElements;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.janmayen.http.HTTPHeaders;
import org.n52.janmayen.http.MediaType;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.sos.binding.rest.Constants;
import org.n52.sos.binding.rest.RestBinding;
import org.n52.sos.binding.rest.requests.RestRequest;
import org.n52.sos.binding.rest.resources.ServiceEndpointDecoder;
import org.n52.sos.binding.rest.resources.capabilities.CapabilitiesDecoder;
import org.n52.sos.binding.rest.resources.features.FeaturesDecoder;
import org.n52.sos.binding.rest.resources.observations.ObservationsDecoder;
import org.n52.sos.binding.rest.resources.offerings.OfferingsDecoder;
import org.n52.sos.binding.rest.resources.sensors.SensorsDecoder;
import org.n52.svalbard.decode.AbstractXmlDecoder;
import org.n52.svalbard.decode.Decoder;
import org.n52.svalbard.decode.DecoderKey;
import org.n52.svalbard.decode.exception.DecodingException;

import com.google.common.base.Joiner;

/**
 * @author <a href="mailto:e.h.juerrens@52north.org">Eike Hinderk J&uuml;rrens</a>
 * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
 *
 */
public class RestDecoder extends AbstractXmlDecoder<HttpServletRequest, RestRequest> implements Decoder<RestRequest, HttpServletRequest>, Constructable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestDecoder.class);

    private Set<DecoderKey> decoderKeys;
    private RestConstants constants;

    @Inject
    public void setConstants(Constants constants) {
        this.constants = constants;
    }

    @Override
    public void init() {
        this.decoderKeys =  decoderKeysForElements(this.constants.getEncodingNamespace(),
                                                   HttpServletRequest.class);
        LOGGER.debug("Decoder for the following keys initialized successfully: {}!",
                    Joiner.on(", ").join(decoderKeys));
    }


    @Override
    public RestRequest decode(final HttpServletRequest httpRequest) throws DecodingException {

        // check requested content type
        if (!isAcceptHeaderOk(httpRequest))
        {
            throw new DecodingException(
                    httpRequest.getContentType(),
                    bindingConstants().getContentTypeDefault().toString());
        }

        // get decoder for method
        final ResourceDecoder decoder = getDecoderForResource(getResourceTypeFromPathInfoWithWorkingUrl(httpRequest.getPathInfo()));
        LOGGER.debug("Decoder found {}", decoder.getClass().getName());

        return decoder.decodeRestRequest(httpRequest);
    }

    private boolean isAcceptHeaderOk(final HttpServletRequest httpRequest) {
        List<MediaType> request = HTTPHeaders.getAcceptHeader(httpRequest);
        for (MediaType mt : request) {
            if (bindingConstants().getContentTypeDefault().isCompatible(mt.withoutQuality())) {
                return true;
            }
        }
        return false;
    }

    protected String getResourceTypeFromPathInfoWithWorkingUrl(String pathInfo)
    {
        /*
         * http:// workaround - Tomcat servlet container removes one "/",
         * if HttpRequest.getPathInfo() contains a second "http://"
         */
        if (pathInfo != null) {
            pathInfo = pathInfo.replaceAll("http:/", "http://");
            // use part from second slash "/" till end
            final int indexOfPotentialSecondSlash = pathInfo.indexOf("/", 1);

            if (indexOfPotentialSecondSlash > 1) {
                return pathInfo.substring(indexOfPotentialSecondSlash + 1);
            } else {
                return pathInfo.substring(1);
            }
        }
        return pathInfo;
    }

    private ResourceDecoder getDecoderForResource(
            final String httpRequestPathInfo) throws DecodingException {
        if (isSensorsRequest(httpRequestPathInfo)) {
            return new SensorsDecoder(constants);
        } else if (isObservationsRequest(httpRequestPathInfo)) {
            return new ObservationsDecoder(constants);
        } else if (isCapabilitiesRequest(httpRequestPathInfo)) {
            return new CapabilitiesDecoder(constants);
        } else if (isOfferingsRequest(httpRequestPathInfo)) {
            return new OfferingsDecoder(constants);
        } else if (isFeaturesRequest(httpRequestPathInfo)) {
            return new FeaturesDecoder(constants);
        } else if (isServiceDefaultEndpoint(httpRequestPathInfo)) {
            return new ServiceEndpointDecoder(constants);
        }
        final String exceptionText = String
                .format("Requested resource type \"%s\" is not supported by this decoder \"%s\"!",
                        httpRequestPathInfo,
                        this.getClass().getName());
        LOGGER.debug(exceptionText);
        throw new DecodingException(httpRequestPathInfo);
    }

    private boolean isServiceDefaultEndpoint(final String pathInfo) {
        return ((pathInfo != null) && pathInfo.isEmpty()) || ("/" + pathInfo)
                .startsWith(RestBinding.URI_PATTERN);
    }

    private boolean isOfferingsRequest(final String pathInfo) {
        return (pathInfo != null) && pathInfo.startsWith(org.n52.sos.binding.rest.Constants.REST_RESOURCE_RELATION_OFFERINGS);
    }

    private boolean isFeaturesRequest(final String pathInfo) {
        return (pathInfo != null) && pathInfo.startsWith(org.n52.sos.binding.rest.Constants.REST_RESOURCE_RELATION_FEATURES);
    }

    private boolean isCapabilitiesRequest(final String pathInfo) {
        return (pathInfo != null) && pathInfo.startsWith(org.n52.sos.binding.rest.Constants.REST_RESOURCE_RELATION_CAPABILITIES);
    }

    private boolean isObservationsRequest(final String pathInfo) {
        return (pathInfo != null) && pathInfo.startsWith(org.n52.sos.binding.rest.Constants.REST_RESOURCE_RELATION_OBSERVATIONS);
    }

    private boolean isSensorsRequest(final String pathInfo) {
        return (pathInfo != null) && pathInfo.startsWith(org.n52.sos.binding.rest.Constants.REST_RESOURCE_SENSORS);
    }

    @Override
    public Set<DecoderKey> getKeys() {
        return Collections.unmodifiableSet(decoderKeys);
    }

    private RestConstants bindingConstants() {
        return this.constants;
    }

}
