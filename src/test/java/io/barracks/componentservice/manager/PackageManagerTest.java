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

package io.barracks.componentservice.manager;

import io.barracks.componentservice.manager.exception.PackageNotFoundException;
import io.barracks.componentservice.model.Package;
import io.barracks.componentservice.repository.PackageRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.barracks.componentservice.utils.PackageUtils.getPackage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PackageManagerTest {
    @Mock
    private PackageRepository packageRepository;

    @InjectMocks
    private PackageManager packageManager;

    @Test
    public void createPackage_shouldCallRepository_andReturnResult() {
        // Given
        final Package aPackage = getPackage();
        final Package expected = aPackage.toBuilder().reference(UUID.randomUUID().toString()).build();
        doReturn(expected).when(packageRepository).createPackage(aPackage);

        // When
        final Package result = packageManager.createPackage(aPackage);

        // Then
        verify(packageRepository).createPackage(aPackage);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackages_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Package package1 = getPackage();
        final Package package2 = getPackage();
        final List<Package> response = Arrays.asList(package1, package2);
        final Page<Package> expected = new PageImpl<>(response, pageable, 2);

        when(packageRepository.getPackage(userId, pageable)).thenReturn(expected);

        // When
        final Page<Package> result = packageManager.getPackages(userId, pageable);

        // Then
        verify(packageRepository).getPackage(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackage_shouldCallRepository_andReturnResult() {
        // Given
        final Package expected = getPackage();

        when(packageRepository.getPackage(expected.getUserId(), expected.getReference())).thenReturn(Optional.of(expected));

        // When
        final Package result = packageManager.getPackage(expected.getUserId(), expected.getReference());

        // Then
        verify(packageRepository).getPackage(expected.getUserId(), expected.getReference());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackage_whenNoPackage_shouldCallRepositoryAndThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();


        when(packageRepository.getPackage(userId, reference)).thenReturn(Optional.empty());

        // Then When
        assertThatExceptionOfType(PackageNotFoundException.class).isThrownBy(() ->
                packageManager.getPackage(userId, reference)
        );

        verify(packageRepository).getPackage(userId, reference);
    }
}
