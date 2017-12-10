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

package io.barracks.componentservice.model;

import io.barracks.componentservice.utils.PackageUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void whenNameNull_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().name(null).build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenNameBlank_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().name("").build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenNameTooLong_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().name(generateStringWithSize(141)).build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenDescriptionTooLong_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().description(generateStringWithSize(1001)).build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenReferenceNull_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().reference(null).build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenReferenceBlank_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().reference("").build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenReferenceNonAscii_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().reference("Ó˝ƒ†").build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void whenReferenceTooLong_shouldNotValidate() {
        // Given
        final Package aPackage = getPredefinedPackageBuilder().reference(generateStringWithSize(141)).build();

        // When
        final Set<ConstraintViolation<Package>> violations = validator.validate(aPackage);

        // Then
        assertThat(violations).isNotEmpty();
    }

    private Package.PackageBuilder getPredefinedPackageBuilder() {
        return PackageUtils.getPackage().toBuilder();
    }

    private String generateStringWithSize(long size) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            stringBuilder.append("q");
        }
        return stringBuilder.toString();
    }
}
