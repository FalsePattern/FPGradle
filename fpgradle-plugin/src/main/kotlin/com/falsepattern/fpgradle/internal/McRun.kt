/*
 * FPGradle
 *
 * Copyright (C) 2024 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.fpgradle.internal

import com.gtnewhorizons.retrofuturagradle.util.Distribution
import com.gtnewhorizons.retrofuturagradle.util.Distribution.CLIENT
import com.gtnewhorizons.retrofuturagradle.util.Distribution.DEDICATED_SERVER

enum class McRun(val taskName: String, val side: Distribution, val obfuscated: Boolean) {
    CLIENT_DEV("runClient", CLIENT, false),
    CLIENT_OBF("runObfClient", CLIENT, true),
    SERVER_DEV("runServer", DEDICATED_SERVER, false),
    SERVER_OBF("runObfServer", DEDICATED_SERVER, true);

    companion object {
        fun client() = arrayOf(CLIENT_DEV, CLIENT_OBF)
    }
}