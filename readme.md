**【开发中】大部分功能已完成，但部分功能尚未进行测试，暂不能用于生产环境。**

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
    implementation 'com.github.jrfeng.snow:player:1.0-alpha2'
}
```

3. 申请权限：

```xml
<!-- 用于播放本地音乐 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

<!-- 用于后台播放 -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- 可选，用于播放网络音乐 -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- 可选，用于访问网络状态 -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**注意！在高于 `Android 6.0(API Level 23)` 的 `Android` 版本中，存储器访问权限 `"android.permission.READ_EXTERNAL_STORAGE"` 需要动态申请。**

4. 配置播放器

```xml
<service android:name="snow.player.PlayerService">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
        <!-- 可选，用于支持低于 Android 5.0(API Level 21) 的版本 -->
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</service>

<!-- 可选，用于支持低于 Android 5.0(API Level 21) 的版本 -->
<receiver android:name="androidx.media.session.MediaButtonReceiver" >
    <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</receiver>
```

对于上面的可选部分，如果你的应用程序不打算支持低于 `Android 5.0(API Level 21)` 的 `Android` 版本，可以省略这部分代码。

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

2. 获取 `PlaylistController`

```java
// 当 PlayerClient 连接成功后，可以调用其 getPlaylistController() 方法获取到一个 PlaylistController 对象
PlayerClient.PlaylistController playlistController =  playerClient.getPlaylistController();
```

**`PlaylistController` 提供了基本的列表播放器功能，例如：**

* `play()`：播放
* `pause()`：暂停
* `playOrPause()`：播放/暂停
* `stop()`：停止
* `seekTo(int progress)`：调整播放位置
* `skipToPrevious()`：上一曲
* `skipToNext()`：下一曲
* `fastForward()`：快进
* `rewind()`：快退
* `setPlayMode(PlayMode)`：设置播放模式（共 `3` 种模式：顺序播放、单曲循环、随机播放）
* `setPlaylist(Playlist playlist, int position, boolean play)`：设置播放列表

3. 设置播放列表并播放音乐

```java
// 创建播放列表
Playlist playlist = createPlaylist();

// 设备播放列表，并播放指定索引处的音乐
playlistController.setPlaylist(playlist, 0, true);

private Playlist createPlaylist() {
    List<MusicItem> musicItemList = new ArrayList<>();

    MusicItem musicItem1 = new MusicItem();
    musicItem1.setTitle("title 1");
    musicItem1.setArtist("artist 1");
    // 设置音乐的 URI（可以是本地路径，但需要有存储器访问权限）
    musicItem1.setUri("http://www.demo.com/songs/song1.mp3");
    // 设置音乐的图标的 URI（可以是本地路径，但需要有存储器访问权限）
    musicItem1.setIconUri("http://www.demo.com/icon/song1_icon.png");

    MusicItem musicItem2 = new MusicItem();
    musicItem2.setTitle("title 2");
    musicItem2.setArtist("artist 2");
    musicItem2.setUri("http://www.demo.com/songs/song2.mp3");
    musicItem2.setIconUri("http://www.demo.com/icon/song2_icon.png");

    MusicItem musicItem3 = new MusicItem();
    musicItem3.setTitle("title 3");
    musicItem3.setArtist("artist 3");
    musicItem3.setUri("http://www.demo.com/songs/song3.mp3");
    musicItem3.setIconUri("http://www.demo.com/icon/song3_icon.png");

    musicItemList.add(musicItem1);
    musicItemList.add(musicItem2);
    musicItemList.add(musicItem3);

    return new Playlist(musicItemList);
}
```

**更多详细介绍，请查看 `Wiki`（编写中...）**

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