/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.componentservice.config;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolverBuilder;
import io.barracks.commons.configuration.ExceptionHandlingConfiguration;
import io.barracks.componentservice.manager.exception.PackageNotFoundException;
import io.barracks.componentservice.manager.exception.VersionNotFoundException;
import io.barracks.componentservice.repository.exception.DuplicatePackageException;
import io.barracks.componentservice.manager.exception.VersionCreationFailedException;
import io.barracks.componentservice.repository.exception.DuplicateVersionException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ExceptionConfig extends ExceptionHandlingConfiguration {

    @Override
    public RestHandlerExceptionResolverBuilder restExceptionResolver() {
        return super.restExceptionResolver()
                .addErrorMessageHandler(DuplicatePackageException.class, HttpStatus.CONFLICT)
                .addErrorMessageHandler(DuplicateVersionException.class, HttpStatus.CONFLICT)
                .addErrorMessageHandler(VersionCreationFailedException.class, HttpStatus.BAD_REQUEST)
                .addErrorMessageHandler(VersionNotFoundException.class, HttpStatus.NOT_FOUND)
                .addErrorMessageHandler(PackageNotFoundException.class, HttpStatus.NOT_FOUND);
    }

    @Override
    protected String getBaseName() {
        return "classpath:/io/barracks/componentservice/configuration/messages";
    }
}
