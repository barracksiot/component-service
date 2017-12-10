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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.componentservice.manager.VersionManager;
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.rest.entity.VersionEntity;
import io.barracks.componentservice.utils.VersionUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionResourceTest {
    @Mock
    private VersionManager versionManager;

    private PagedResourcesAssembler<Version> pagedResourcesAssembler = PagedResourcesUtils.getPagedResourcesAssembler();

    @InjectMocks
    private VersionResource versionResource;

    @Before
    public void setup() {
        versionResource = new VersionResource(pagedResourcesAssembler, versionManager);
    }

    @Test
    public void createVersion_shouldCallManager_andReturnVersion() throws Exception {
        // Given
        final MockMultipartFile file = new MockMultipartFile("file", UUID.randomUUID().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});
        final VersionEntity entity = VersionUtils.getVersionEntity();
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version version = Version.builder()
                .userId(userId)
                .packageRef(packageRef)
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .metadata(entity.getMetadata())
                .filename(file.getOriginalFilename())
                .build();

        final Version expected = VersionUtils.getVersion();
        doReturn(expected).when(versionManager).createVersion(eq(version), isA(InputStream.class));

        // When
        final Version result = versionResource.createVersion(file, entity, userId, packageRef);

        // Then
        assertThat(version).hasNoNullFieldsOrPropertiesExcept("md5", "length", "inputStream");
        verify(versionManager).createVersion(eq(version), isA(InputStream.class));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "Throwing exception")
    public void createVersion_whenFileAccessFails_shouldThrowException() throws Exception {
        // Given
        final MockMultipartFile file = spy(new MockMultipartFile("file", UUID.randomUUID().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'}));
        final VersionEntity entity = VersionUtils.getVersionEntity();
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        doThrow(IOException.class).when(file).getInputStream();

        // Then When
        assertThatExceptionOfType(MultipartException.class).isThrownBy(() -> versionResource.createVersion(file, entity, userId, packageRef));
    }

    @Test
    public void getVersions_whenAllIsFine_shouldCallManagerAndReturnVersionList() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Version version1 = VersionUtils.getVersion();
        final Version version2 = VersionUtils.getVersion();
        final Page<Version> page = new PageImpl<>(Lists.newArrayList(version1, version2));
        final PagedResources<Resource<Version>> expected = PagedResourcesUtils.<Version>getPagedResourcesAssembler().toResource(page);

        when(versionManager.getVersions(userId, packageRef, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<Version>> result = versionResource.getVersions(userId, packageRef, pageable);

        // Then
        verify(versionManager).getVersions(userId, packageRef, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersions_whenAllIsFineAndNoVersion_shouldCallManagerAndReturnEmptyPage() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(1, 10);
        final Page<Version> page = new PageImpl<>(Collections.emptyList());
        final PagedResources<Resource<Version>> expected = PagedResourcesUtils.<Version>getPagedResourcesAssembler().toResource(page);

        when(versionManager.getVersions(userId, packageRef, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<Version>> result = versionResource.getVersions(userId, packageRef, pageable);

        // Then
        verify(versionManager).getVersions(userId, packageRef, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersion_whenVersionExist_shouldReturnThatVersion() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Version expected = VersionUtils.getVersion();
        doReturn(expected).when(versionManager).getVersion(userId, packageRef, versionId);

        // When
        final Version result = versionResource.getVersion(userId, packageRef, versionId);

        // Then
        verify(versionManager).getVersion(userId, packageRef, versionId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersionFile_whenVersionExist_shouldReturnFileContents() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Version version = VersionUtils.getVersion();
        doReturn(version).when(versionManager).getVersion(userId, packageRef, versionId);

        // When
        final ResponseEntity result = versionResource.getVersionFile(userId, packageRef, versionId);

        // Then
        verify(versionManager).getVersion(userId, packageRef, versionId);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(new InputStreamResource(version.getInputStream()));
        assertThat(result.getHeaders().getContentLength()).isEqualTo(version.getLength());
    }
}