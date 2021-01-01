/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.avatica.server;

import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

/**
 * Implementation of UserStore which creates users when they do not already exist.
 */
public class AvaticaUserStore extends UserStore {
  private static final Credential USER_CREDENTIAL = Credential.getCredential("");

  public static final String AVATICA_USER_ROLE = "avatica-user";

  private static final String[] USER_ROLES = new String[] {AVATICA_USER_ROLE};

  @Override public UserIdentity getUserIdentity(String userName) {
    UserIdentity userId = super.getUserIdentity(userName);
    if (userId != null) {
      return userId;
    }

    // Do we need to be concerned about the recursion?
    addUser(userName, USER_CREDENTIAL, USER_ROLES);
    return getUserIdentity(userName);
  }
}

// End AvaticaUserStore.java
