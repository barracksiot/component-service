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

import io.barracks.componentservice.manager.VersionManager;
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.rest.entity.VersionEntity;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Collection;

@RestController
@RequestMapping("/owners/{userId}/packages/{reference}/versions")
public class VersionResource {
    private final PagedResourcesAssembler<Version> assembler;
    private final VersionManager versionManager;

    @Autowired
    public VersionResource(PagedResourcesAssembler<Version> assembler, VersionManager versionManager) {
        this.versionManager = versionManager;
        this.assembler = assembler;
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.CREATED)
    public Version createVersion(
            @RequestParam("file") MultipartFile file,
            @RequestPart("version") @Valid VersionEntity versionEntity,
            @PathVariable("userId") String userId,
            @PathVariable("reference") String reference) {
        try {
            final Version version = Version.builder()
                    .userId(userId)
                    .id(versionEntity.getId())
                    .packageRef(reference)
                    .name(versionEntity.getName())
                    .description(versionEntity.getDescription())
                    .filename(file.getOriginalFilename())
                    .metadata(versionEntity.getMetadata())
                    .build();
            return versionManager.createVersion(version, file.getInputStream());
        } catch (IOException e) {
            throw new MultipartException("Failed to access file.", e);
        }
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<Version>> getVersions(
            @PathVariable("userId") @Valid @NotBlank String userId,
            @PathVariable("reference") @Valid @NotBlank String reference,
            Pageable pageable
    ) {
        return assembler.toResource(versionManager.getVersions(userId, reference, pageable));
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{version}")
    @ResponseBody
    @ResponseStatus(value = HttpStatus.OK)
    public Version getVersion(
            @PathVariable("userId") String userId,
            @PathVariable("reference") String reference,
            @PathVariable("version") String version) {
        return versionManager.getVersion(userId, reference, version);
    }


    @RequestMapping(method = RequestMethod.GET, path = "/{version}/file", produces = "application/octet-stream")
    public ResponseEntity<?> getVersionFile(
            @PathVariable("userId") String userId,
            @PathVariable("reference") String reference,
            @PathVariable("version") String versionId) {
        final Version version = versionManager.getVersion(userId, reference, versionId);
        InputStreamResource inputStreamResource = new InputStreamResource(version.getInputStream());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentLength(version.getLength());
        return new ResponseEntity<>(inputStreamResource, httpHeaders, HttpStatus.OK);
    }
}