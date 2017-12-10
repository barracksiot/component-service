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

package io.barracks.componentservice.rest;

import io.barracks.componentservice.manager.PackageManager;
import io.barracks.componentservice.model.Package;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/owners/{userId}/packages")
public class PackageResource {

    private final PagedResourcesAssembler<Package> assembler;
    private final PackageManager packageManager;

    public PackageResource(PagedResourcesAssembler<Package> assembler, PackageManager packageManager) {
        this.assembler = assembler;
        this.packageManager = packageManager;
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Package createPackage(@PathVariable("userId") @Valid @NotBlank String userId, @Valid @RequestBody Package aPackage) {
        return packageManager.createPackage(aPackage.toBuilder().userId(userId).build());
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<Package>> getPackages(
            @PathVariable("userId") @Valid @NotBlank String userId,
            Pageable pageable
    ) {
        return assembler.toResource(packageManager.getPackages(userId, pageable));
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/{reference}")
    public Package getPackage(
            @PathVariable("userId") @Valid @NotBlank String userId,
            @PathVariable("reference") @Valid @NotBlank String reference
    ) {
        return packageManager.getPackage(userId, reference);
    }

}
