package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private UserStorage userStorage = new UserStorage();

    // [THEM MOI 2026-08-22 - co ho tro cua AI] Cau hinh cho QueryRewriteService.
    private QueryRewrite queryRewrite = new QueryRewrite();

    public UserStorage getUserStorage() {
        return userStorage;
    }

    public void setUserStorage(UserStorage userStorage) {

        this.userStorage = userStorage;
    }

    public QueryRewrite getQueryRewrite() {
        return queryRewrite;
    }

    public void setQueryRewrite(QueryRewrite queryRewrite) {
        this.queryRewrite = queryRewrite;
    }

    /**
     * [THEM MOI 2026-08-22 - co ho tro cua AI]
     * Anh xa khoi `rag.query-rewrite` trong application.yaml.
     */
    public static class QueryRewrite {

        // MAC DINH true: khong bat thi tinh nang coi nhu khong ton tai.
        // Dat ve false qua RAG_QUERY_REWRITE_ENABLED de so sanh A/B ma khong can build lai.
        private boolean enabled = true;

        // So tin nhan gan nhat dua vao prompt viet lai. CO Y de 3 chu khong phai 5 nhu
        // ChatConversationMemoryService.MAX_MEMORY_MESSAGES: cang nhieu lich su thi model
        // cang de viet lai lech sang chu de cu.
        private int maxMemoryMessages = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxMemoryMessages() {
            return maxMemoryMessages;
        }

        public void setMaxMemoryMessages(int maxMemoryMessages) {
            this.maxMemoryMessages = maxMemoryMessages;
        }
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
