/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.qt.core.qmljs;

public interface IQmlPropertyDeclaration extends IQmlObjectMember {
	@Override
	default public String getType() {
		return "QMLPropertyDeclaration"; //$NON-NLS-1$
	}

	public boolean isReadonly();

	public boolean isDefault();

	public IQmlPropertyType getKind();

	public IQmlPropertyType getModifier();

	public IJSIdentifier getIdentifier();

	public IQmlBinding getBinding();
}
