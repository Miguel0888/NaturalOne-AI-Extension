package de.bund.zrb.natural.ui.settings;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Settings dialog with a left navigation tree and a right details pane.
 *
 * v1 contains provider settings for Custom and Ollama.
 */
public final class ChatSettingsDialog extends TitleAreaDialog {

    private static final String PREF_NODE = "de.bund.zrb.natural.codeinsightbridge.ui";

    private static final String PREF_OLLAMA_BASE_URL = "chat.ollama.baseUrl";
    private static final String PREF_OLLAMA_MODEL = "chat.ollama.model";

    private static final class Node {
        final String id;
        final String label;
        final Node[] children;

        Node(String id, String label, Node... children) {
            this.id = id;
            this.label = label;
            this.children = children == null ? new Node[0] : children;
        }

        boolean isLeaf() {
            return children.length == 0;
        }

        public String toString() {
            return label;
        }
    }

    private final Node root;

    private TreeViewer tree;
    private Composite rightPane;

    // Ollama fields
    private Text ollamaBaseUrlText;
    private Text ollamaModelText;

    public ChatSettingsDialog(Shell parentShell) {
        super(parentShell);
        this.root = new Node("root", "root",
                new Node("providers", "Provider",
                        new Node("provider.custom", "Custom"),
                        new Node("provider.ollama", "Ollama")));
    }

    @Override
    public void create() {
        super.create();
        setTitle("Settings");
        setMessage("Konfiguriere Provider-Einstellungen (v1: Custom, Ollama).");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        layout.horizontalSpacing = 12;
        container.setLayout(layout);

        tree = new TreeViewer(container, SWT.BORDER | SWT.SINGLE);
        tree.getTree().setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, true));
        tree.setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return ((Node) inputElement).children;
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                return ((Node) parentElement).children;
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return ((Node) element).children.length > 0;
            }
        });
        tree.setLabelProvider(new LabelProvider());
        tree.setInput(root);
        tree.expandAll();

        Composite right = new Composite(container, SWT.BORDER);
        right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        right.setLayout(new FillLayout());

        rightPane = new Composite(right, SWT.NONE);
        rightPane.setLayout(new GridLayout(2, false));

        tree.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Object sel = ((TreeSelection) event.getSelection()).getFirstElement();
                if (sel instanceof Node) {
                    showNode((Node) sel);
                }
            }
        });

        // default selection
        tree.setSelection(new StructuredSelection(findNode("provider.ollama")), true);

        return area;
    }

    private Node findNode(String id) {
        if (id == null) {
            return root;
        }
        return findNodeRecursive(root, id);
    }

    private Node findNodeRecursive(Node current, String id) {
        if (current == null) {
            return null;
        }
        if (id.equals(current.id)) {
            return current;
        }
        for (Node c : current.children) {
            Node found = findNodeRecursive(c, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void showNode(Node node) {
        for (Control child : rightPane.getChildren()) {
            child.dispose();
        }

        if (node == null) {
            createPlaceholder("Bitte links einen Eintrag auswählen.");
        } else if ("provider.custom".equals(node.id)) {
            createCustomPane();
        } else if ("provider.ollama".equals(node.id)) {
            createOllamaPane();
        } else {
            createPlaceholder("Keine Einstellungen für: " + node.label);
        }

        rightPane.layout(true, true);
    }

    private void createPlaceholder(String text) {
        Label l = new Label(rightPane, SWT.WRAP);
        l.setText(text == null ? "" : text);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);
    }

    private void createCustomPane() {
        Label header = new Label(rightPane, SWT.NONE);
        header.setText("Custom");
        GridData hd = new GridData(SWT.FILL, SWT.TOP, true, false);
        hd.horizontalSpan = 2;
        header.setLayoutData(hd);

        createPlaceholder("Custom hat in v1 keine konfigurierbaren Einstellungen.");
    }

    private void createOllamaPane() {
        Label header = new Label(rightPane, SWT.NONE);
        header.setText("Ollama");
        GridData hd = new GridData(SWT.FILL, SWT.TOP, true, false);
        hd.horizontalSpan = 2;
        header.setLayoutData(hd);

        Label urlLabel = new Label(rightPane, SWT.NONE);
        urlLabel.setText("Base URL");
        ollamaBaseUrlText = new Text(rightPane, SWT.BORDER);
        ollamaBaseUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label modelLabel = new Label(rightPane, SWT.NONE);
        modelLabel.setText("Model");
        ollamaModelText = new Text(rightPane, SWT.BORDER);
        ollamaModelText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PREF_NODE);
        String baseUrl = prefs.get(PREF_OLLAMA_BASE_URL, "http://localhost:11434");
        String model = prefs.get(PREF_OLLAMA_MODEL, "llama3.1");

        ollamaBaseUrlText.setText(baseUrl == null ? "" : baseUrl);
        ollamaModelText.setText(model == null ? "" : model);

        Label hint = new Label(rightPane, SWT.WRAP);
        hint.setText("Hinweis: Änderungen werden beim Bestätigen gespeichert. Der aktuelle Chat nutzt neue Werte beim nächsten Request.");
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = 2;
        hint.setLayoutData(gd);
    }

    @Override
    protected void okPressed() {
        saveIfPresent();
        super.okPressed();
    }

    private void saveIfPresent() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PREF_NODE);

        if (ollamaBaseUrlText != null && !ollamaBaseUrlText.isDisposed()) {
            String v = ollamaBaseUrlText.getText();
            prefs.put(PREF_OLLAMA_BASE_URL, v == null ? "" : v.trim());
        }
        if (ollamaModelText != null && !ollamaModelText.isDisposed()) {
            String v = ollamaModelText.getText();
            prefs.put(PREF_OLLAMA_MODEL, v == null ? "" : v.trim());
        }

        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            // best-effort
        }
    }
}
