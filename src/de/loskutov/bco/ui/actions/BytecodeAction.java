/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.ui.actions;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.compare.BytecodeCompare;
import de.loskutov.bco.compare.TypedElement;
import de.loskutov.bco.preferences.BCOConstants;
import de.loskutov.bco.ui.JdtUtils;

public abstract class BytecodeAction implements IObjectActionDelegate {
    protected IStructuredSelection selection;
    protected Shell shell;

    @Override
    public void selectionChanged(IAction action, ISelection newSelection) {
        if (newSelection instanceof IStructuredSelection) {
            this.selection = (IStructuredSelection) newSelection;
        }
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.shell = targetPart.getSite().getShell();
    }

    protected void exec(IJavaElement element1, IJavaElement element2) throws Exception {
        final BitSet modes = getModes();
        CompareUI.openCompareEditor(new BytecodeCompare(
            createTypedElement(element1, modes),
            createTypedElement(element2, modes)));
    }

    protected TypedElement createTypedElement(IJavaElement javaElement, BitSet modes) {
        String name;
        IClassFile classFile = (IClassFile) javaElement
            .getAncestor(IJavaElement.CLASS_FILE);
        // existing read-only class files
        if (classFile != null) {
            name = classFile.getPath().toOSString();
            if (!name.endsWith(".class")) { //$NON-NLS-1$
                name += '/' + JdtUtils.getFullBytecodeName(classFile);
            }
        } else {
            // usual eclipse - generated bytecode
            name = JdtUtils.getByteCodePath(javaElement);
        }
        String methodName = null;
        if(javaElement.getElementType() == IJavaElement.METHOD ||
            javaElement.getElementType() == IJavaElement.INITIALIZER){
            methodName = JdtUtils.getMethodSignature(javaElement);
            if(methodName != null){
                name += ":" + methodName;
            }
        }
        return new TypedElement(name, methodName, TypedElement.TYPE_BYTECODE, javaElement, modes);
    }

    private static BitSet getModes() {
        IPreferenceStore store = BytecodeOutlinePlugin.getDefault().getPreferenceStore();
        BitSet modes = new BitSet();
        modes.set(BCOConstants.F_LINK_VIEW_TO_EDITOR, store.getBoolean(BCOConstants.LINK_VIEW_TO_EDITOR));
        modes.set(BCOConstants.F_SHOW_ONLY_SELECTED_ELEMENT, store.getBoolean(BCOConstants.SHOW_ONLY_SELECTED_ELEMENT));
        modes.set(BCOConstants.F_SHOW_RAW_BYTECODE, store.getBoolean(BCOConstants.SHOW_RAW_BYTECODE));
        modes.set(BCOConstants.F_SHOW_LINE_INFO, store.getBoolean(BCOConstants.DIFF_SHOW_LINE_INFO));
        modes.set(BCOConstants.F_SHOW_VARIABLES, store.getBoolean(BCOConstants.DIFF_SHOW_VARIABLES));
        modes.set(BCOConstants.F_SHOW_ASMIFIER_CODE, store.getBoolean(BCOConstants.DIFF_SHOW_ASMIFIER_CODE));
        modes.set(BCOConstants.F_SHOW_ANALYZER, store.getBoolean(BCOConstants.SHOW_ANALYZER));
        modes.set(BCOConstants.F_SHOW_STACKMAP, store.getBoolean(BCOConstants.DIFF_SHOW_STACKMAP));
        modes.set(BCOConstants.F_EXPAND_STACKMAP, store.getBoolean(BCOConstants.DIFF_EXPAND_STACKMAP));
        return modes;
    }

    protected IJavaElement[] getSelectedResources() {
        ArrayList<Object> resources = null;
        if (!selection.isEmpty()) {
            resources = new ArrayList<Object>();
            for (Iterator elements = selection.iterator(); elements.hasNext();) {
                Object next = elements.next();
                if (next instanceof IFile) {
                    resources.add(JavaCore.create((IFile)next));
                    continue;
                } if (next instanceof IJavaElement) {
                    resources.add(next);
                    continue;
                } else if (next instanceof IAdaptable) {
                    IAdaptable a = (IAdaptable) next;
                    Object adapter = a.getAdapter(IFile.class);
                    if (adapter instanceof IFile) {
                        resources.add(JavaCore.create((IFile)adapter));
                        continue;
                    }
                    adapter = a.getAdapter(ICompilationUnit.class);
                    if (adapter instanceof ICompilationUnit) {
                        resources.add(adapter);
                        continue;
                    }

                    adapter = a.getAdapter(IClassFile.class);
                    if (adapter instanceof IClassFile) {
                        resources.add(adapter);
                        continue;
                    }
                }
            }
        }

        if (resources != null && !resources.isEmpty()) {
            return resources.toArray(new IJavaElement[resources
                .size()]);
        }

        return new IJavaElement[0];
    }
}
