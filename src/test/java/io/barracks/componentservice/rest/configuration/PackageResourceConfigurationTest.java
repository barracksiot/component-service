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
import io.barracks.componentservice.model.Package;
import io.barracks.componentservice.rest.PackageResource;
import io.barracks.componentservice.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = PackageResource.class, includeFilters = @ComponentScan.Filter(classes = {EnableSpringDataWebSupport.class}, type = FilterType.ANNOTATION))
@EnableSpringDataWebSupport
@AutoConfigureRestDocs("build/generated-snippets/packages")
public class PackageResourceConfigurationTest {
    private static final Endpoint GET_PACKAGES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages");
    private static final Endpoint GET_PACKAGE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{reference}");
    private static final String baseUrl = "https://not.barracks.io";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PackageResource packageResource;

    @Test
    public void documentCreatePackage() throws Exception {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = UUID.randomUUID().toString();
        final Package request = Package.builder()
                .name("A great package")
                .description("This package will be used to build great stuff !")
                .reference("io.barracks.package")
                .build();
        final Package savedPackage = request.toBuilder().userId(userId).build();
        doReturn(savedPackage).when(packageResource).createPackage(userId, request);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.post("/owners/{userId}/packages", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // Then
        result.andExpect(status().isCreated())
                .andDo(
                        document(
                                "create",
                                pathParameters(
                                        parameterWithName("userId").description("User ID")
                                ),
                                requestFields(
                                        fieldWithPath("reference").description("The package's unique reference"),
                                        fieldWithPath("name").description("The package's name"),
                                        fieldWithPath("description").description("The package's description")
                                )
                        )
                );

    }

    @Test
    public void createPackage_whenArgumentIsValid_shouldReturn201() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package request = PackageUtils.getPackage().toBuilder().userId(null).build();
        final Package savedPackage = PackageUtils.getPackage().toBuilder().userId(null).build();
        doReturn(savedPackage).when(packageResource).createPackage(userId, request);

        // When
        final ResultActions result = mvc.perform(
                post("/owners/{userId}/packages", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().string(containsString(savedPackage.getReference())));
        verify(packageResource).createPackage(userId, request);
    }

    @Test
    public void createPackage_whenEmptyBody_shouldNotCallResourceAndReturn405() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(
                post("/owners/{userId}/packages", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isUnprocessableEntity());
        verifyZeroInteractions(packageResource);
    }

    @Test
    public void createPackage_whenPackageNotValid_shouldNotCallResourceAndReturnBadRequest() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = PackageUtils.getPackage().toBuilder().reference(null).build();

        // When
        final ResultActions result = mvc.perform(
                post("/owners/{userId}/packages", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aPackage))
        );

        // Then
        result.andExpect(status().isBadRequest());
        verifyZeroInteractions(packageResource);
    }

    @Test
    public void documentGetPackages() throws Exception {
        final Endpoint endpoint = GET_PACKAGES_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = UUID.randomUUID().toString();
        final Package package1 = Package.builder()
                .name("A great package")
                .description("This package will be used to build great stuff !")
                .reference("io.barracks.package1")
                .build();

        final Package package2 = Package.builder()
                .name("An other great package")
                .description("This package will also be used to build great stuff !")
                .reference("io.barracks.package2")
                .build();

        final Page<Package> page = new PageImpl<>(Arrays.asList(package1, package2));
        final PagedResources<Resource<Package>> packages = PagedResourcesUtils.<Package>getPagedResourcesAssembler(baseUrl).toResource(page);
        doReturn(packages).when(packageResource).getPackages(eq(userId), any(Pageable.class));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        // Then
        verify(packageResource).getPackages(eq(userId), any(Pageable.class));
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "list",
                                pathParameters(
                                        parameterWithName("userId").description("User ID")
                                ),
                                responseFields(
                                        fieldWithPath("_embedded.packages").description("The list of packages"),
                                        fieldWithPath("_links").ignored(),
                                        fieldWithPath("page").ignored()
                                )
                        )
                );
    }

    @Test
    public void getPackages_whenAllIsFine_shouldCallResourceAndReturnPackageList() throws Exception {
        //Given
        final Endpoint endpoint = GET_PACKAGES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Package package1 = PackageUtils.getPackage();
        final Package package2 = PackageUtils.getPackage();
        final Page<Package> page = new PageImpl<>(Arrays.asList(package1, package2));
        final PagedResources<Resource<Package>> expected = PagedResourcesUtils.<Package>getPagedResourcesAssembler(baseUrl).toResource(page);

        doReturn(expected).when(packageResource).getPackages(userId, pageable);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        //Then
        verify(packageResource).getPackages(userId, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.packages", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.packages[0].name").value(package1.getName()))
                .andExpect(jsonPath("$._embedded.packages[1].name").value(package2.getName()));
    }

    @Test
    public void documentGetPackage() throws Exception {
        final Endpoint endpoint = GET_PACKAGE_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = Package.builder()
                .name("A great aPackage")
                .description("This aPackage will be used to build great stuff !")
                .reference("io.barracks.aPackage")
                .build();

        doReturn(aPackage).when(packageResource).getPackage(userId, aPackage.getReference());

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId, aPackage.getReference())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        // Then
        verify(packageResource).getPackage(userId, aPackage.getReference());
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "get",
                                pathParameters(
                                        parameterWithName("userId").description("User ID"),
                                        parameterWithName("reference").description("Package reference")
                                ),
                                responseFields(
                                        fieldWithPath("reference").description("The aPackage's unique reference"),
                                        fieldWithPath("name").description("The aPackage's name"),
                                        fieldWithPath("description").description("The aPackage's description")
                                )
                        )
                );
    }

    @Test
    public void getPackage_whenAllIsFine_shouldCallResourceAndReturnPackage() throws Exception {
        //Given
        final Endpoint endpoint = GET_PACKAGE_ENDPOINT;
        final Package expected = PackageUtils.getPackage();

        doReturn(expected).when(packageResource).getPackage(expected.getUserId(), expected.getReference());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(expected.getUserId(), expected.getReference()))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        //Then
        verify(packageResource).getPackage(expected.getUserId(), expected.getReference());
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(expected.getName()))
                .andExpect(jsonPath("$.reference").value(expected.getReference()));
    }

}
