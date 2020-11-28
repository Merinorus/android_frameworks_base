/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wm.shell.flicker.helpers

import android.app.Instrumentation
import com.android.wm.shell.flicker.TEST_APP_FIXED_ACTIVITY_LABEL
import com.android.wm.shell.flicker.testapp.Components

class FixedAppHelper(
    instrumentation: Instrumentation
) : BaseAppHelper(
        instrumentation,
        TEST_APP_FIXED_ACTIVITY_LABEL,
        Components.FixedActivity()
)