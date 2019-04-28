/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.journalq.security.impl;

import com.jd.journalq.exception.JournalqCode;
import com.jd.journalq.exception.JournalqException;
import com.jd.journalq.response.BooleanResponse;
import com.jd.journalq.security.Authentication;
import com.jd.journalq.security.PasswordEncoder;
import com.jd.journalq.security.UserDetails;
import com.jd.journalq.toolkit.security.Encrypt;
import com.jd.journalq.toolkit.security.Md5;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * @author majun8
 */
@Deprecated
public class DefaultAuthentication implements Authentication {

    private static DefaultPasswordEncoder defaultPasswordEncoder = new DefaultPasswordEncoder();
    // 管理员用户
    private String adminUser;
    // 管理员密码
    private String adminPassword;
    // 用户
    private Map<String, String> users;
    private String tokenPrefix = "";

    public DefaultAuthentication() {
    }

    public DefaultAuthentication(String adminUser, String adminPassword) {
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    public DefaultAuthentication(String adminUser, String adminPassword, String prefix) {
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.tokenPrefix = prefix;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public Map<String, String> getUsers() {
        return users;
    }

    public void setUsers(Map<String, String> users) {
        this.users = users;
    }

    /**
     * 根据用户名产生密码
     *
     * @param user
     * @return
     */
    public static String createPassword(final String user) throws JournalqException {
        try {
            // 构造用户名，不足32位则右填充‘0’
            StringBuilder builder = new StringBuilder(32).append(user == null ? "" : user);
            int length = builder.length();
            if (length < 32) {
                for (int i = 0; i < 32 - length; i++) {
                    builder.append('0');
                }
            }
            // 用户名的MD5
            String source = builder.toString();
            try {
                source = Encrypt.encrypt(source, Encrypt.DEFAULT_KEY, Md5.INSTANCE);
            } catch (NoSuchAlgorithmException ignored) {
            }
            // 取0,4,8,12,16,20,24,28位字符
            builder.delete(0, builder.length());
            length = source.length() / 4;
            for (int i = 0; i < length; i++) {
                builder.append(source.charAt(i * 4));
            }
            source = builder.toString();
            return source;
        } catch (Exception e) {
            throw new JournalqException(JournalqCode.CN_AUTHENTICATION_ERROR);
        }

    }
    @Override
    public UserDetails getUser(final String user) throws JournalqException {
        boolean admin = false;
        String password;
        if (adminUser != null && adminUser.equalsIgnoreCase(user)) {
            password = adminPassword;
            admin = true;
        } else if (users != null && users.containsKey(user)) {
            // 兼容原有密码
            password = users.get(user);
        } else {
            // 原始密码
            password = createPassword(tokenPrefix + user);
        }

        // 加密
        password = defaultPasswordEncoder.encode(password);

        return new DefaultUserDetail(user, password, admin);
    }

    @Override
    public PasswordEncoder getPasswordEncode() {
        return defaultPasswordEncoder;
    }

    @Override
    public BooleanResponse auth(String userName, String password) {
        return null;
    }

    @Override
    public BooleanResponse auth(String userName, String password, boolean checkAdmin) {
        return null;
    }

    @Override
    public boolean isAdmin(String userName) {
        return false;
    }
}
