/*
 *  Copyright (c) 2021 Proton AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.protonvpn.actions

import com.protonvpn.MockSwitch
import com.protonvpn.android.R
import com.protonvpn.base.BaseRobot

class CountriesRobot : BaseRobot() {

    fun selectCountry(country: String): CountriesRobot = clickElementByText(country)

    fun clickConnectButton(contentDescription: String): ConnectionRobot {
        clickElementByIdAndContentDescription<HomeRobot>(R.id.buttonConnect, contentDescription)
        if (!MockSwitch.mockedConnectionUsed) {
            HomeRobot().allowVpnToBeUsed()
        }
        return ConnectionRobot()
    }
}