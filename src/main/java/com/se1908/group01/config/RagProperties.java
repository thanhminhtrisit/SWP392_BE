package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private UserStorage userStorage = new UserStorage();

    public UserStorage getUserStorage() {
        return userStorage;
    }

    public void setUserStorage(UserStorage userStorage) {

        this.userStorage = userStorage;
    }

    public static class UserStorage {

        private boolean allowGeneralKnowledge = false;

        public boolean isAllowGeneralKnowledge() {
            return allowGeneralKnowledge;
        }

        public void setAllowGeneralKnowledge(boolean allowGeneralKnowledge) {
            this.allowGeneralKnowledge = allowGeneralKnowledge;
        }
    }
}
