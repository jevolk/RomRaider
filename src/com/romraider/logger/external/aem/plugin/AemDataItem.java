/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2010 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.external.aem.plugin;

import com.romraider.logger.external.core.DataListener;
import com.romraider.logger.external.core.ExternalDataItem;

public final class AemDataItem implements ExternalDataItem, DataListener {
    private double data;

    public String getName() {
        return "AEM UEGO AFR [9600]";
    }

    public String getDescription() {
        return "AEM UEGO AFR data";
    }

    public String getUnits() {
        return "AFR";
    }

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
    }
}