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

import static org.opensearch.action.ValidateActions.addValidationError;

public class IndexStoreRequest extends ActionRequest {

    private String index;

    private String type;

    private String format;

    public IndexStoreRequest(String index, String type) {
        this.index = index;
        this.type = type;
    }

    public IndexStoreRequest(String index, String type, String format) {
        this.index = index;
        this.type = type;
        this.format = format;
    }

    public IndexStoreRequest(StreamInput in) throws IOException {
        super(in);
        this.index = in.readString();
        this.type = in.readString();
        this.format = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(index);
        out.writeString(type);
        out.writeString(format);
    }

    public String getType() {
        return type;
    }

    public String getFormat() {
        return format;
    }

    public String getIndex() {
        return index;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if(index == null || index.trim().length() == 0) {
            validationException = addValidationError("Index attribute is missing", validationException);
            return validationException;
        }

        if(type == null || type.trim().length() == 0) {
            validationException = addValidationError("Type attribute is missing", validationException);
            return validationException;
        }

        if("S3".equalsIgnoreCase(type) && ( format == null || format.trim().length() == 0)) {
            validationException = addValidationError("Type is S3. Format is needed ", validationException);
            return validationException;
        }

        if(!("S3".equalsIgnoreCase(type) || "Redshift".equalsIgnoreCase(type))) {
            validationException = addValidationError("Type should be Redshift/S3.", validationException);
            return validationException;
        }
        return null;
    }
}
