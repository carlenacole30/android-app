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

package com.protonvpn.app.ui.promooffer

import android.app.Activity
import com.protonvpn.android.appconfig.ApiNotification
import com.protonvpn.android.appconfig.ApiNotificationManager
import com.protonvpn.android.appconfig.ApiNotificationTypes
import com.protonvpn.android.ui.ForegroundActivityTracker
import com.protonvpn.android.ui.promooffers.OneTimePopupNotificationTrigger
import com.protonvpn.android.ui.promooffers.PromoActivityOpener
import com.protonvpn.android.ui.promooffers.PromoOffersPrefs
import com.protonvpn.android.utils.Storage
import com.protonvpn.test.shared.ApiNotificationTestHelper.mockFullScreenImagePanel
import com.protonvpn.test.shared.ApiNotificationTestHelper.mockOffer
import com.protonvpn.test.shared.MockSharedPreference
import com.protonvpn.test.shared.MockSharedPreferencesProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test

private const val NOTIFICATION_ID = "notification 1"

@OptIn(ExperimentalCoroutinesApi::class)
class OneTimePopupNotificationTriggerTests {

    @MockK
    private lateinit var mockForegroundActivityTracker: ForegroundActivityTracker
    @MockK
    private lateinit var mockApiNotificationManager: ApiNotificationManager
    @MockK
    private lateinit var mockAccountManager: AccountManager
    @RelaxedMockK
    private lateinit var mockPromoActivityOpener: PromoActivityOpener

    private lateinit var activeNotificationsFlow: MutableStateFlow<List<ApiNotification>>
    private lateinit var primaryUserIdFlow: MutableStateFlow<UserId?>
    private lateinit var foregroundActivityFlow: MutableStateFlow<Activity?>
    private lateinit var promoOffersPrefs: PromoOffersPrefs
    private lateinit var testScope: TestScope

    private lateinit var oneTimePopupNotificationTrigger: OneTimePopupNotificationTrigger

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Storage.setPreferences(MockSharedPreference())
        promoOffersPrefs = PromoOffersPrefs(MockSharedPreferencesProvider())

        primaryUserIdFlow = MutableStateFlow(UserId("user"))
        every { mockAccountManager.getPrimaryUserId() } returns primaryUserIdFlow

        activeNotificationsFlow = MutableStateFlow(emptyList())
        every { mockApiNotificationManager.activeListFlow } returns activeNotificationsFlow

        foregroundActivityFlow = MutableStateFlow(null)
        every { mockForegroundActivityTracker.foregroundActivityFlow } returns foregroundActivityFlow

        testScope = TestScope(UnconfinedTestDispatcher())
        oneTimePopupNotificationTrigger = OneTimePopupNotificationTrigger(
            mainScope = testScope.backgroundScope,
            foregroundActivityTracker = mockForegroundActivityTracker,
            apiNotificationManager = mockApiNotificationManager,
            accountManager = mockAccountManager,
            promoOffersPrefs = promoOffersPrefs,
            promoActivityOpener = mockPromoActivityOpener
        )
    }

    @Test
    fun `when app goes to foreground then notification is triggered`() = testScope.runTest {
        activeNotificationsFlow.value = listOf(createTestNotification(NOTIFICATION_ID))

        foregroundActivityFlow.value = mockk()
        verify(exactly = 1) { mockPromoActivityOpener.open(any(), NOTIFICATION_ID) }
    }

    @Test
    fun `when navigating between activities then notification is not triggered`() = testScope.runTest {
        foregroundActivityFlow.value = mockk()

        activeNotificationsFlow.value = listOf(createTestNotification(NOTIFICATION_ID))

        foregroundActivityFlow.value = mockk()
        verify(exactly = 0) { mockPromoActivityOpener.open(any(), NOTIFICATION_ID) }
    }

    @Test
    fun `when no user is logged in then notification is not triggered`() = testScope.runTest {
        primaryUserIdFlow.value = null
        activeNotificationsFlow.value = listOf(createTestNotification(NOTIFICATION_ID))

        foregroundActivityFlow.value = mockk()
        verify(exactly = 0) { mockPromoActivityOpener.open(any(), NOTIFICATION_ID) }
    }

    @Test
    fun `notification is triggered only once`() = testScope.runTest {
        activeNotificationsFlow.value = listOf(createTestNotification(NOTIFICATION_ID))

        foregroundActivityFlow.value = mockk()
        verify(exactly = 1) { mockPromoActivityOpener.open(any(), NOTIFICATION_ID) }

        foregroundActivityFlow.value = null
        foregroundActivityFlow.value = mockk()
        verify(exactly = 1) { mockPromoActivityOpener.open(any(), NOTIFICATION_ID) }
    }

    @Test
    fun `only notifications of TYPE_ONE_TIME_POPUP are triggered`() = testScope.runTest {
        activeNotificationsFlow.value =
            listOf(createTestNotification(NOTIFICATION_ID, ApiNotificationTypes.TYPE_TOOLBAR))

        foregroundActivityFlow.value = mockk()
        verify(exactly = 0) { mockPromoActivityOpener.open(any(), NOTIFICATION_ID) }
    }

    @Test
    fun `when multiple notifications are active then only the first TYPE_ONE_TIME_POPUP is triggered`() = testScope.runTest {
        activeNotificationsFlow.value = listOf(
            createTestNotification("toolbar", ApiNotificationTypes.TYPE_TOOLBAR),
            createTestNotification("popup 1", ApiNotificationTypes.TYPE_ONE_TIME_POPUP),
            createTestNotification("popup 2", ApiNotificationTypes.TYPE_ONE_TIME_POPUP),
        )

        foregroundActivityFlow.value = mockk()
        verify(exactly = 1) { mockPromoActivityOpener.open(any(), "popup 1") }

        foregroundActivityFlow.value = null
        foregroundActivityFlow.value = mockk()
        verify(exactly = 1) { mockPromoActivityOpener.open(any(), "popup 2") }
    }

    private fun createTestNotification(
        id: String,
        type: Int = ApiNotificationTypes.TYPE_ONE_TIME_POPUP
    ): ApiNotification = mockOffer(id, type = type, panel = mockFullScreenImagePanel(""))
}
