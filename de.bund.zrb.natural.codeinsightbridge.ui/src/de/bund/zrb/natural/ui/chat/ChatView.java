package de.bund.zrb.natural.ui.chat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.bund.zrb.natural.ui.chat.internal.BundleResourceReader;
import de.bund.zrb.natural.ui.chat.internal.ChatPresenter;
import de.bund.zrb.natural.ui.chat.internal.ChatViewPort;
import de.bund.zrb.natural.ui.chat.internal.DummyChatPresenter;
import de.bund.zrb.natural.ui.chat.internal.EmbeddedFontCssBuilder;
import de.bund.zrb.natural.ui.chat.internal.MiniMarkdownParser;

/**
 * Provide an AssistAI-like chat UI (click dummy) without any real backend.
 */
public final class ChatView extends ViewPart implements ChatViewPort {

    public static final String VIEW_ID = "de.bund.zrb.natural.codeinsightbridge.ui.chatView";

    private final BundleResourceReader resourceReader;
    private final EmbeddedFontCssBuilder fontCssBuilder;
    private final MiniMarkdownParser markdown;
    private final ChatPresenter presenter;

    private Browser browser;
    private Text inputArea;

    private ScrolledComposite scrolledComposite;
    private Composite imagesContainer;

    private ToolBar actionToolBar;
    private ToolItem modelDropdownItem;
    private Menu modelMenu;

    private boolean autoScrollEnabled;
    private int notificationIdCounter;

    private final Map<String, String> autocompleteModel;
    private final List<ImageAttachment> attachments;

    private final List<ModelOption> models;
    private String selectedModelId;

    private final Map<String, Image> iconCache;

    public ChatView() {
        this.resourceReader = new BundleResourceReader(ChatView.class);
        this.fontCssBuilder = new EmbeddedFontCssBuilder(resourceReader);
        this.markdown = new MiniMarkdownParser();
        this.presenter = new DummyChatPresenter(this, markdown);
        this.autocompleteModel = new LinkedHashMap<String, String>();
        this.attachments = new ArrayList<ImageAttachment>();
        this.models = createDefaultModels();
        this.selectedModelId = "assistai";
        this.autoScrollEnabled = true;
        this.notificationIdCounter = 0;
        this.iconCache = new LinkedHashMap<String, Image>();

        // Provide a few dummy slash commands for the autocomplete UI.
        this.autocompleteModel.put("help", "Show available dummy commands");
        this.autocompleteModel.put("clear", "Clear the conversation");
        this.autocompleteModel.put("model", "Show model selector hint");
    }

    @Override
    public void createPartControl(Composite parent) {
        SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite browserContainer = new Composite(sashForm, SWT.NONE);
        browserContainer.setLayout(new FillLayout());

        browser = new Browser(browserContainer, SWT.NONE);
        initializeFunctions(browser);
        initializeChatView(browser);

        Composite controls = new Composite(sashForm, SWT.NONE);
        GridLayout controlsLayout = new GridLayout(1, false);
        controlsLayout.marginWidth = 5;
        controlsLayout.marginHeight = 5;
        controls.setLayout(controlsLayout);

        Composite attachmentsPanel = createAttachmentsPanel(controls);
        attachmentsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Composite inputContainer = new Composite(controls, SWT.NONE);
        GridLayout inputLayout = new GridLayout(1, false);
        inputLayout.marginWidth = 0;
        inputLayout.marginHeight = 0;
        inputLayout.horizontalSpacing = 5;
        inputContainer.setLayout(inputLayout);
        inputContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        inputArea = createUserInput(inputContainer);
        inputArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        setupAutocomplete(inputArea);

        Composite buttonBar = new Composite(controls, SWT.NONE);
        GridLayout buttonBarLayout = new GridLayout(1, false);
        buttonBarLayout.marginHeight = 0;
        buttonBarLayout.marginWidth = 0;
        buttonBar.setLayout(buttonBarLayout);
        buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        actionToolBar = new ToolBar(buttonBar, SWT.FLAT | SWT.RIGHT);
        actionToolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        modelDropdownItem = createModelSelector(actionToolBar);
        createAttachmentToolItem(actionToolBar);
        createReplayToolItem(actionToolBar);
        createClearChatToolItem(actionToolBar);
        createStopToolItem(actionToolBar);
        createSendToolItem(actionToolBar);

        sashForm.setWeights(new int[] { 70, 30 });

        // Build model menu and select the default model.
        setAvailableModels(models, selectedModelId);
        clearAttachments();

        String welcomeId = "welcome-message";
        appendMessage(welcomeId, "assistant");
        setMessageHtml(welcomeId, "Welcome! This is an AssistAI-like UI dummy.\n\nType something and press Ctrl+Enter.");
    }

