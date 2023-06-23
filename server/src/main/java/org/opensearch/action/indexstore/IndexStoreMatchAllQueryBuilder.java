/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.index.query.MatchAllQueryBuilder;

import java.io.IOException;

public class IndexStoreMatchAllQueryBuilder extends MatchAllQueryBuilder {

    public IndexStoreMatchAllQueryBuilder() {}

    public IndexStoreMatchAllQueryBuilder(StreamInput in) throws IOException {
        super(in);
    }
}
