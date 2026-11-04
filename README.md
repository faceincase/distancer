# Distancer

A small, client-side Fabric mod for Minecraft 26.3 that estimates how far away a player is, using the locator bar.

For players far away, the locator bar only tells the game a *direction*, never a distance. Distancer records that direction from several places as you move, then finds the point where the lines cross, the same idea as locating a stronghold with two eyes of ender. The estimate shows in a small box beside the locator bar, for the player whose dot is in the middle of the bar.

## Using it

- Face a player's dot on the locator bar so it sits in the middle.
- Walk sideways relative to them for a few dozen blocks. Walking straight at or away from them gives parallel lines and no answer.
- The number is gray while the estimate is shaky and white once the lines agree.
- For nearby players the game already knows roughly where they are, so the real distance is shown.

Estimates are approximate: a moving target changes the lines between samples, and height is ignored (the bearing carries no pitch). Estimates of 100+ blocks are rounded to the nearest 10.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API

Client-only: nothing needs installing on the server. No mixins, no config, no networking.

## Building

Needs JDK 25.

```
./gradlew build
```

The jar is written to `build/libs/`. `./gradlew runClient` starts a development client.

## License

MIT, see [LICENSE](LICENSE).
