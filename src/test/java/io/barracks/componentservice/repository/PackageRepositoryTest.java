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

package io.barracks.componentservice.repository;

import io.barracks.componentservice.model.Package;
import io.barracks.componentservice.repository.exception.DuplicatePackageException;
import io.barracks.componentservice.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(SpringRunner.class)
@DataMongoTest
public class PackageRepositoryTest {
    @Autowired
    private PackageRepository packageRepository;

    @Test
    public void createPackage_shouldReturnSamePackage() {
        // Given
        final Package aPackage = PackageUtils.getPackage();

        // When
        final Package result = packageRepository.createPackage(aPackage);

        // Then
        assertThat(result).isEqualTo(aPackage);
    }

    @Test
    public void createPackage_withSameUserAndReference_shouldThrowException() {
        // Given
        final Package aPackage = PackageUtils.getPackage();
        packageRepository.createPackage(aPackage);

        // Then When
        assertThatExceptionOfType(DuplicatePackageException.class).isThrownBy(() -> packageRepository.createPackage(aPackage));
    }

    @Test
    public void getPackage_whenNoPackage_shouldReturnEmptyResult() {
        // Given
        final String userId = UUID.randomUUID().toString(), reference = UUID.randomUUID().toString();

        // When
        final Optional<Package> result = packageRepository.getPackage(userId, reference);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getPackage_whenPackageExists_shouldReturnPackage() {
        // Given
        final Package aPackage = PackageUtils.getPackage();
        packageRepository.save(aPackage);

        // When
        final Optional<Package> result = packageRepository.getPackage(aPackage.getUserId(), aPackage.getReference());

        // Then
        assertThat(result).contains(aPackage);
    }

    @Test
    public void getPackages_whenNoPackage_shouldReturnEmptyList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        // When
        final Page<Package> result = packageRepository.getPackage(userId, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getPackages_whenPackages_shouldReturnPackagesList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Package> expected = getPackages(userId).stream().map(reference -> packageRepository.createPackage(reference)).collect(Collectors.toList());

        // When
        final Page<Package> result = packageRepository.getPackage(userId, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void getPackages_whenPackagesForMoreThanOneUser_shouldReturnPackagesOfCurrentUserOnly() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<Package> expected = getPackages(userId);
        final List<Package> otherPackages = getPackages(UUID.randomUUID().toString());

        expected.forEach(reference -> packageRepository.createPackage(reference));
        otherPackages.forEach(reference -> packageRepository.createPackage(reference));

        // When
        final Page<Package> result = packageRepository.getPackage(userId, pageable);

        // Then
        assertThat(result).containsAll(expected);
    }

    @Test
    public void getPackages_whenManyPackages_shouldReturnPagedList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(1, 5);
        final List<Package> expected = getPackages(userId).stream().map(reference -> packageRepository.createPackage(reference)).collect(Collectors.toList());

        // When
        final Page<Package> result = packageRepository.getPackage(userId, pageable);

        // Then
        assertThat(result).hasSize(5).isSubsetOf(expected);
    }

    private List<Package> getPackages(String userId) {
        return IntStream.range(0, 10)
                .mapToObj((index) -> getPackage(userId))
                .collect(Collectors.toList());
    }

    private Package getPackage(String userId) {
        return PackageUtils.getPackage().toBuilder()
                .userId(userId)
                .build();
    }
}
