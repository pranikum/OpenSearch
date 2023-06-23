/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration.migrator.redshift;

import java.sql.Connection;
import java.sql.DriverManager;

public class RedshiftConnector {

    public void getConnection() {
        Connection conn1 = null;
        try {
            Class.forName("com.amazon.redshift.jdbc42.Driver");
            conn1 = DriverManager.getConnection("jdbc:redshift://redshift-dc2-test.ctzrqaulg0u6.us-east-1.redshift.amazonaws.com:5439/testdb", "awsuser","Awsuser1234");
            if (conn1 != null) {
                System.out.println("Connected with connection #1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
