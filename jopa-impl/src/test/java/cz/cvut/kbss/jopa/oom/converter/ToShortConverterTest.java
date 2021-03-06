/**
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.oom.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ToShortConverterTest {

    private ToShortConverter converter = new ToShortConverter();

    @Test
    public void toAttributeSupportsWideningIntegerConversion() {
        assertEquals(Short.valueOf((short) 11), converter.convertToAttribute(Byte.valueOf((byte) 11)));
    }

    @Test
    public void toAttributeSupportsIdentityConversion() {
        assertEquals(Short.valueOf((short) 11), converter.convertToAttribute(Short.valueOf((short) 11)));
    }
}