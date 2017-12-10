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
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.repository.exception.DuplicateVersionException;
import io.barracks.componentservice.utils.PackageUtils;
import io.barracks.componentservice.utils.VersionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(SpringRunner.class)
@DataMongoTest
public class VersionRepositoryTest {

    @Autowired
    private VersionRepository versionRepository;

    @Test
    public void createVersion_shouldReturnIdenticalDocuments_exceptForIdAndLengthAndMd5() throws Exception {
        // Given
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        final Version version = VersionUtils.getVersion();
        final Version expected = version.toBuilder()
                .length(3)
                .md5("900150983cd24fb0d6963f7d28e17f72")
                .build();

        // When
        final Version result = versionRepository.createVersion(version, inputStream);

        // Then
        assertThat(result).hasNoNullFieldsOrProperties().isEqualTo(expected);
    }

    @Test
    public void createVersion_withIdenticalUserIdPackageRefAndVersionId_shouldThrowException() throws Exception {
        // Given
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        final Version version = VersionUtils.getVersion();
        Version savedVersion = versionRepository.createVersion(version, inputStream);

        // Then When
        assertThatExceptionOfType(DuplicateVersionException.class).isThrownBy(() -> versionRepository.createVersion(version, inputStream));
        assertThat(savedVersion).isNotNull();
    }

    @Test
    public void findVersion_whenNoVersionMatches_shouldReturnEmpty() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();

        // When
        final Optional<Version> result = versionRepository.getVersion(userId, packageRef, versionId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findVersion_whenVersionExists_shouldReturnVersion() throws Exception {
        // Given
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        final Version version = VersionUtils.getVersion();
        final Version expected = versionRepository.createVersion(version, inputStream);

        // When
        final Version result = versionRepository.getVersion(version.getUserId(), version.getPackageRef(), version.getId()).orElseThrow(() -> new RuntimeException("Document should not be null"));

        // Then
        assertThat(result).hasNoNullFieldsOrProperties();
        assertThat(result).isEqualTo(expected);
        assertThat(result.getInputStream()).isNotNull();
    }

    @Test
    public void getVersions_WhenNoVersion_ShouldReturnEmptyList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = PackageUtils.getPackage();
        final Pageable pageable = new PageRequest(0, 10);

        // When
        final Page<Version> result = versionRepository.getVersions(userId, aPackage.getReference(), pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getVersions_whenVersions_shouldReturnVersionsList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Version> expected = getVersions(userId, packageRef).stream().map(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'}))).collect(Collectors.toList());

        // When
        final Page<Version> result = versionRepository.getVersions(userId, packageRef, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void getVersions_whenVersionsForMoreThanOneUser_shouldReturnVersionsOfCurrentUserOnly() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<Version> expected = getVersions(userId, packageRef)
                .stream()
                .map(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})))
                .collect(Collectors.toList());

        getVersions(UUID.randomUUID().toString(), packageRef)
                .forEach(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})));

        // When
        final Page<Version> result = versionRepository.getVersions(userId, packageRef, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void getVersions_whenVersionsForMoreThanOnePackage_shouldReturnVersionsOfCurrentPackageOnly() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = PackageUtils.getPackage();
        final Pageable pageable = new PageRequest(0, 20);
        final List<Version> expected = getVersions(userId, aPackage.getReference())
                .stream()
                .map(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})))
                .collect(Collectors.toList());

        getVersions(userId, PackageUtils.getPackage().getReference())
                .forEach(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})));


        // When
        final Page<Version> result = versionRepository.getVersions(userId, aPackage.getReference(), pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void getVersions_whenVersionsForMoreThanOneUserAndMoreThanOnePackage_shouldReturnVersionsOfCurrentUserAndCurrentPackageOnly() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<Version> expected = getVersions(userId, packageRef)
                .stream()
                .map(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})))
                .collect(Collectors.toList());

        getVersions(UUID.randomUUID().toString(), UUID.randomUUID().toString())
                .forEach(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})));

        // When
        final Page<Version> result = versionRepository.getVersions(userId, packageRef, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void getVersions_whenManyVersions_shouldReturnPagedList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 5);
        final List<Version> expected = getVersions(userId, packageRef)
                .stream()
                .map(version -> versionRepository.createVersion(version, new ByteArrayInputStream(new byte[]{'a', 'b', 'c'})))
                .collect(Collectors.toList());

        // When
        final Page<Version> result = versionRepository.getVersions(userId, packageRef, pageable);

        // Then
        assertThat(result).hasSize(5)
                .isSubsetOf(expected);
    }

    private List<Version> getVersions(String userId, String packageRef) {
        return IntStream.range(0, 10)
                .mapToObj((index) -> getVersion(userId, packageRef))
                .collect(Collectors.toList());
    }

    private Version getVersion(String userId, String packageRef) {
        return VersionUtils.getVersion().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .build();
    }
}