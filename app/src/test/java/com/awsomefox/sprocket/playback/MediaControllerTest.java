/*
 * Copyright (C) 2017 Simon Norberg
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
package com.awsomefox.sprocket.playback;

import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.MediaControllerCompat.TransportControls;
import android.support.v4.media.session.PlaybackStateCompat;

import com.awsomefox.sprocket.util.Rx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaControllerTest {

  @Mock MediaControllerCompat mockMediaController;
  @Mock TransportControls mockTransportControls;
  @Mock PlaybackStateCompat mockPlaybackState;
    private MediaController mediaController;

  @Before public void setup() {
      mediaController = new MediaController(seconds(), Rx.test());
      mediaController.setMediaController(mockMediaController);
    when(mockMediaController.getTransportControls()).thenReturn(mockTransportControls);
    when(mockMediaController.getPlaybackState()).thenReturn(mockPlaybackState);
  }

  @Test public void play() {
      mediaController.play();
    verify(mockTransportControls, times(1)).play();
  }

  @Test public void pause() {
      mediaController.pause();
    verify(mockTransportControls, times(1)).pause();
  }

  @Test public void togglePlayToPause() {
    when(mockPlaybackState.getState()).thenReturn(STATE_PLAYING);
      mediaController.playPause();
    verify(mockTransportControls, times(1)).pause();
    verify(mockTransportControls, never()).play();
  }

  @Test public void togglePauseToPlay() {
    when(mockPlaybackState.getState()).thenReturn(STATE_PAUSED);
      mediaController.playPause();
    verify(mockTransportControls, times(1)).play();
    verify(mockTransportControls, never()).pause();
  }

  @Test public void playQueueItem() {
      mediaController.playQueueItem(1337);
    verify(mockTransportControls, times(1)).skipToQueueItem(1337);
  }

  @Test public void seekTo() {
      mediaController.seekTo(10000L);
    verify(mockTransportControls, times(1)).seekTo(10000L);
  }

  @Test public void next() {
      mediaController.next();
    verify(mockTransportControls, times(1)).skipToNext();
  }

  @Test public void previous() {
      mediaController.previous();
    verify(mockTransportControls, times(1)).skipToPrevious();
  }

  @Test public void shuffle() {
      mediaController.shuffle();
    verify(mockTransportControls, times(1)).sendCustomAction(PlaybackManager.CUSTOM_ACTION_SHUFFLE, null);
  }

  @Test public void repeat() {
      mediaController.repeat();
    verify(mockTransportControls, times(1)).sendCustomAction(PlaybackManager.CUSTOM_ACTION_REPEAT, null);
  }

  @Test public void notifyWhenPlaybackStateChanges() {
    when(mockPlaybackState.getState())
        .thenReturn(STATE_BUFFERING)
        .thenReturn(STATE_PLAYING)
        .thenReturn(STATE_PLAYING) // same state again!
        .thenReturn(STATE_PAUSED);

    doAnswer(mock -> {
      Callback callback = mock.getArgument(0);
      callback.onPlaybackStateChanged(mockPlaybackState);
      callback.onPlaybackStateChanged(mockPlaybackState);
      callback.onPlaybackStateChanged(mockPlaybackState);
      callback.onPlaybackStateChanged(mockPlaybackState);
      return callback;
    }).when(mockMediaController).registerCallback(any(Callback.class));

      TestSubscriber<Integer> test = mediaController.state().take(4).test();

      mediaController.setMediaController(mockMediaController);

    test.awaitTerminalEvent();
    test.assertValues(STATE_NONE, STATE_BUFFERING, STATE_PLAYING, STATE_PAUSED);
  }

  @Test public void startProgressWhenPlaying() {
    when(mockPlaybackState.getState()).thenReturn(STATE_PLAYING);

    doAnswer(mock -> {
      Callback callback = mock.getArgument(0);
      callback.onPlaybackStateChanged(mockPlaybackState);
      return callback;
    }).when(mockMediaController).registerCallback(any(Callback.class));

      TestSubscriber<Long> test = mediaController.progress().take(5).test();

      mediaController.setMediaController(mockMediaController);

    test.awaitTerminalEvent();
    test.assertValues(0L, 0L, 1000L, 2000L, 3000L);
  }

  private Flowable<Long> seconds() {
    return Flowable.rangeLong(0, 3).onBackpressureBuffer();
  }
}
