/*******************************************************************************
 * Copyright (c) 2012 Jens Kristian Villadsen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Jens Kristian Villadsen - initial API and implementation
 ******************************************************************************/
package com.jb.dailyplay.GooglePlayMusicApi.impl;

public class InvalidCredentialsException extends Exception
{

    InvalidCredentialsException(
        final IllegalStateException ise,
        final String string)
    {
        super(string, ise);
    }

    /**
	 *
	 */
    private static final long serialVersionUID = 7121443984182451964L;

}