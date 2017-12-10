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
import io.barracks.componentservice.manager.exception.VersionNotFoundException;
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.repository.PackageRepository;
import io.barracks.componentservice.repository.VersionRepository;
import io.barracks.componentservice.manager.exception.VersionCreationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class VersionManager {
    private final PackageRepository packageRepository;
    private final VersionRepository versionRepository;

    @Autowired
    public VersionManager(PackageRepository packageRepository, VersionRepository versionRepository) {
        this.packageRepository = packageRepository;
        this.versionRepository = versionRepository;
    }

    public Version createVersion(
            Version version,
            InputStream inputStream) {
        packageRepository.getPackage(version.getUserId(), version.getPackageRef()).orElseThrow(() ->
                new VersionCreationFailedException(version, new PackageNotFoundException(version.getUserId(), version.getPackageRef()))
        );
        return versionRepository.createVersion(version, inputStream);
    }

    public Version getVersion(String userId, String packageRef, String id) {
        return versionRepository.getVersion(userId, packageRef, id).orElseThrow(() ->
                new VersionNotFoundException(userId, packageRef, id)
        );
    }

    public Page<Version> getVersions(String userId, String packageRef, Pageable pageable) {
        return versionRepository.getVersions(userId, packageRef, pageable);
    }
}
