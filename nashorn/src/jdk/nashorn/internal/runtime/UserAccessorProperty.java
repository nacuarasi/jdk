/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.nashorn.internal.runtime;

import java.lang.invoke.MethodHandle;
import jdk.nashorn.internal.runtime.linker.Lookup;

/**
 * Property with user defined getters/setters. Actual getter and setter
 * functions are stored in underlying ScriptObject. Only the 'slot' info is
 * stored in the property.
 *
 * The slots here denote either ScriptObject embed field number or spill
 * array index. For spill array index, we use slot value of
 * (index + ScriptObject.embedSize). See also ScriptObject.getEmbedOrSpill
 * method. Negative slot value means that the corresponding getter or setter
 * is null. Note that always two slots are allocated in ScriptObject - but
 * negative (less by 1) slot number is stored for null getter or setter.
 * This is done so that when the property is redefined with a different
 * getter and setter (say, both non-null), we'll have spill slots to store
 * those. When a slot is negative, (-slot - 1) is the embed/spill index.
 */
public final class UserAccessorProperty extends Property {

    /** User defined getter function slot. */
    private final int getterSlot;

    /** User defined setter function slot. */
    private final int setterSlot;

    /**
     * Constructor
     *
     * @param key        property key
     * @param flags      property flags
     * @param getterSlot getter slot, starting at first embed
     * @param setterSlot setter slot, starting at first embed
     */
    public UserAccessorProperty(final String key, final int flags, final int getterSlot, final int setterSlot) {
        super(key, flags, -1);
        this.getterSlot = getterSlot;
        this.setterSlot = setterSlot;
    }

    private UserAccessorProperty(final UserAccessorProperty property) {
        super(property);

        this.getterSlot = property.getterSlot;
        this.setterSlot = property.setterSlot;
    }

    /**
     * Return getter slot for this UserAccessorProperty. Slots start with first embed field.
     * @return getter slot
     */
    public int getGetterSlot() {
        return getterSlot < 0 ? -getterSlot - 1 : getterSlot;
    }

    /**
     * Return setter slot for this UserAccessorProperty. Slots start with first embed field.
     * @return setter slot
     */
    public int getSetterSlot() {
        return setterSlot < 0 ? -setterSlot - 1 : setterSlot;
    }

    @Override
    protected Property copy() {
        return new UserAccessorProperty(this);
    }

    @Override
    public boolean equals(final Object other) {
        if (!super.equals(other)) {
            return false;
        }

        final UserAccessorProperty uc = (UserAccessorProperty) other;
        return getterSlot == uc.getterSlot && setterSlot == uc.setterSlot;
     }

    @Override
    public int hashCode() {
        return super.hashCode() ^ getterSlot ^ setterSlot;
    }

    /*
     * Accessors.
     */
    @Override
    public int getSpillCount() {
        // calculate how many spill array slots used by this propery.
        int count = 0;
        if (getGetterSlot() >= ScriptObject.EMBED_SIZE) {
            count++;
        }
        if (getSetterSlot() >= ScriptObject.EMBED_SIZE) {
            count++;
        }
        return count;
    }

    @Override
    public boolean hasGetterFunction() {
        return getterSlot > -1;
    }

    @Override
    public boolean hasSetterFunction() {
        return setterSlot > -1;
    }

    @Override
    public MethodHandle getGetter(final Class<?> type) {
        return Lookup.filterReturnType(ScriptObject.USER_ACCESSOR_GETTER.methodHandle(), type);
    }

    @Override
    public ScriptFunction getGetterFunction(final ScriptObject obj) {
        final Object value = obj.getEmbedOrSpill(getterSlot);
        return (value instanceof ScriptFunction) ? (ScriptFunction) value : null;
    }

    @Override
    public MethodHandle getSetter(final Class<?> type, final PropertyMap currentMap) {
        return ScriptObject.USER_ACCESSOR_SETTER.methodHandle();
    }

    @Override
    public ScriptFunction getSetterFunction(final ScriptObject obj) {
        final Object value = obj.getEmbedOrSpill(setterSlot);
        return (value instanceof ScriptFunction) ? (ScriptFunction) value : null;
    }

}
