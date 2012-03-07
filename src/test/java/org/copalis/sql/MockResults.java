/*
 *  Copyright 2012 Giles Burgess
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.copalis.sql;

import java.sql.ResultSet;

/**
 * A base concrete implementation of Results
 * @author gilesjb
 */
public class MockResults implements Results, Results.Updatable {

    public void updateRow() throws DataException {
        throw new UnsupportedOperationException();
    }

    public ResultSet results() {
        throw new UnsupportedOperationException();
    }

    public boolean next() throws DataException {
        throw new UnsupportedOperationException();
    }

    public void close() throws DataException {
        throw new UnsupportedOperationException();
    }
}
