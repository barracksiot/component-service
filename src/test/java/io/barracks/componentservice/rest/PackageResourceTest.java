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

import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.componentservice.manager.PackageManager;
import io.barracks.componentservice.model.Package;
import io.barracks.componentservice.utils.PackageUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PackageResourceTest {

    @Mock
    private PackageManager packageManager;
    private PagedResourcesAssembler<Package> pagedResourcesAssembler = PagedResourcesUtils.getPagedResourcesAssembler();
    private PackageResource packageResource;

    @Before
    public void setup() {
        packageResource = new PackageResource(pagedResourcesAssembler, packageManager);
    }

    @Test
    public void createPackage_whenArgumentIsValid_shouldCallManagerAndReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = PackageUtils.getPackage();
        final Package toCreate = aPackage.toBuilder().userId(userId).build();
        final Package expected = toCreate.toBuilder().reference(UUID.randomUUID().toString()).build();
        given(packageManager.createPackage(toCreate))
                .willReturn(expected);

        // When
        final Package result = packageResource.createPackage(userId, aPackage);

        // Then
        verify(packageManager).createPackage(toCreate);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackages_whenAllIsFine_shouldCallManagerAndReturnPackageList() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Package package1 = PackageUtils.getPackage();
        final Package package2 = PackageUtils.getPackage();
        final Page<Package> page = new PageImpl<>(Lists.newArrayList(package1, package2));
        final PagedResources<Resource<Package>> expected = pagedResourcesAssembler.toResource(page);

        when(packageManager.getPackages(userId, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<Package>> result = packageResource.getPackages(userId, pageable);

        // Then
        verify(packageManager).getPackages(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackages_whenAllIsFineAndNoPackage_shouldCallManagerAndReturnEmptyPage() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(1, 10);
        final Page<Package> page = new PageImpl<>(Collections.emptyList());
        final PagedResources<Resource<Package>> expected = pagedResourcesAssembler.toResource(page);

        when(packageManager.getPackages(userId, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<Package>> result = packageResource.getPackages(userId, pageable);

        // Then
        verify(packageManager).getPackages(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackage_whenAllIsFineAndPackage_shouldReturnPackageOfUser() throws Exception {
        // Given
        final Package expected = PackageUtils.getPackage();

        when(packageManager.getPackage(expected.getUserId(), expected.getReference())).thenReturn(expected);

        // When
        final Package result = packageResource.getPackage(expected.getUserId(), expected.getReference());

        // Then
        verify(packageManager).getPackage(expected.getUserId(), expected.getReference());
        assertThat(result).isEqualTo(expected);
    }
}