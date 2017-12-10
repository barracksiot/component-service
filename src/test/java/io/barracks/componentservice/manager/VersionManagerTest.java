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

import io.barracks.componentservice.manager.exception.VersionCreationFailedException;
import io.barracks.componentservice.manager.exception.VersionNotFoundException;
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.repository.PackageRepository;
import io.barracks.componentservice.repository.VersionRepository;
import io.barracks.componentservice.utils.PackageUtils;
import io.barracks.componentservice.utils.VersionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionManagerTest {
    @Mock
    private PackageRepository packageRepository;
    @Mock
    private VersionRepository versionRepository;
    @InjectMocks
    private VersionManager versionManager;

    @Test
    public void createVersion_shouldCallRepositoryWithCompleteVersion_andReturnResult() {
        // Given
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        final Version toCreate = VersionUtils.getVersion();
        final Version expected = VersionUtils.getVersion();
        doReturn(Optional.of(PackageUtils.getPackage())).when(packageRepository).getPackage(toCreate.getUserId(), toCreate.getPackageRef());
        doReturn(expected).when(versionRepository).createVersion(toCreate, inputStream);

        // When
        final Version result = versionManager.createVersion(toCreate, inputStream);

        // Then
        verify(packageRepository).getPackage(toCreate.getUserId(), toCreate.getPackageRef());
        verify(versionRepository).createVersion(toCreate, inputStream);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void createVersion_whenPackageDoesNotExist_shouldThrowException() {
        // Given
        final Version toCreate = VersionUtils.getVersion();
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        doReturn(Optional.empty()).when(packageRepository).getPackage(toCreate.getUserId(), toCreate.getPackageRef());

        // Then When
        assertThatExceptionOfType(VersionCreationFailedException.class).isThrownBy(() -> versionManager.createVersion(toCreate, inputStream));
    }

    @Test
    public void getVersion_shouldCallRepository_andReturnVersion() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Version expected = VersionUtils.getVersion();
        doReturn(Optional.of(expected)).when(versionRepository).getVersion(userId, reference, versionId);

        // When
        final Version result = versionManager.getVersion(userId, reference, versionId);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersion_whenVersionNotPresent_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        doReturn(Optional.empty()).when(versionRepository).getVersion(userId, reference, versionId);

        // Then When
        assertThatExceptionOfType(VersionNotFoundException.class).isThrownBy(() -> versionManager.getVersion(userId, reference, versionId));
    }

    @Test
    public void getVersions_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Version version1 = VersionUtils.getVersion();
        final Version version2 = VersionUtils.getVersion();
        final List<Version> response = Arrays.asList(version1, version2);
        final Page<Version> expected = new PageImpl<>(response, pageable, 2);

        when(versionRepository.getVersions(userId, reference, pageable)).thenReturn(expected);

        // When
        final Page<Version> result = versionManager.getVersions(userId, reference, pageable);

        // Then
        verify(versionRepository).getVersions(userId, reference, pageable);
        assertThat(result).isEqualTo(expected);
    }
}
