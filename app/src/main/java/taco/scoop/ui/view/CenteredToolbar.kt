/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package taco.scoop.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import taco.scoop.R

/*
 * Copyright (C) 2018 paphonb@xda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class CenteredToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.toolbarStyle
) : Toolbar(context, attrs, defStyleAttr) {

    private var mTitleTextView: AppCompatTextView? = null
    private var mSubtitleTextView: AppCompatTextView? = null
    private var mTitleText: CharSequence? = null
    private var mSubtitleText: CharSequence? = null

    private fun inflateTitle() {
        LayoutInflater.from(context).inflate(R.layout.toolbar_title, this)
        mTitleTextView = findViewById(R.id.toolbar_title)
        mSubtitleTextView = findViewById(R.id.toolbar_subtitle)
    }

    override fun setTitle(title: CharSequence) {
        if (title.isNotEmpty()) {
            if (mTitleTextView == null) {
                inflateTitle()
            }
        }
        mTitleTextView?.let {
            it.text = title
        }
        mTitleText = title
    }

    override fun getTitle(): CharSequence? {
        return mTitleText
    }

    override fun setSubtitle(subtitle: CharSequence) {
        if (subtitle.isNotEmpty()) {
            if (mSubtitleTextView == null) {
                inflateTitle()
            }
        }
        mSubtitleTextView?.isGone = subtitle.isEmpty()
        mSubtitleTextView?.let {
            it.text = subtitle
        }
        mSubtitleText = subtitle
    }

    override fun getSubtitle(): CharSequence? {
        return mSubtitleText
    }
}
