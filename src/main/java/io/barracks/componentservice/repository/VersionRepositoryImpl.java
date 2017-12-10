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

import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.gridfs.GridFSDBFile;
import io.barracks.componentservice.model.Version;
import io.barracks.componentservice.repository.documents.VersionDocument;
import io.barracks.componentservice.repository.exception.DuplicateVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class VersionRepositoryImpl implements VersionRepositoryCustom {
    private static final String USER_ID_KEY = "metadata.userId";
    private static final String PACKAGE_REF_KEY = "metadata.packageRef";
    private static final String VERSION_ID_KEY = "metadata.versionId";
    private final GridFsOperations gridFsOperations;
    private final MongoOperations mongoOperations;

    @Autowired
    public VersionRepositoryImpl(MongoDbFactory mongoDbFactory, MongoOperations mongoOperations) {
        this.gridFsOperations = new GridFsTemplate(mongoDbFactory, mongoOperations.getConverter(), VersionDocument.class.getAnnotation(Document.class).collection().replace(".files", ""));
        this.mongoOperations = mongoOperations;
    }

    @Override
    public Version createVersion(Version version, InputStream file) {
        VersionDocument.Metadata metadata = VersionDocument.Metadata.builder()
                .userId(version.getUserId())
                .packageRef(version.getPackageRef())
                .versionId(version.getId())
                .metadata(version.getMetadata())
                .name(version.getName())
                .description(version.getDescription())
                .build();
        try {
            return Optional.ofNullable(this.gridFsOperations.store(file, version.getFilename(), metadata))
                    .flatMap(saved -> getVersion(version.getUserId(), version.getPackageRef(), version.getId()))
                    .orElseThrow(() -> new RuntimeException("Version returned null object, this should not happen!"));
        } catch (DuplicateKeyException dke) {
            throw new DuplicateVersionException(version, dke);
        }
    }

    @Override
    public Optional<Version> getVersion(String userId, String packageRef, String versionId) {
        final Query query = Query.query(where(USER_ID_KEY).is(userId).and(PACKAGE_REF_KEY).is(packageRef).and(VERSION_ID_KEY).is(versionId));
        return Optional.ofNullable(this.gridFsOperations.findOne(query))
                .map(this::fileToVersion);
    }

    @Override
    public Page<Version> getVersions(String userId, String packageRef, Pageable pageable) {
        final Query query = query(where(USER_ID_KEY).is(userId).and(PACKAGE_REF_KEY).is(packageRef)).with(pageable);

        final List<Version> versions = this.mongoOperations.find(query, DBObject.class, VersionDocument.class.getAnnotation(Document.class).collection())
                .stream()
                .map(this::dbObjectToVersion)
                .collect(Collectors.toList());
        final long count = mongoOperations.count(query, Version.class);
        return new PageImpl<>(versions, pageable, count);
    }

    private Version dbObjectToVersion(DBObject dbObject) {
        GridFSDBFile file = new GridFSDBFile();
        dbObject.keySet().forEach(key -> file.put(key, dbObject.get(key)));
        return fileToVersion(file);
    }

    private Version fileToVersion(GridFSDBFile file) {
        VersionDocument.Metadata metadata = mongoOperations.getConverter().read(VersionDocument.Metadata.class, file.getMetaData());
        return Version.builder()
                .filename(file.getFilename())
                .length(file.getLength())
                .md5(file.getMD5())
                .userId(metadata.getUserId())
                .packageRef(metadata.getPackageRef())
                .id(metadata.getVersionId())
                .metadata(metadata.getMetadata())
                .name(metadata.getName())
                .description(metadata.getDescription())
                .inputStream(file.getInputStream())
                .build();
    }
}
