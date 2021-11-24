# Scoop

Catches a stack trace when an app crashes unexpectedly.

[<img src="images/get-it-on-github.png"
      alt="Get it on GitHub"
      height="80">](https://github.com/TacoTheDank/Scoop/releases)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/taco.scoop/)

Scoop catches and saves the stack traces of crashing apps and displays all crashes in a list so you don't
have to look through annoyingly long logcats anymore.

Extremely useful for app debugging.

Scoop supports both rooted and non-rooted devices (though non-rooted devices require some [setup](https://github.com/TacoTheDank/Scoop#guide)).

Scoop also supports Xposed.

Features:
- Search (apps, stack traces)
- Crash preview in notifications (configurable in settings)
- Combination of the same crashes / apps to avoid repetitive lists (configurable in settings)
- App blacklist
- Quick actions in notifications to copy / share stack traces


## Guide

Instructions on how to set the app up can be found [here](https://github.com/TacoTheDank/Scoop/wiki).


## General info

This app is a fork of https://github.com/paphonb/Scoop.

This fork aims to further improve and maintain the original app, as it seems to me that paphonb is finished with it.

Most important differences with this fork:
- Lots of under-the-hood improvements
- Using topjohnwu's libsu
- App now has an adaptive icon

You can also read the release notes for changes that have been made to the app since then.

Here is the info for the original app by paphonb: https://forum.xda-developers.com/android/apps-games/adb-root-scoop-catch-stack-trace-app-t3888798


## Contributing

Yes please! I am not really all that knowledgeable in some things.
Contributions are absolutely appreciated!
Feel free to tackle open issues, add something neat, etc. whatever you feel like. :)


## Bugs

Please open an issue if you encounter any problems or have a feature request. Thank you!


## License

This app is under the Apache 2.0 license as licensed by @wasdennnoch (Scoop's original ORIGINAL author).

```
Copyright (C) 2017 Adrian Paschkowski

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
