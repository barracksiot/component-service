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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.componentservice.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class PackageJsonTests {
    @Autowired
    private JacksonTester<Package> json;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void deserializeJson_shouldIgnoreIdAndUserId() throws Exception {
        // Given
        final JsonNode jsonNode = objectMapper.readTree(new ClassPathResource("package.json", getClass()).getInputStream());
        final Package expected = Package.builder()
                .name(jsonNode.get("name").textValue())
                .description(jsonNode.get("description").textValue())
                .reference(jsonNode.get("reference").textValue())
                .build();

        // When
        final Package result = json.parseObject(jsonNode.toString());

        // Then
        assertThat(expected).hasNoNullFieldsOrPropertiesExcept("id", "userId");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serializeJson_shouldSerializeAllFieldsExceptIdAndUserId() throws IOException {
        // Given
        final Package aPackage = PackageUtils.getPackage();

        // When
        JsonContent<Package> result = json.write(aPackage);

        // Then
        assertThat(aPackage).hasNoNullFieldsOrProperties();
        assertThat(result).doesNotHaveJsonPathValue("@.id");
        assertThat(result).doesNotHaveJsonPathValue("@.userId");
        assertThat(result).extractingJsonPathStringValue("@.name").isEqualTo(aPackage.getName());
        assertThat(result).extractingJsonPathStringValue("@.reference").isEqualTo(aPackage.getReference());
        assertThat(result).extractingJsonPathStringValue("@.description").isEqualTo(aPackage.getDescription());
    }

}
