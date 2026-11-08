# War and CWL strip (optional)

The widget can show your clan's current war, friendly war or CWL round, with a countdown to the start or the end.

**It is off by default.** Clash Ledger otherwise sends nothing to any server except its app-update check.

## Turning it on

1. Long-press the app icon and choose **War timers**. You can also tap the war strip on the widget once it is visible.
2. Switch on **Show war and CWL on the widget**.
3. Tap **Save**, then **Check now**.

You need an imported village first, because the player tag comes from the export.

## What is sent

Only your **player tag** and your **clan tag**, to a public proxy of the official Clash of Clans API:

- Without a key, the free ClashKing proxy is used (`proxy.clashk.ing`).
- With your own key from developer.clashofclans.com, requests go through RoyaleAPI's proxy (`cocproxy.royaleapi.dev`) first, then ClashKing. Add `45.79.218.79` to the key's allowed IP addresses.

Your village export, upgrades and settings are never sent. The key is stored only on the phone.

## Limits

- Regular and friendly wars appear only when the clan's **war log is public**. CWL is still checked when it is private.
- Data refreshes at most every ten minutes, when the widget refreshes. Android may delay refreshes to save battery.
- The proxies are community services. If one stops working, add your own key.
- The strip shows the attacks you have left, but only if you are in the war lineup.
