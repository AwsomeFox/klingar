/*
 * Copyright (C) 2016 Simon Norberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.awsomefox.sprocket.data;

import com.awsomefox.sprocket.data.api.AuthInterceptor;
import com.awsomefox.sprocket.util.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LoginManager {

  private static final String PREF_AUTH_TOKEN = "pref_auth_token";

  private final AuthInterceptor authInterceptor;
  private final Prefs prefs;
  private String authToken;

  @Inject LoginManager(AuthInterceptor authInterceptor, Prefs prefs) {
    this.authInterceptor = authInterceptor;
    this.prefs = prefs;
    setAuthToken(prefs.getString(PREF_AUTH_TOKEN, null));
  }

  public void login(String authToken) {
    setAuthToken(authToken);
    prefs.putString(PREF_AUTH_TOKEN, authToken);
  }

  public void logout() {
    setAuthToken(null);
    prefs.remove(PREF_AUTH_TOKEN);
  }

  public boolean isLoggedOut() {
    return Strings.isBlank(authToken);
  }

  private void setAuthToken(String authToken) {
    this.authToken = authToken;
    authInterceptor.setAuthToken(authToken);
  }
}
