/*******************************************************************************
 * Copyright (c) 2009 Alena Laskavaia 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alena Laskavaia  - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.codan.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.cdt.codan.core.CodanCorePlugin;
import org.eclipse.cdt.codan.core.PreferenceConstants;
import org.eclipse.cdt.codan.core.builder.CodanPreferencesLoader;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class CheckersRegisry implements Iterable<IChecker> {
	private static final String EXTENSION_POINT_NAME = "checkers";
	private static final String CHECKER_ELEMENT = "checker";
	private static final String PROBLEM_ELEMENT = "problem";
	private static final Object DEFAULT = "DEFAULT";
	private Collection<IChecker> checkers = new ArrayList<IChecker>();
	private static CheckersRegisry instance;
	private HashMap<Object, IProblemProfile> profiles = new HashMap<Object, IProblemProfile>();

	private CheckersRegisry() {
		instance = this;
		profiles.put(DEFAULT, new ProblemProfile());
		readCheckersRegistry();
	}

	private void readCheckersRegistry() {
		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint(
				CodanCorePlugin.PLUGIN_ID, EXTENSION_POINT_NAME);
		if (ep == null)
			return;
		IConfigurationElement[] elements = ep.getConfigurationElements();
		// process categories
		// process shared problems
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement configurationElement = elements[i];
			processProblem(configurationElement);
		}
		// process checkers
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement configurationElement = elements[i];
			processChecker(configurationElement);
		}
	}

	/**
	 * @param configurationElement
	 */
	private void processChecker(IConfigurationElement configurationElement) {
		try {
			if (configurationElement.getName().equals(CHECKER_ELEMENT)) {
				String id = getAtt(configurationElement, "id");
				if (id == null)
					return;
				String name = getAtt(configurationElement, "name", false);
				if (name == null)
					name = id;
				IChecker checkerObj = null;
				try {
					Object checker = configurationElement
							.createExecutableExtension("class");
					checkerObj = (IChecker) checker;
					addChecker(checkerObj);
				} catch (CoreException e) {
					CodanCorePlugin.log(e);
					return;
				}
				IConfigurationElement[] children1 = configurationElement
						.getChildren("problemRef");
				boolean hasRef = false;
				IConfigurationElement[] children2 = configurationElement
						.getChildren(PROBLEM_ELEMENT);
				if (children2 != null) {
					for (IConfigurationElement ref : children2) {
						IProblem p = processProblem(ref);
						addRefProblem(checkerObj, p);
						hasRef = true;
					}
				}
				if (children1 != null) {
					for (IConfigurationElement ref : children1) {
						hasRef = true;
						IProblem p = getDefaultProfile().findProblem(
								ref.getAttribute("refId"));
						addRefProblem(checkerObj, p);
					}
				}
				if (!hasRef) {
					addProblem(new CodanProblem(id, name), null);
				}
			}
		} catch (Exception e) {
			CodanCorePlugin.log(e);
		}
	}

	/**
	 * @param configurationElement
	 * @return
	 */
	private CodanProblem processProblem(
			IConfigurationElement configurationElement) {
		if (configurationElement.getName().equals(PROBLEM_ELEMENT)) {
			String id = getAtt(configurationElement, "id");
			if (id == null)
				return null;
			String name = getAtt(configurationElement, "name");
			if (name == null)
				name = id;
			CodanProblem p = new CodanProblem(id, name);
			String category = getAtt(configurationElement, "category", false);
			if (category == null)
				category = "org.eclipse.cdt.codan.core.categories.ProgrammingProblems";
			addProblem(p, category);
			return p;
		}
		return null;
	}

	private static String getAtt(IConfigurationElement configurationElement,
			String name) {
		return getAtt(configurationElement, name, true);
	}

	private static String getAtt(IConfigurationElement configurationElement,
			String name, boolean req) {
		String elementValue = configurationElement.getAttribute(name);
		if (elementValue == null && req)
			CodanCorePlugin.log("Extension "
					+ configurationElement.getDeclaringExtension()
							.getUniqueIdentifier()
					+ " missing required attribute: "
					+ configurationElement.getName() + "." + name);
		return elementValue;
	}

	public Iterator<IChecker> iterator() {
		return checkers.iterator();
	}

	public static CheckersRegisry getInstance() {
		if (instance == null)
			new CheckersRegisry();
		return instance;
	}

	public void addChecker(IChecker checker) {
		checkers.add(checker);
	}

	public void addProblem(IProblem p, String category) {
		((ProblemProfile) getDefaultProfile()).addProblem(p,
				getDefaultProfile().getRoot());
	}

	public void addRefProblem(IChecker c, IProblem p) {
	}

	/**
	 * @return
	 */
	public IProblemProfile getDefaultProfile() {
		return profiles.get(DEFAULT);
	}

	/**
	 * @return
	 */
	public IProblemProfile getWorkspaceProfile() {
		IProblemProfile wp = profiles.get(ResourcesPlugin.getWorkspace());
		if (wp == null) {
			try {
				wp = (IProblemProfile) getDefaultProfile().clone();
				// load default values
				CodanPreferencesLoader loader = new CodanPreferencesLoader(wp);
				loader.load(CodanCorePlugin.getDefault().getStorePreferences());
			} catch (CloneNotSupportedException e) {
				wp = getDefaultProfile();
			}
		}
		return wp;
	}

	public void updateProfile(IResource element, IProblemProfile profile) {
		if (profile == null)
			profiles.remove(element);
		else
			profiles.put(element, profile);
	}

	/**
	 * @param element
	 * @return
	 */
	public IProblemProfile getResourceProfile(IResource element) {
		IProblemProfile prof = profiles.get(element);
		if (prof == null) {
			if (element instanceof IProject) {
				try {
					prof = (IProblemProfile) getWorkspaceProfile().clone();
					// load default values
					CodanPreferencesLoader loader = new CodanPreferencesLoader(
							prof);
					IEclipsePreferences node = new ProjectScope(
							(IProject) element)
							.getNode(CodanCorePlugin.PLUGIN_ID);
					boolean useWorkspace = node.getBoolean(
							PreferenceConstants.P_USE_PARENT, false);
					if (!useWorkspace) {
						loader.load(node);
					}
					updateProfile(element, prof);
				} catch (CloneNotSupportedException e) {
					// cant
				}
			} else if (element.getParent() != null) {
				prof = getResourceProfile(element.getParent());
			} else {
				prof = getResourceProfile(element.getProject());
			}
		} else {
		}
		return prof;
	}

	/**
	 * @param element
	 * @return
	 */
	public IProblemProfile getResourceProfileWorkingCopy(IResource element) {
		if (element instanceof IProject) {
			try {
				IProblemProfile prof = (IProblemProfile) getWorkspaceProfile()
						.clone();
				// load default values
				CodanPreferencesLoader loader = new CodanPreferencesLoader(prof);
				IEclipsePreferences node = new ProjectScope((IProject) element)
						.getNode(CodanCorePlugin.PLUGIN_ID);
				loader.load(node);
				return prof;
			} catch (CloneNotSupportedException e) {
				// cant
			}
		}
		return null;
	}
}
