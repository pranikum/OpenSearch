/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.cluster.metadata;

import org.opensearch.cluster.decommission.DecommissionedAttribute;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class DecommissionedAttributeMetadata implements Writeable {

    private final String name;
    private final DecommissionedAttribute decommissionedAttribute;

    public DecommissionedAttributeMetadata(DecommissionedAttributeMetadata metadata, DecommissionedAttribute decommissionedAttribute) {
        this(metadata.name, decommissionedAttribute);
    }

    /**
     * Constructs new decommissioned attribute metadata
     *
     * @param name                  attribute name
     * @param decommissionedAttribute attribute value
     */
    public DecommissionedAttributeMetadata(String name, DecommissionedAttribute decommissionedAttribute) {
        this.name = name;
        this.decommissionedAttribute = decommissionedAttribute;
    }

    public DecommissionedAttributeMetadata(String name, String key, List<String> values) {
        this(name, new DecommissionedAttribute(key, values));
    }

    /**
     * Returns attribute name
     *
     * @return attribute name
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns attribute value
     *
     * @return attribute value
     */
    public DecommissionedAttribute decommissionedAttribute() {
        return this.decommissionedAttribute;
    }

    public DecommissionedAttributeMetadata(StreamInput in) throws IOException {
        name = in.readString();
        decommissionedAttribute = new DecommissionedAttribute(in);
    }

    /**
     * Writes decommissioned attribute metadata to stream output
     *
     * @param out stream output
     */
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        decommissionedAttribute.writeTo(out);
    }

    /**
     * Checks if this instance is equal to the other instance in name other than {@link #decommissionedAttribute}.
     *
     * @param other other decommissioned attribute metadata
     * @return {@code true} if both instances equal in all fields but the values fields
     */
    public boolean equalsIgnoreValues(DecommissionedAttributeMetadata other) {
        return name.equals(other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecommissionedAttributeMetadata that = (DecommissionedAttributeMetadata) o;

        if (!name.equals(that.name)) return false;
        return decommissionedAttribute.equals(that.decommissionedAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, decommissionedAttribute);
    }

    @Override
    public String toString() {
        return "DecommissionedAttributeMetadata{" + name + "}{" + decommissionedAttribute().toString() + "}";
    }
}
