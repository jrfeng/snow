![API Level](https://img.shields.io/badge/Android-API%20Level%2016%2B-brightgreen)
[![GitHub](https://img.shields.io/github/license/jrfeng/snow)](./license)

[**中文**](./readme_zh.md)

Android music player library. Compatible with MediaSession.

**Support:**

* Custom music player (MediaPlayer, ExoPlayer)
* Custom Notification
* Custom audio effect engine
* Headset clicks
* Sleep timer
* Playback history
* Player state persistence

**Document:**

* [**Getting Started**](https://github.com/jrfeng/snow/wiki/[EN]-1.Getting-Started)
* [**Custom PlayerService**](https://github.com/jrfeng/snow/wiki/[EN]-2.Custom-PlayerService)
* [**Custom Notification**](https://github.com/jrfeng/snow/wiki/[EN]-3.Custom-Notification)
* [**Use ExoPlayer**](https://github.com/jrfeng/snow/wiki/[EN]-4.Use-ExoPlayer)

**Sample App:**

* [**Download**](https://github.com/jrfeng/snow/releases/tag/1.0)

**More:**

* [**Wiki**](https://github.com/jrfeng/snow/wiki)
* [**API Doc**](https://jrfeng.github.io/snow-doc/)

## Add dependency

1. Make sure you have the jitpack repositories included in the `build.gradle` file in the root of your project.

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. Add a dependency in the `build.gradle` file of your app module --- [latest version](https://github.com/jrfeng/snow/releases)

```gradle
dependencies {
    implementation 'com.github.jrfeng.snow:player:1.0'
}
```

3. Request permission.

```xml
<!-- for start foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- for play in the background -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- for play local music -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

<!-- for play network music -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Note: Android 6.0 (API level 23) need request `android.permission.READ_EXTERNAL_STORAGE` permission at runtime.**

4. Create a PlayerService

Create a class and let it extends the `snow.player.PlayerService`, and annotate it with `@PersistenceId` annotation. You don't need to override any methods of this class.

```java
@PersistenId("MyPlayerService")
public MyPlayerService extends PlayerService {
}
```

The `@PersistenceId` annotation is used to set a persistent ID for the current `PlayerService`, which will be used for the persistence of the `PlayerService` state. If you do not use the `@PersistenceId` annotation to set the persistent ID, the persistent ID defaults to the full class name of your `PlayerService` (such as `snow.demo.MyPlayerService`). It is recommended to set a persistent ID for your `PlayerService` so that even if the `PlayerService` is renamed, the state will not be lost.

5. Register PlayerService on `AndroidManifest.xml`.

```xml
<service android:name="snow.demo.MyPlayerService">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
</service>

<receiver android:name="androidx.media.session.MediaButtonReceiver" >
    <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</receiver>
```

## Getting Started

1. Connect to `PlayerService`.

```java
// create a PlayerClient instance
PlayerClient playerClient = PlayerClient.newInstance(context, MyPlayerService.class);

// connect to PlayerService
playerClient.connect(new PlayerClient.OnConnectCallback() {
    @Override
    public void onConnected(boolean success) {
        // DEBUG
        Log.d("App", "connect: " + success);
    }
});
```

3. Create a Playlist.

```java
private Playlist createPlaylist() {
    MusicItem song1 = new MusicItem.Builder()
            .setTitle("逍遥叹")
            .setArtist("胡歌")
            .setDuration(313520)
            .setUri("http://music.163.com/song/media/outer/url?id=4875306")
            .setIconUri("http://p1.music.126.net/4tTN8CnR7wG4E1cauIPCvQ==/109951163240682406.jpg")
            .build();

    MusicItem song2 = new MusicItem.Builder()
            .setTitle("终于明白")
            .setArtist("动力火车")
            .setDuration(267786)
            .setUri("http://music.163.com/song/media/outer/url?id=4875305")
            .build();

    MusicItem song3 = new MusicItem.Builder()
            .setTitle("千年泪")
            .setArtist("Tank")
            .setDuration(260946)
            .setUri("http://music.163.com/song/media/outer/url?id=150371")
            .setIconUri("http://p2.music.126.net/0543F-ln2Apdiopez_jbsA==/109951163244853571.jpg")
            .build();

    MusicItem song4 = new MusicItem.Builder()
            .setTitle("此生不换")
            .setArtist("青鸟飞鱼")
            .setDuration(265000)
            .setUri("http://music.163.com/song/media/outer/url?id=25638340")
            .setIconUri("http://p2.music.126.net/UyDVlWWgOn8p8U8uQ_I1xQ==/7934075907687518.jpg")
            .build();

    return new Playlist.Builder()
            .append(song1)
            .append(song2)
            .append(song3)
            .append(song4)
            .build();
}
```

4. Set playlist and start playing music.

```java
// create a Playlist instance.
Playlist playlist = createPlaylist();

// set playlist and start playing music
playerClient.setPlaylist(playlist, true);

```

**`PlayerClient` common methods:**

* `setPlaylist(Playlist playlist, boolean play)`
* `play()`
* `pause()`
* `playPause()`
* `playPause(int position)`
* `stop()`
* `seekTo(int progress)`
* `skipToPrevious()`
* `skipToNext()`
* `fastForward()`
* `rewind()`
* `setNextPlay(MusicItem musicItem)`
* `setPlayMode(PlayMode playMode)`

## LICENSE

```txt
MIT License

Copyright (c) 2020 jrfeng

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```