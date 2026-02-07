package de.bund.zrb.natural.ui;

import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import de.bund.zrb.natural.infrastructure.logging.PluginLog;

/**
 * Simple tree-based view.
 *
 * This is based on the classic PDE "Sample View" template and can later be
 * replaced by a real Natural AST explorer.
 */
public class AstExplorerView extends ViewPart {

    public static final String ID = "de.bund.zrb.natural.ui.AstExplorerView";

    private TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;

    private Action action1;
    private Action action2;
    private Action doubleClickAction;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, 0x302); // SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
        drillDownAdapter = new DrillDownAdapter(viewer);

        viewer.setContentProvider(new ViewContentProvider());
        viewer.setInput(getViewSite());
        viewer.setLabelProvider(new ViewLabelProvider());

        getSite().setSelectionProvider(viewer);

        createActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();

        PluginLog.info("AstExplorerView created.");
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    private void hookContextMenu() {
        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });

        Menu menu = menuManager.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuManager, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(action1);
        manager.add(new Separator());
        manager.add(action2);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(action1);
        manager.add(action2);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IMenuManager manager) {
        // Never called - kept to mirror original template.
    }

    private void fillLocalToolBar(org.eclipse.jface.action.IToolBarManager manager) {
        manager.add(action1);
        manager.add(action2);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private void createActions() {
        action1 = new Action() {
            @Override
            public void run() {
                showMessage("Action 1 executed");
            }
        };
        action1.setText("Action 1");
        action1.setToolTipText("Action 1 tooltip");
        action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        action2 = new Action() {
            @Override
            public void run() {
                showMessage("Action 2 executed");
            }
        };
        action2.setText("Action 2");
        action2.setToolTipText("Action 2 tooltip");
        action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        doubleClickAction = new Action() {
            @Override
            public void run() {
                Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                showMessage(String.valueOf(selected));
            }
        };
    }

    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    private void showMessage(String message) {
        MessageDialog.openInformation(viewer.getControl().getShell(), "AST Explorer", message);
    }

    private final class ViewContentProvider implements ITreeContentProvider {

        private TreeParent invisibleRoot;

        @Override
        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                if (invisibleRoot == null) {
                    initialize();
                }
                return getChildren(invisibleRoot);
            }
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child) {
            if (child instanceof TreeObject) {
                return ((TreeObject) child).getParent();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof TreeParent) {
                return ((TreeParent) parent).getChildren();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeParent) {
                return ((TreeParent) parent).hasChildren();
            }
            return false;
        }

        private void initialize() {
            TreeObject leaf1 = new TreeObject("Leaf 1");
            TreeObject leaf2 = new TreeObject("Leaf 2");
            TreeObject leaf3 = new TreeObject("Leaf 3");
            TreeParent parent1 = new TreeParent("Parent 1");
            parent1.addChild(leaf1);
            parent1.addChild(leaf2);
            parent1.addChild(leaf3);

            TreeObject leaf4 = new TreeObject("Leaf 4");
            TreeParent parent2 = new TreeParent("Parent 2");
            parent2.addChild(leaf4);

            TreeParent root = new TreeParent("Root");
            root.addChild(parent1);
            root.addChild(parent2);

            invisibleRoot = new TreeParent("");
            invisibleRoot.addChild(root);
        }
    }

    private static final class ViewLabelProvider extends LabelProvider {

        @Override
        public String getText(Object element) {
            return String.valueOf(element);
        }

        @Override
        public Image getImage(Object element) {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
            if (element instanceof TreeParent) {
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            }
            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
    }

    private static class TreeObject implements IAdaptable {

        private final String name;
        private TreeParent parent;

        public TreeObject(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setParent(TreeParent parent) {
            this.parent = parent;
        }

        public TreeParent getParent() {
            return parent;
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
            return null;
        }
    }

    private static final class TreeParent extends TreeObject {

        private final ArrayList children = new ArrayList();

        public TreeParent(String name) {
            super(name);
        }

        public void addChild(TreeObject child) {
            children.add(child);
            child.setParent(this);
        }

        public void removeChild(TreeObject child) {
            children.remove(child);
            child.setParent(null);
        }

        public TreeObject[] getChildren() {
            return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
        }

        public boolean hasChildren() {
            return children.size() > 0;
        }
    }
}
