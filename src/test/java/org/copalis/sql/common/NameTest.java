/*
 *  Copyright 2011 Giles Burgess
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
package org.copalis.sql.common;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author gilesjb
 *
 */
public class NameTest extends TestCase {
    
    public interface Iface {
        void a() throws IOException;
    }

    public void testMethodName() throws SecurityException, NoSuchMethodException {
        assertEquals("org.copalis.sql.common.NameTest$Iface.a()", Name.of(Iface.class.getMethod("a")));
    }
}