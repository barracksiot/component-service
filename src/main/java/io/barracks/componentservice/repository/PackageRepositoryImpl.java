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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class PackageRepositoryImpl implements PackageRepositoryCustom {
    private static final String USER_ID_KEY = "userId";
    private final MongoOperations mongoOperations;

    @Autowired
    public PackageRepositoryImpl(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public Package createPackage(Package aPackage) {
        try {
            mongoOperations.insert(aPackage);
            return aPackage;
        } catch (DuplicateKeyException dke) {
            throw new DuplicatePackageException(aPackage, dke);
        }
    }

    @Override
    public Optional<Package> getPackage(String userId, String reference) {
        return Optional.ofNullable(mongoOperations.findOne(Query.query(where("userId").is(userId).and("reference").is(reference)), Package.class));
    }

    @Override
    public Page<Package> getPackage(String userId, Pageable pageable) {
        final Query query = query(where(USER_ID_KEY).is(userId));

        final long count = mongoOperations.count(query, Package.class);
        final List<Package> aPackages = mongoOperations.find(query.with(pageable), Package.class);
        return new PageImpl<>(aPackages, pageable, count);
    }
}
