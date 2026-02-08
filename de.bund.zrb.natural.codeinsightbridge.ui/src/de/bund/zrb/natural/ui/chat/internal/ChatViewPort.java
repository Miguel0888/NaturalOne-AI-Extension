package de.bund.zrb.natural.ui.chat.internal;

import java.time.Duration;

/**
 * Offer view operations to the presenter.
 */
public interface ChatViewPort {

    enum NotificationType {
        INFO, WARNING, ERROR
    }

    void clearChatView();

    void clearUserInput();

    void appendMessage(String messageId, String role);

    void setMessageHtml(String messageId, String htmlOrMarkdown);

    void removeMessage(String messageId);

    void showNotification(String message, Duration duration, NotificationType type);
}
