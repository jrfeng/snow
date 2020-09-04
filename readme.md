[![GitHub](https://img.shields.io/github/license/jrfeng/snow)](./license)
[![](https://jitpack.io/v/jrfeng/snow.svg)](https://jitpack.io/#jrfeng/snow)
[![GitHub issues](https://img.shields.io/github/issues/jrfeng/snow)](https://github.com/jrfeng/snow/issues)
[![GitHub closed issues](https://img.shields.io/github/issues-closed/jrfeng/snow)](https://github.com/jrfeng/snow/issues?q=is%3Aissue+is%3Aclosed)

[**中文**](./readme_zh.md)

Android audio service library. Support custom music player(MediaPlayer, ExoPlayer), custom Notification, custom audio effect engine, headset clicks, record playback historical, player status automatic recovery on restart.

**Document:**

* [**Getting Started**](https://github.com/jrfeng/snow/wiki/1.%E5%BF%AB%E9%80%9F%E4%B8%8A%E6%89%8B)
* [**Custom Player Component**](https://github.com/jrfeng/snow/wiki/2.%E8%87%AA%E5%AE%9A%E4%B9%89%E6%92%AD%E6%94%BE%E5%99%A8%E7%BB%84%E4%BB%B6)
* [**Custom Notification**](https://github.com/jrfeng/snow/wiki/3.%E8%87%AA%E5%AE%9A%E4%B9%89%E9%80%9A%E7%9F%A5%E6%A0%8F%E6%8E%A7%E5%88%B6%E5%99%A8)
* [**Use ExoPlayer**](https://github.com/jrfeng/snow/wiki/4.%E4%BD%BF%E7%94%A8-ExoPlayer)

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

2. Add a dependency in the `build.gradle` file of your app module. ![](https://jitpack.io/v/jrfeng/snow.svg)

```gradle
dependencies {
    implementation 'com.github.jrfeng.snow:player:1.0-alpha6'
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

4. Config PlayerService.

```xml
<service android:name="snow.player.PlayerService">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
        <action android:name="android.intent.action.MEDIA_BUTTON" />
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
PlayerClient playerClient = PlayerClient.newInstance(context, PlayerService.class);

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

**More Content:**

* [**Wiki**](https://github.com/jrfeng/snow/wiki)
* [**API Doc**](https://jrfeng.github.io/snow-doc/)

### ProGuard

```txt
-keep public class * extends snow.player.PlayerService$ComponentFactory { *; }
```

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