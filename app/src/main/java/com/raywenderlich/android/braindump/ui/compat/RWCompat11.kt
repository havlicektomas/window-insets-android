/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.braindump.ui.compat

import android.content.Context
import android.graphics.Insets
import android.os.Build
import android.os.CancellationSignal
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.AbsListView
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.*

internal class RWCompat11(private val view: View, private val container: View) {

  //1
  private var posTop = 0
  private var posBottom = 0

  private var animationController: WindowInsetsAnimationController? = null

  private val animationControlListener: WindowInsetsAnimationControlListener by lazy {
    @RequiresApi(Build.VERSION_CODES.R)
    object : WindowInsetsAnimationControlListener {

      override fun onReady(
        controller: WindowInsetsAnimationController,
        types: Int
      ) {
        animationController = controller
      }

      override fun onFinished(controller: WindowInsetsAnimationController) {
        animationController = null
      }

      override fun onCancelled(controller: WindowInsetsAnimationController?) {
        animationController = null
      }
    }
  }

  fun setUiWindowInsets() {
    //2
    ViewCompat.setOnApplyWindowInsetsListener(container) { _, insets ->
      //3
      if (posBottom == 0) {
        posTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
      }
      //4
      container.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        updateMargins(
          top = posTop,
          bottom = posBottom)
      }

      insets
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  fun createLinearLayoutManager(context: Context, view: View): LinearLayoutManager {
    var scrolledY = 0
    var scrollToOpenKeyboard = false

    return object : LinearLayoutManager(context) {
      var visible = false

      override fun onScrollStateChanged(state: Int) {
        //1
        if (state == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
          //2
          visible = view.rootWindowInsets?.isVisible(WindowInsetsCompat.Type.ime()) == true
          //3
          if (visible) {
            scrolledY = view.rootWindowInsets?.getInsets(WindowInsetsCompat.Type.ime())!!.bottom
          }
          //4
          createWindowInsetsAnimation()
          //5
        } else if (state == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
          //6
          scrolledY = 0
          animationController?.finish(scrollToOpenKeyboard)
        }

        super.onScrollStateChanged(state)
      }

      override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
        //1
        scrollToOpenKeyboard = scrolledY < scrolledY + dy
        //2
        scrolledY += dy
        //3
        if (scrolledY < 0) {
          scrolledY = 0
        }
        //4
        animationController?.setInsetsAndAlpha(
          Insets.of(0, 0, 0, scrolledY),
          1f,
          0f
        )

        return super.scrollVerticallyBy(dy, recycler, state)
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun createWindowInsetsAnimation() {
    view.windowInsetsController?.controlWindowInsetsAnimation(
      WindowInsetsCompat.Type.ime(), //types
      -1,                //durationMillis
      LinearInterpolator(),          //interpolator
      CancellationSignal(),          //cancellationSignal
      animationControlListener       //listener
    )
  }

  //1
  @RequiresApi(Build.VERSION_CODES.R)
  fun animateKeyboardDisplay() {
    //2
    val cb = object : WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
      //3
      override fun onProgress(insets: WindowInsets, animations: MutableList<WindowInsetsAnimation>): WindowInsets {
        //4
        posBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom +
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        //5
        container.updateLayoutParams<ViewGroup.MarginLayoutParams> {
          updateMargins(
            top = posTop,
            bottom = posBottom)
        }

        return insets
      }
    }
    //6
    container.setWindowInsetsAnimationCallback(cb)
  }
}