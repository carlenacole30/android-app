/*
 * Copyright (c) 2022. Proton AG
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

package com.protonvpn.tests.login

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.protonvpn.actions.HomeRobot
import com.protonvpn.actions.LoginRobot
import com.protonvpn.android.ui.main.MobileMainActivity
import com.protonvpn.mocks.TestApiConfig
import com.protonvpn.test.shared.TestUser
import com.protonvpn.testRules.LoginTestRule
import com.protonvpn.testRules.ProtonHiltAndroidRule
import com.protonvpn.testsHelper.TestSetup
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlin.test.assertNotNull

@HiltAndroidTest
class TokenExpirationTests {
    private val hiltRule = ProtonHiltAndroidRule(this, TestApiConfig.Backend)
    private lateinit var homeRobot: HomeRobot
    private lateinit var loginRobot: LoginRobot

    @get:Rule
    val rules = RuleChain
        .outerRule(ProtonHiltAndroidRule(this, TestApiConfig.Backend))
        .around(LoginTestRule(TestUser.plusUser))

    @Before
    fun setUp() {
        assertNotNull(
            TestSetup.quark,
            "Quark can be null. Make sure this test is not being run on production."
        )
        TestSetup.quark.jailUnban()
        hiltRule.inject()
        hiltRule.startApplicationAndWaitForIdle()
        ActivityScenario.launch(MobileMainActivity::class.java)
        homeRobot = HomeRobot()
        loginRobot = LoginRobot()
        homeRobot.verify { serverListIsVisible() }
    }

    @Test
    fun sessionAndRefreshTokenExpiration() {
        TestSetup.quark!!.expireSession(TestUser.plusUser.email, true)
        homeRobot.swipeDownToRefreshServerList()
        loginRobot.verify { loginScreenIsDisplayed() }
    }

    @Test
    fun sessionExpirationCheckIfNotLoggedOut() {
        TestSetup.quark!!.expireSession(TestUser.plusUser.email)
        homeRobot.swipeDownToRefreshServerList()
            .verify { loginScreenIsNotDisplayed() }
    }
}