    @Override
    public void setFocus() {
        if (inputArea != null && !inputArea.isDisposed()) {
            inputArea.setFocus();
        }
        presenter.onViewVisible();
    }



    private void initializeFunctions(Browser b) {
        new CopyCodeFunction(b, "eclipseFunc");
        new CopyCodeFunction(b, "eclipseCopyCode");
        new ApplyPatchFunction(b, "eclipseApplyPatch");
        new DiffCodeFunction(b, "eclipseDiffCode");
        new InsertCodeFunction(b, "eclipseInsertCode");
        new NewFileFunction(b, "eclipseNewFile");
        new ScrollInteractionFunction(b, "eclipseScrollInteraction");
        new RemoveMessageFunction(b, "eclipseRemoveMessage");
    }

    private void initializeChatView(Browser b) {
        String css = loadCss();
        String js = loadJavaScripts();
        String fonts = fontCssBuilder.buildCss();

        String html = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<style>" + css + "</style>"
                + "<style>" + fonts + "</style>"
                + "<script>" + js + "</script>"
                + "</head>"
                + "<body>"
                + "<div id=\"notification-container\"></div>"
                + "<div id=\"content\"></div>"
                + "</body>"
                + "</html>";

        b.setText(html);
    }

    private String loadCss() {
        String[] cssFiles = new String[] { "textview.css", "dark.min.css", "fa6.all.min.css", "katex.min.css" };
        StringBuilder sb = new StringBuilder();
        for (String f : cssFiles) {
            sb.append(resourceReader.readUtf8("css/" + f)).append("\n");
        }
        return sb.toString();
    }

    private String loadJavaScripts() {
        String[] jsFiles = new String[] { "highlight.min.js", "textview.js", "katex.min.js" };
        StringBuilder sb = new StringBuilder();
        for (String f : jsFiles) {
            sb.append(resourceReader.readUtf8("js/" + f)).append("\n\n");
        }
        return sb.toString();
    }

