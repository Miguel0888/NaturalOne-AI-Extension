package de.bund.zrb.natural.ui.chat.internal;

/**
 * Handle UI events coming from the chat view.
 */
public interface ChatPresenter {

    void onViewVisible();

    void onSendUserMessage(String text);

    void onReplayLastMessage();

    void onClear();

    void onStop();

    void onChatModelSelected(String modelId);

    void onCopyCode(String code);

    void onInsertCode(String code);

    void onDiffCode(String code);

    void onApplyPatch(String code);

    void onNewFile(String code, String lang);

    void onRemoveMessage(String messageId);
}
