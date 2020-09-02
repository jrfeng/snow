Android 音乐播放器框架，实现了常见的列表播放器功能。支持线控播放，自定义播放器（MediaPlayer, ExoPlayer），自定义通知栏控制器，自定义音频特效引擎，记录播放历史，播放器状态自动恢复。

## 项目配置

1. 将以下代码添加到项目根目录中的 `build.gradle` 中：

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. 将以下代码添加到模块的依赖中：[![](https://jitpack.io/v/jrfeng/snow.svg)](https://jitpack.io/#jrfeng/snow)

```gradle
dependencies {
    implementation 'com.github.jrfeng.snow:player:1.0-alpha5'
}
```

3. 申请权限：

```xml
<!-- 用于启动前台 Service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- 用于后台播放 -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- 用于播放本地音乐 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

<!-- 用于播放网络音乐 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**注意！在大于等于 `Android 6.0(API Level 23)` 的 `Android` 版本中，需要动态申请存储器访问权限：`"android.permission.READ_EXTERNAL_STORAGE"`。**

4. 配置播放器

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

## 开始使用

1. 连接到 `PlayerService`：

```java
// 创建一个 PlayerClient 对象
PlayerClient playerClient = PlayerClient.newInstance(context, PlayerService.class);

// 连接到 PlayerService
playerClient.connect(new PlayerClient.OnConnectCallback() {
    @Override
    public void onConnected(boolean success) {
        // DEBUG
        Log.d("App", "connect: " + success);
    }
});
```

3. 创建一个播放列表

```java
private Playlist createPlaylist() {
    MusicItem song1 = new MusicItemBuilder(313520, "http://music.163.com/song/media/outer/url?id=4875306")
            .setTitle("逍遥叹")
            .setArtist("胡歌")
            .setIconUri("http://p1.music.126.net/4tTN8CnR7wG4E1cauIPCvQ==/109951163240682406.jpg")
            .build();

    MusicItem song2 = new MusicItemBuilder(267786, "http://music.163.com/song/media/outer/url?id=4875305")
            .setTitle("终于明白")
            .setArtist("动力火车")
            .build();

    MusicItem song3 = new MusicItemBuilder(260946, "http://music.163.com/song/media/outer/url?id=150371")
            .setTitle("千年泪")
            .setArtist("Tank")
            .setIconUri("http://p2.music.126.net/0543F-ln2Apdiopez_jbsA==/109951163244853571.jpg")
            .build();

    MusicItem song4 = new MusicItemBuilder(265000, "http://music.163.com/song/media/outer/url?id=25638340")
            .setTitle("此生不换")
            .setArtist("青鸟飞鱼")
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

4. 设置播放列表并播放音乐

```java
// 创建播放列表
Playlist playlist = createPlaylist();

// 设置播放列表，并播放音乐
playerClient.setPlaylist(playlist, true);

```

**`PlayerClient` 支持的播放器功能：**

* `setPlaylist(Playlist playlist, boolean play)`：设置播放列表
* `play()`：播放
* `pause()`：暂停
* `playPause()`：播放/暂停
* `playPause(int position)`：播放/暂停列表中指定位置处的音乐
* `stop()`：停止
* `seekTo(int progress)`：调整播放位置
* `skipToPrevious()`：上一曲
* `skipToNext()`：下一曲
* `fastForward()`：快进
* `rewind()`：快退
* `setNextPlay(MusicItem musicItem)`：下一首播放
* `setPlayMode(PlayMode playMode)`：设置播放模式（共 `3` 种模式：顺序播放、单曲循环、随机播放）

更多内容，请参考：

* [**Wiki**](https://github.com/jrfeng/snow/wiki)
* [**API Doc**](https://jrfeng.github.io/snow/)

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