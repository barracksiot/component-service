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

package io.barracks.componentservice.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.componentservice.manager.exception.VersionNotFoundException;
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.rest.VersionResource;
import io.barracks.componentservice.rest.entity.VersionEntity;
import io.barracks.componentservice.utils.VersionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@WebMvcTest(controllers = VersionResource.class, includeFilters = @ComponentScan.Filter(classes = {EnableSpringDataWebSupport.class}, type = FilterType.ANNOTATION))
@EnableSpringDataWebSupport
@AutoConfigureRestDocs("build/generated-snippets/versions")
public class VersionResourceConfigurationTest {
    private static final Endpoint GET_VERSION_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{reference}/versions/{version}");
    private static final Endpoint GET_VERSION_FILE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{reference}/versions/{version}/file");
    private static final Endpoint GET_VERSIONS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{reference}/versions");
    private static final String baseUrl = "https://not.barracks.io";
    @MockBean
    private VersionResource versionResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    public void documentCreateVersion() throws Exception {
        // Given
        json.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = UUID.randomUUID().toString();
        final String packageRef = "io.barracks.package";
        final VersionEntity version = VersionEntity.builder()
                .id("0.0.1")
                .name("First package version")
                .description("Changes : Initial version")
                .metadata(Collections.singletonMap("critical", true))
                .build();
        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "package.bin", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});
        final MockMultipartFile mockMultipartVersion = new MockMultipartFile("version", null, MediaType.APPLICATION_JSON_UTF8_VALUE, json.writeValueAsBytes(version));
        final Version expected = Version.builder()
                .userId(userId)
                .packageRef(packageRef)
                .id(version.getId())
                .filename(mockMultipartFile.getOriginalFilename())
                .length(3)
                .md5("900150983cd24fb0d6963f7d28e17f72")
                .name(version.getName())
                .description(version.getDescription())
                .metadata(version.getMetadata())
                .build();
        doReturn(expected).when(versionResource).createVersion(
                mockMultipartFile,
                version,
                userId,
                packageRef
        );

        // When
        final ResultActions result = mvc.perform(
                fileUpload("/owners/{userId}/packages/{packageRef}/versions", userId, packageRef)
                        .file(mockMultipartFile)
                        .file(mockMultipartVersion)
        );

        // Then
        assertThat(expected).hasNoNullFieldsOrPropertiesExcept("inputStream");
        verify(versionResource).createVersion(
                mockMultipartFile,
                version,
                userId,
                packageRef
        );
        result.andExpect(status().isCreated())
                .andExpect(content().json(json.writeValueAsString(expected)))
                .andDo(
                        document("create")
                );
    }

    @Test
    public void documentGetVersion() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = "io.barracks.package";
        final String versionId = "2-5-1";
        final Version version = Version.builder()
                .userId(userId)
                .packageRef(packageRef)
                .id(versionId)
                .description("Barracks package description")
                .name("Barracks package v2-5-1")
                .filename("barracks-package-2-5-1.tar.gz")
                .md5("4c2383f5c88e9110642953b5dd7c88a1")
                .length(76544567L)
                .build();

        doReturn(version).when(versionResource).getVersion(userId, packageRef, versionId);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/owners/{userId}/packages/{packageRef}/versions/{versionId}/", userId, packageRef, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "get",
                        pathParameters(
                                parameterWithName("userId").description("The User Id"),
                                parameterWithName("packageRef").description("The reference of the package to get the version from"),
                                parameterWithName("versionId").description("The id of the version to retrieve")
                        )
                        )
                );
    }

    @Test
    public void documentGetVersionFile() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSION_FILE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = "io.barracks.package";
        final String versionId = "2-5-1";
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(3);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        final ResponseEntity<?> response = new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.OK);

        doReturn(response).when(versionResource).getVersionFile(userId, packageRef, versionId);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId, packageRef, versionId)
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
        );

        // Then
        verify(versionResource).getVersionFile(userId, packageRef, versionId);
        result.andExpect(status().isOk())
                .andDo(document(
                        "get-file",
                        pathParameters(
                                parameterWithName("userId").description("The User Id"),
                                parameterWithName("reference").description("The reference of the package to get the version from"),
                                parameterWithName("version").description("The id of the version to retrieve")
                        )
                        )
                );
    }

    @Test
    public void postMultipart_shouldCallCreateVersion() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString(), packageRef = UUID.randomUUID().toString();
        final VersionEntity version = VersionUtils.getVersionEntity();
        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", UUID.randomUUID().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});
        final MockMultipartFile mockMultipartVersion = new MockMultipartFile("version", UUID.randomUUID().toString(), MediaType.APPLICATION_JSON_UTF8_VALUE, json.writeValueAsBytes(version));
        final Version expected = Version.builder()
                .packageRef(packageRef)
                .id(version.getId())
                .filename(mockMultipartFile.getOriginalFilename())
                .length(3)
                .md5("900150983cd24fb0d6963f7d28e17f72")
                .name(version.getName())
                .description(version.getDescription())
                .metadata(version.getMetadata())
                .build();
        doReturn(expected).when(versionResource).createVersion(
                mockMultipartFile,
                version,
                userId,
                packageRef
        );

        // When
        final ResultActions result = mvc.perform(
                fileUpload("/owners/{userId}/packages/{packageRef}/versions", userId, packageRef)
                        .file(mockMultipartFile)
                        .file(mockMultipartVersion)
        );

        // Then
        assertThat(expected).hasNoNullFieldsOrPropertiesExcept("userId", "inputStream");
        verify(versionResource).createVersion(
                mockMultipartFile,
                version,
                userId,
                packageRef
        );
        result.andExpect(status().isCreated())
                .andExpect(content().json(json.writeValueAsString(expected)));
    }

    @Test
    public void postMultipart_withNoFile_shouldReturn400() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString(), packageRef = UUID.randomUUID().toString();
        final VersionEntity version = VersionUtils.getVersionEntity();
        final MockMultipartFile mockMultipartVersion = new MockMultipartFile("version", UUID.randomUUID().toString(), MediaType.APPLICATION_JSON_UTF8_VALUE, json.writeValueAsBytes(version));

        // When
        final ResultActions result = mvc.perform(
                fileUpload("/owners/{userId}/packages/{packageRef}/versions", userId, packageRef)
                        .file(mockMultipartVersion)
        );

        // Then
        verifyZeroInteractions(versionResource);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void postMultipart_withNoVersion_shouldReturn400() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString(), packageRef = UUID.randomUUID().toString();
        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", UUID.randomUUID().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});

        // When
        final ResultActions result = mvc.perform(
                fileUpload("/owners/{userId}/packages/{packageRef}/versions", userId, packageRef)
                        .file(mockMultipartFile)
        );

        // Then
        verifyZeroInteractions(versionResource);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getVersion_whenNoVersion_shouldReturnNotFound() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = "a.ref.coucou";
        final String versionId = "2-0";
        doThrow(VersionNotFoundException.class).when(versionResource).getVersion(userId, packageRef, versionId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).getURI(userId, packageRef, versionId)
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(versionResource).getVersion(userId, packageRef, versionId);
        result.andExpect(status().isNotFound());
    }

    @Test
    public void getVersion_whenAllIsFine_shouldReturnVersion() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = "a.ref.coucou";
        final String versionId = "2-0";
        final Version version = VersionUtils.getVersion();
        doReturn(version).when(versionResource).getVersion(userId, packageRef, versionId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).getURI(userId, packageRef, versionId)
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(versionResource).getVersion(userId, packageRef, versionId);
        result.andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(version)));
    }

    @Test
    public void getVersions_whenAllIsFine_shouldCallResourceAndReturnVersionList() throws Exception {
        //Given
        final Endpoint endpoint = GET_VERSIONS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Version version1 = VersionUtils.getVersion();
        final Version version2 = VersionUtils.getVersion();
        final Page<Version> page = new PageImpl<>(Arrays.asList(version1, version2));
        final PagedResources<Resource<Version>> expected = PagedResourcesUtils.<Version>getPagedResourcesAssembler(baseUrl).toResource(page);

        doReturn(expected).when(versionResource).getVersions(userId, packageRef, pageable);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, packageRef))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        //Then
        verify(versionResource).getVersions(userId, packageRef, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.versions", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.versions[0].name").value(version1.getName()))
                .andExpect(jsonPath("$._embedded.versions[1].name").value(version2.getName()));
    }
}
