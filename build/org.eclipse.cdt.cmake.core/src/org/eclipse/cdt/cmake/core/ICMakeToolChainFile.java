/*******************************************************************************
 * Copyright (c) 2016 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.cdt.cmake.core;

import java.nio.file.Path;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.core.runtime.CoreException;

/**
 * A toolchain file.
 *
 * @noimplement
 * @noextend
 */
public interface ICMakeToolChainFile {

	Path getPath();

	String getProperty(String key);

	void setProperty(String key, String value);

	IToolChain getToolChain() throws CoreException;

}
