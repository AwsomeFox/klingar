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

import com.awsomefox.sprocket.data.api.MediaService;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Rx;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

/**
 * Updates the Plex server of current playback status
 */
class TimelineManager {

  private final MediaController mediaController;
  private final QueueManager queueManager;
  private final MediaService media;
  private final Rx rx;
  private CompositeDisposable disposables;

  TimelineManager(MediaController mediaController, QueueManager queueManager, MediaService media,
                  Rx rx) {
    this.mediaController = mediaController;
    this.queueManager = queueManager;
    this.media = media;
    this.rx = rx;
    disposables = new CompositeDisposable();
  }

  void start() {

    disposables.add(Flowable.combineLatest(state(), currentTrack(), progress(),
        (state, track, time) -> new Timeline(state, time, track))
        .observeOn(rx.io())
        .flatMapCompletable(this::updateTimeline)
        .subscribeOn(rx.io())
        .observeOn(rx.io())
            .subscribe(() -> Timber.d("onCompleted"), Rx::onError));
  }

  private Completable updateTimeline(Timeline t) {
    return media.timeline(t.track.uri(), t.track.queueItemId(), t.track.key(), t.track.ratingKey(),
        t.state, t.track.duration(), t.time)
        .onErrorComplete(); // Skip errors;
  }

  private Flowable<Long> progress() {
    return mediaController.progress()
            .filter(progress -> (progress % 5000) == 0);  // Send updates every 10 seconds but not exact
  }

  private Flowable<Track> currentTrack() {
    return queueManager.queue()
        .filter(pair -> pair.second < pair.first.size())
        .map(pair -> pair.first.get(pair.second));
  }

  private Flowable<String> state() {
    Timber.d("TIMELINE STATE");
    return mediaController.state()
        .filter(state -> state == STATE_PLAYING || state == STATE_PAUSED || state == STATE_STOPPED)
        .map(state -> {
          if (state == STATE_PLAYING) {
            return "playing";
          } else if (state == STATE_PAUSED) {
            return "paused";
          }
          return "stopped";
        });
  }

  void stop() {
    Rx.dispose(disposables);
  }

  private static class Timeline {
    private final String state;
    private final long time;
    private final Track track;

    Timeline(String state, long time, Track track) {
      this.state = state;
      this.time = time;
      this.track = track;
    }
  }
}