    private Composite createAttachmentsPanel(Composite parent) {
        Composite attachmentsPanel = new Composite(parent, SWT.NONE);
        attachmentsPanel.setLayout(new GridLayout(1, false));

        scrolledComposite = new ScrolledComposite(attachmentsPanel, SWT.H_SCROLL | SWT.V_SCROLL);
        GridData scrolledData = new GridData(SWT.FILL, SWT.FILL, true, false);
        scrolledData.heightHint = 0;
        scrolledComposite.setLayoutData(scrolledData);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        imagesContainer = new Composite(scrolledComposite, SWT.NONE);
        RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.spacing = 8;
        rowLayout.marginTop = 4;
        rowLayout.marginBottom = 4;
        rowLayout.marginLeft = 4;
        rowLayout.marginRight = 4;
        rowLayout.wrap = true;
        imagesContainer.setLayout(rowLayout);

        scrolledComposite.setContent(imagesContainer);
        scrolledComposite.setMinSize(imagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        imagesContainer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL || e.keyCode == SWT.BS) {
                    removeSelectedAttachments();
                }
            }
        });

        return attachmentsPanel;
    }

    private void removeSelectedAttachments() {
        List<Integer> indices = new ArrayList<Integer>();
        for (org.eclipse.swt.widgets.Control child : imagesContainer.getChildren()) {
            if (child instanceof Label) {
                Boolean selected = (Boolean) child.getData("selected");
                if (selected != null && selected.booleanValue()) {
                    Integer idx = (Integer) child.getData("attachmentIndex");
                    if (idx != null) {
                        indices.add(idx);
                    }
                }
            }
        }

        Collections.sort(indices, Collections.reverseOrder());
        for (Integer idx : indices) {
            if (idx.intValue() >= 0 && idx.intValue() < attachments.size()) {
                attachments.remove(idx.intValue());
            }
        }
        renderAttachments();
    }

    private ToolItem createAttachmentToolItem(ToolBar toolbar) {
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        Image attachIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD);
        item.setImage(attachIcon);
        item.setToolTipText("Add image attachment");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openImageDialogAndAttach();
            }
        });
        return item;
    }

    private void openImageDialogAndAttach() {
        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.MULTI);
        dialog.setText("Select images");
        dialog.setFilterExtensions(new String[] { "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp" });
        String first = dialog.open();
        if (first == null) {
            return;
        }
        String dir = dialog.getFilterPath();
        String[] names = dialog.getFileNames();
        for (String name : names) {
            String path = dir + System.getProperty("file.separator") + name;
            try {
                ImageData img = new ImageData(path);
                attachments.add(new ImageAttachment(img, name));
            } catch (Exception ex) {
                showNotification("Cannot load image: " + name, Duration.ofSeconds(4), NotificationType.ERROR);
            }
        }
        renderAttachments();
    }

    private ToolItem createSendToolItem(ToolBar toolbar) {
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        Image sendIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD);
        item.setImage(sendIcon);
        item.setToolTipText("Send message (Ctrl+Enter)");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendIfNotEmpty();
            }
        });
        return item;
    }

    private ToolItem createReplayToolItem(ToolBar toolbar) {
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        Image replayIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_REDO);
        item.setImage(replayIcon);
        item.setToolTipText("Regenerate response");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presenter.onReplayLastMessage();
            }
        });
        return item;
    }

    private ToolItem createClearChatToolItem(ToolBar toolbar) {
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        Image clearIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_CLEAR);
        item.setImage(clearIcon);
        item.setToolTipText("Clear conversation");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presenter.onClear();
                clearAttachments();
            }
        });
        return item;
    }

    private ToolItem createStopToolItem(ToolBar toolbar) {
        ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        Image stopIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP);
        item.setImage(stopIcon);
        item.setToolTipText("Stop generation");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presenter.onStop();
            }
        });
        return item;
    }

    private ToolItem createModelSelector(ToolBar toolbar) {
        ToolItem item = new ToolItem(toolbar, SWT.DROP_DOWN);
        item.setImage(loadIcon("assistai-16.png"));
        item.setText("AssistAI");
        item.setToolTipText("Select AI Model");
        return item;
    }

    private Text createUserInput(Composite parent) {
        Text t = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        t.setMessage("Type a message or question here... (Press Ctrl+Enter to send)");
        t.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR && (e.stateMask & SWT.CTRL) != 0) {
                    e.doit = false;
                    sendIfNotEmpty();
                }
            }
        });
        return t;
    }

    private void sendIfNotEmpty() {
        String text = inputArea.getText();
        if (text == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.startsWith("/")) {
            handleSlashCommand(trimmed);
            return;
        }
        presenter.onSendUserMessage(trimmed);
    }

    private void handleSlashCommand(String trimmed) {
        String cmd = trimmed.substring(1).trim();
        if ("clear".equalsIgnoreCase(cmd)) {
            presenter.onClear();
            clearAttachments();
            clearUserInput();
            return;
        }
        if ("help".equalsIgnoreCase(cmd)) {
            String msg = "Available dummy commands:\n\n" + "- /help\n" + "- /clear\n" + "- /model";
            String id = UUID.randomUUID().toString();
            appendMessage(id, "assistant");
            setMessageHtml(id, msg);
            clearUserInput();
            return;
        }
        if ("model".equalsIgnoreCase(cmd)) {
            showNotification("Use the dropdown next to the send button to select a model.", Duration.ofSeconds(4), NotificationType.INFO);
            clearUserInput();
            return;
        }
        showNotification("Unknown command: " + trimmed, Duration.ofSeconds(4), NotificationType.WARNING);
        clearUserInput();
    }

    private void setupAutocomplete(Text textField) {
        IContentProposalProvider provider = new IContentProposalProvider() {
            @Override
            public IContentProposal[] getProposals(String contents, int position) {
                if (contents == null || !contents.startsWith("/")) {
                    return new IContentProposal[0];
                }
                String prefix = contents.substring(1);
                List<String> matches = new ArrayList<String>();
                for (String key : autocompleteModel.keySet()) {
                    if (key.startsWith(prefix)) {
                        matches.add(key);
                    }
                }
                IContentProposal[] proposals = new IContentProposal[matches.size()];
                for (int i = 0; i < matches.size(); i++) {
                    proposals[i] = new SimpleCommandProposal(matches.get(i), autocompleteModel.get(matches.get(i)));
                }
                return proposals;
            }
        };

        ContentProposalAdapter adapter = new ContentProposalAdapter(textField, new TextContentAdapter(), provider, null, null);
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    }

    private void setAvailableModels(final List<ModelOption> availableModels, final String selectedId) {
        selectedModelId = selectedId;
        ModelOption selected = null;
        for (ModelOption m : availableModels) {
            if (m.id.equals(selectedId)) {
                selected = m;
                break;
            }
        }
        if (selected == null && !availableModels.isEmpty()) {
            selected = availableModels.get(0);
        }
        if (selected != null) {
            modelDropdownItem.setText(selected.displayName);
            modelDropdownItem.setImage(loadIcon(selected.iconFile));
            updateLayout(actionToolBar);
        }

        if (modelMenu != null && !modelMenu.isDisposed()) {
            modelMenu.dispose();
        }
        modelMenu = new Menu(actionToolBar.getShell(), SWT.POP_UP);

        modelDropdownItem.addListener(SWT.Selection, event -> {
            if (event.detail == SWT.ARROW) {
                for (MenuItem item : modelMenu.getItems()) {
                    item.dispose();
                }
                for (final ModelOption m : availableModels) {
                    MenuItem mi = new MenuItem(modelMenu, SWT.CHECK);
                    mi.setText(m.displayName);
                    mi.setSelection(m.id.equals(selectedModelId));
                    mi.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            selectModel(m);
                        }
                    });
                }
                Rectangle rect = modelDropdownItem.getBounds();
                Point pt = new Point(rect.x, rect.y + rect.height);
                pt = actionToolBar.toDisplay(pt);
                modelMenu.setLocation(pt.x, pt.y);
                modelMenu.setVisible(true);
            }
        });
    }

    private void selectModel(ModelOption m) {
        if (m == null) {
            return;
        }
        this.selectedModelId = m.id;
        modelDropdownItem.setText(m.displayName);
        modelDropdownItem.setImage(loadIcon(m.iconFile));
        updateLayout(actionToolBar);
        presenter.onChatModelSelected(m.id);
    }

    private void clearAttachments() {
        attachments.clear();
        renderAttachments();
    }

    private void renderAttachments() {
        if (imagesContainer == null || imagesContainer.isDisposed()) {
            return;
        }

        for (org.eclipse.swt.widgets.Control child : imagesContainer.getChildren()) {
            child.dispose();
        }

        if (attachments.isEmpty()) {
            scrolledComposite.setVisible(false);
            ((GridData) scrolledComposite.getLayoutData()).heightHint = 0;
            updateLayout(imagesContainer);
            return;
        }

        AttachmentRenderer renderer = new AttachmentRenderer();
        for (ImageAttachment a : attachments) {
            renderer.add(a.image, a.caption);
        }

        scrolledComposite.setVisible(true);

        int thumbnailSize = 96;
        int spacing = 8;
        int margin = 8;
        int maxHeight = (thumbnailSize + spacing) * 2 + margin;

        Point size = imagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        imagesContainer.setSize(size);
        scrolledComposite.setMinSize(size);
        ((GridData) scrolledComposite.getLayoutData()).heightHint = Math.min(size.y + margin, maxHeight);

        updateLayout(imagesContainer);
    }

    private static List<ModelOption> createDefaultModels() {
        return Arrays.asList(
                new ModelOption("assistai", "AssistAI", "assistai-16.png"),
                new ModelOption("openai", "ChatGPT", "chatgpt-icon-16.png"),
                new ModelOption("anthropic", "Claude", "claude-ai-icon-16.png"),
                new ModelOption("google", "Gemini", "google-gemini-icon-16.png"),
                new ModelOption("grok", "Grok", "grok-icon-16.png"),
                new ModelOption("deepseek", "DeepSeek", "deepseek-logo-icon-16.png")
        );
    }

    private Image loadIcon(String iconFileName) {
        if (iconFileName == null) {
            return null;
        }
        Image cached = iconCache.get(iconFileName);
        if (cached != null && !cached.isDisposed()) {
            return cached;
        }
        try {
            Image img = org.eclipse.jface.resource.ImageDescriptor.createFromFile(ChatView.class, "/icons/" + iconFileName).createImage();
            iconCache.put(iconFileName, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private static void updateLayout(Composite composite) {
        if (composite == null || composite.isDisposed()) {
            return;
        }
        composite.layout();
        if (composite.getParent() != null) {
            updateLayout(composite.getParent());
        }
    }

    // --- ChatViewPort

    @Override
    public void clearChatView() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                initializeChatView(browser);
            }
        });
    }

    @Override
    public void clearUserInput() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (inputArea == null || inputArea.isDisposed()) {
                    return;
                }
                inputArea.setText("");
            }
        });
    }

    @Override
    public void appendMessage(final String messageId, final String role) {
        final String cssClass = "user".equals(role) ? "chat-bubble me" : "chat-bubble you";
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                String id = escapeForJs(messageId);
                String js = "var node = document.createElement('div');"
                        + "node.setAttribute('id', 'message-" + id + "');"
                        + "node.setAttribute('class', '" + cssClass + "');"
                        + "var toolbar = document.createElement('div');"
                        + "toolbar.setAttribute('class', 'message-toolbar');"
                        + "var trash = document.createElement('i');"
                        + "trash.setAttribute('class', 'fa-solid fa-trash');"
                        + "trash.onclick = function() { window.eclipseRemoveMessage('" + id + "'); };"
                        + "toolbar.appendChild(trash);"
                        + "var content = document.createElement('div');"
                        + "content.setAttribute('id', 'message-content-" + id + "');"
                        + "node.appendChild(toolbar);"
                        + "node.appendChild(content);"
                        + "document.getElementById('content').appendChild(node);";
                browser.execute(js);
                if (autoScrollEnabled) {
                    browser.execute("window.scrollTo(0, document.body.scrollHeight);");
                }
            }
        });
    }

    @Override
    public void setMessageHtml(final String messageId, final String htmlOrMarkdown) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }

                String body = markdown.toHtml(htmlOrMarkdown);
                String fixed = escapeForJsStringLiteral(body);
                String id = escapeForJs(messageId);

                String js = "var target = document.getElementById('message-content-" + id + "') || document.getElementById('message-" + id + "');"
                        + "if (target) { target.innerHTML = '" + fixed + "'; } renderCode();";

                browser.execute(js);
                if (autoScrollEnabled) {
                    browser.execute("window.scrollTo(0, document.body.scrollHeight);");
                }
            }
        });
    }

    @Override
    public void removeMessage(final String messageId) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                String id = escapeForJs(messageId);
                browser.execute("var node = document.getElementById('message-" + id + "'); if(node) { node.remove(); }");
            }
        });
    }

    @Override
    public void showNotification(final String message, final Duration duration, final NotificationType type) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                String notificationId = "notification-" + (notificationIdCounter++);
                String icon;
                String bg;
                String fg;

                if (type == NotificationType.ERROR) {
                    icon = "fa-solid fa-circle-xmark";
                    bg = "#ffdddd";
                    fg = "#7a0000";
                } else if (type == NotificationType.WARNING) {
                    icon = "fa-solid fa-triangle-exclamation";
                    bg = "#fff4cc";
                    fg = "#6b4e00";
                } else {
                    icon = "fa-solid fa-circle-info";
                    bg = "#ddf1ff";
                    fg = "#004b7a";
                }

                String safeMsg = escapeForJs(message);
                String js = "showNotification('" + notificationId + "', '" + icon + "', '" + bg + "', '" + fg + "', '" + safeMsg + "');";
                browser.execute(js);

                long ms = duration == null ? 0L : duration.toMillis();
                if (ms > 0L) {
                    browser.execute("setTimeout(function(){ removeNotification('" + notificationId + "'); }, " + ms + ");");
                }
            }
        });
    }

    // --- Utilities

    private static String escapeForJs(String text) {
        if (text == null) {
            return "";
        }
        String s = text;
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("'", "\\'");
        s = s.replace("\r", "");
        return s;
    }

    private static String escapeForJsStringLiteral(String text) {
        if (text == null) {
            return "";
        }
        String s = text;
        s = s.replace("\\", "\\\\");
        s = s.replace("\r", "");
        s = s.replace("\n", "\\n");
        s = s.replace("\t", "\\t");
        s = s.replace("'", "\\'");
        return s;
    }

    // --- Browser Functions

    private final class CopyCodeFunction extends BrowserFunction {
        private CopyCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                String code = (String) arguments[0];
                copyToClipboard(code);
                presenter.onCopyCode(code);
            }
            return null;
        }
    }

    private final class ApplyPatchFunction extends BrowserFunction {
        private ApplyPatchFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onApplyPatch((String) arguments[0]);
            }
            return null;
        }
    }

    private final class InsertCodeFunction extends BrowserFunction {
        private InsertCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onInsertCode((String) arguments[0]);
            }
            return null;
        }
    }

    private final class DiffCodeFunction extends BrowserFunction {
        private DiffCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onDiffCode((String) arguments[0]);
            }
            return null;
        }
    }

    private final class NewFileFunction extends BrowserFunction {
        private NewFileFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length >= 2 && arguments[0] instanceof String && arguments[1] instanceof String) {
                presenter.onNewFile((String) arguments[0], (String) arguments[1]);
            }
            return null;
        }
    }

    private final class RemoveMessageFunction extends BrowserFunction {
        private RemoveMessageFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onRemoveMessage((String) arguments[0]);
            }
            return null;
        }
    }

    private final class ScrollInteractionFunction extends BrowserFunction {
        private ScrollInteractionFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof Boolean) {
                autoScrollEnabled = ((Boolean) arguments[0]).booleanValue();
            }
            return null;
        }
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
        try {
            clipboard.setContents(new Object[] { text }, new org.eclipse.swt.dnd.Transfer[] { TextTransfer.getInstance() });
        } finally {
            clipboard.dispose();
        }
    }

    // --- Nested Types

    private static final class SimpleCommandProposal implements IContentProposal {
        private final String command;
        private final String description;

        private SimpleCommandProposal(String command, String description) {
            this.command = command;
            this.description = description;
        }

        @Override
        public String getContent() {
            return "/" + command;
        }

        @Override
        public int getCursorPosition() {
            return getContent().length();
        }

        @Override
        public String getLabel() {
            return "/" + command;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    private static final class ImageAttachment {
        private final ImageData image;
        private final String caption;

        private ImageAttachment(ImageData image, String caption) {
            this.image = image;
            this.caption = caption;
        }
    }

    private static final class ModelOption {
        private final String id;
        private final String displayName;
        private final String iconFile;

        private ModelOption(String id, String displayName, String iconFile) {
            this.id = id;
            this.displayName = displayName;
            this.iconFile = iconFile;
        }
    }

    private final class AttachmentRenderer {
        private static final int THUMBNAIL_SIZE = 96;
        private static final int BORDER_WIDTH = 2;

        private void add(final ImageData preview, final String caption) {
            final Display display = Display.getCurrent();
            final Image normal = createThumbnailWithBorder(preview, display, false);
            final Image selected = createThumbnailWithBorder(preview, display, true);
            final boolean[] isSelected = new boolean[] { false };

            final Label imageLabel = new Label(imagesContainer, SWT.NONE);
            imageLabel.setImage(normal);

            int attachmentIndex = imagesContainer.getChildren().length - 1;
            imageLabel.setData("attachmentIndex", Integer.valueOf(attachmentIndex));
            imageLabel.setData("selected", Boolean.FALSE);
            imageLabel.setData("normalImage", normal);
            imageLabel.setData("selectedImage", selected);

            if (caption != null) {
                imageLabel.setToolTipText(caption);
            }

            imageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    boolean ctrlPressed = (e.stateMask & SWT.CTRL) != 0;
                    if (!ctrlPressed) {
                        for (org.eclipse.swt.widgets.Control child : imagesContainer.getChildren()) {
                            if (child instanceof Label && child != imageLabel) {
                                Boolean wasSelected = (Boolean) child.getData("selected");
                                if (wasSelected != null && wasSelected.booleanValue()) {
                                    child.setData("selected", Boolean.FALSE);
                                    ((Label) child).setImage((Image) child.getData("normalImage"));
                                }
                            }
                        }
                    }

                    isSelected[0] = !isSelected[0];
                    imageLabel.setData("selected", Boolean.valueOf(isSelected[0]));
                    imageLabel.setImage(isSelected[0] ? selected : normal);
                    imagesContainer.setFocus();
                }
            });

            imageLabel.addDisposeListener(e -> {
                if (normal != null && !normal.isDisposed()) {
                    normal.dispose();
                }
                if (selected != null && !selected.isDisposed()) {
                    selected.dispose();
                }
            });
        }

        private Image createThumbnailWithBorder(ImageData sourceData, Display display, boolean selected) {
            int borderWidth = selected ? 3 : BORDER_WIDTH;
            Color borderColor = selected ? new Color(display, 0, 120, 215) : new Color(display, 128, 128, 128);
            Color bgColor = imagesContainer.getBackground();

            Image result = new Image(display, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            GC gc = new GC(result);
            try {
                gc.setAntialias(SWT.ON);
                gc.setInterpolation(SWT.HIGH);
                gc.setAdvanced(true);

                gc.setBackground(bgColor);
                gc.fillRectangle(0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

                gc.setBackground(borderColor);
                gc.fillRectangle(0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

                int innerSize = THUMBNAIL_SIZE - borderWidth * 2;

                if (sourceData != null) {
                    int srcWidth = sourceData.width;
                    int srcHeight = sourceData.height;
                    int cropSize = Math.min(srcWidth, srcHeight);
                    int cropX = (srcWidth - cropSize) / 2;
                    int cropY = (srcHeight - cropSize) / 2;

                    Image sourceImage = new Image(display, sourceData);
                    try {
                        gc.drawImage(sourceImage, cropX, cropY, cropSize, cropSize, borderWidth, borderWidth, innerSize, innerSize);
                    } finally {
                        sourceImage.dispose();
                    }
                } else {
                    Color fillColor = new Color(display, 60, 60, 60);
                    try {
                        gc.setBackground(fillColor);
                        gc.fillRectangle(borderWidth, borderWidth, innerSize, innerSize);
                    } finally {
                        fillColor.dispose();
                    }
                }
            } finally {
                gc.dispose();
                borderColor.dispose();
            }

            return result;
        }
    }

    // --- Cleanup

    @Override
    public void dispose() {
        if (modelMenu != null && !modelMenu.isDisposed()) {
            modelMenu.dispose();
        }
        for (Image img : iconCache.values()) {
            if (img != null && !img.isDisposed()) {
                img.dispose();
            }
        }
        iconCache.clear();
        super.dispose();
    }
}
