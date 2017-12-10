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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PackageManager {

    private final PackageRepository packageRepository;

    public PackageManager(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public Package createPackage(Package aPackage) {
        return packageRepository.createPackage(aPackage);
    }

    public Page<Package> getPackages(String userId, Pageable pageable) {
        return packageRepository.getPackage(userId, pageable);
    }

    public Package getPackage(String userId, String reference) {
        return packageRepository.getPackage(userId, reference).orElseThrow(() -> new PackageNotFoundException(userId, reference));
    }
}
