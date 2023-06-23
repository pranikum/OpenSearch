/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.cluster.decommission.DecommissionAttribute;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class IndexStoreRequest extends ActionRequest {

    public String getIndex() {
        return index;
    }

    private String index;

    public IndexStoreRequest(String index) {
        this.index = index;
    }

    public IndexStoreRequest(StreamInput in) throws IOException {
        super(in);
        this.index = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(index);
    }

    @Override
    public ActionRequestValidationException validate() {

        return null;
    }
}
